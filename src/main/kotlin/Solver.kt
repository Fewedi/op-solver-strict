package masterthesis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import masterthesis.solver.*
import masterthesis.solver.config.ConfigProvider
import masterthesis.solver.config.Solver
import masterthesis.solver.legacy.*
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.ProblemSpace
import org.slf4j.LoggerFactory
import solver.ProblemParser
import solver.Visualizer

class Solver {

    private val logger = LoggerFactory.getLogger(ProblemParser::class.java)
    private val cleanupService = CleanupService()
    private val problemParser = ProblemParser()
    private val clustering = Clustering()
    private val problemWriter: ProblemWriter = when (ConfigProvider.config.startEntries) {
        0 -> ProblemWriterWithDummy()
        1 -> ProblemWriterNoDummy()
        else -> ProblemWriterWithDummy()
    }
    private val objectMapper = jacksonObjectMapper().apply {
        when (ConfigProvider.config.solver) {
            Solver.gurobi -> setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.UPPER_CAMEL_CASE)
            Solver.ea4op -> setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
        }
    }
    private val gkobeagaOpSolverClient = GkobeagaOpSolverClient()
    private val gurobiOpSolverClient = GurobiClient()
    private val tSPForCluster = TSPForCluster()
    private val visualizer = Visualizer()

    fun solve() {

        cleanupService.cleanUp()
        val problemSpace = problemParser.readProblemSpace()
        val clusterMap = clustering.cluster(problemSpace.nodeMap, problemSpace.metaData.costLimit.toInt())
        val clusterPath = tSPForCluster.provideClusterPath(clusterMap)
        val clusters = clusterPath.map { cluster ->
            val costLimit = problemSpace.metaData.costLimit.toInt() / (clusterPath.size)
            when (ConfigProvider.config.solver) {
                Solver.gurobi -> {
                    solveWithGurobi(cluster, costLimit)
                }
                Solver.ea4op -> {
                    solveWithEa4op(cluster, problemSpace,costLimit, "src/main/resources/a280-gen1-50-cluster-${cluster.id}.oplib")
                }
            }
            cluster
        }

        visualizer.plotGraph(problemSpace.nodeMap, clusters.filterNotNull())
    }

    private fun solveWithGurobi(cluster: Cluster, costLimit: Int) {
        try {
            val startNodeIndex = cluster.nodes.indexOf(cluster.startNodes.first())
            val preparedList = listOf(cluster.nodes[startNodeIndex]) + cluster.nodes.filterIndexed { index, _ -> index != startNodeIndex} + listOf(cluster.endNodes.first())
            val solution = gurobiOpSolverClient.solve(objectMapper, preparedList, costLimit)
            cluster.solutionList = problemParser.addSolutionToProblem(preparedList, solution, cluster.startNodes.first(), cluster.endNodes.first())
            logger.info("Cluster ${cluster.id} solved with solution: ${cluster.solutionList.map { it.id }}")
        } catch (e: Exception) {
            logger.error("Cluster ${cluster.id} failed with error: ${e.message}")
            throw e
        }
    }

    private fun solveWithEa4op(cluster: Cluster, problemSpace: ProblemSpace, costLimit: Int, path: String) {
        try {
            problemWriter.writeCluster(
                problemSpace.metaData,
                problemSpace.distanceMatrix,
                cluster,
                path,
                costLimit
            )
            val solution = gkobeagaOpSolverClient.solve(path, objectMapper)
            cluster.solutionList = problemParser.addSolutionToProblem(cluster.solutionMap, solution!!)

        } catch (e: Exception) {
            logger.error("Cluster ${cluster.id} failed with error: ${e.message}")
        }
    }
}