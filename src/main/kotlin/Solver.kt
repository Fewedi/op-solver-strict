package masterthesis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import masterthesis.evaluation.Evaluater
import masterthesis.evaluation.Visualizer
import masterthesis.solver.*
import masterthesis.solver.config.BudgetDistributionMethod
import masterthesis.solver.config.ClusteringMethod
import masterthesis.solver.config.ConfigProvider
import masterthesis.solver.config.Solver
import masterthesis.solver.legacy.*
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.ProblemSpace
import masterthesis.solver.model.Result
import org.slf4j.LoggerFactory
import solver.ProblemParser

class Solver {

    private val logger = LoggerFactory.getLogger(Solver::class.java)
    private val cleanupService = CleanupService()
    private val problemParser = ProblemParser()
    private val clustering = Clustering()
    private val problemWriter: ProblemWriter? = when (ConfigProvider.config.ea4op?.startEntries) {
        0 -> ProblemWriterWithDummy()
        1 -> ProblemWriterNoDummy()
        else -> null
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
    private val evaluater = Evaluater()
    private val budgetCalculator = BudgetCalculator()
    private val clusterCorrecter = ClusterCorrecter()

    fun solve(folderName: String, gen: String): Result {

        cleanupService.cleanUp()

        val startTime = System.nanoTime()

        val problemSpace = problemParser.readProblemSpace(folderName, gen)

        logger.info("clustering ${problemSpace.nodeMap.size} nodes with method: ${ConfigProvider.config.clustering}")
        val clusterMap = when (ConfigProvider.config.clustering) {
            ClusteringMethod.KMEANSUPPERBOUND -> { clustering.clusterKmeansUpperBound(problemSpace.nodeMap) }
            ClusteringMethod.KMEANSUPPERBOUNDIGNOREOUTLIERS -> { clustering.clusterKmeansUpperBoundIgnoreOutliers(problemSpace.nodeMap) }
            ClusteringMethod.KMEANSCAPACITATEDCUSTOM -> { clustering.clusterCapacitatedCustom(problemSpace.nodeMap) }
            ClusteringMethod.KMEANSCAPACITATED -> { clustering.clusterCapacitated(problemSpace.nodeMap) }
            ClusteringMethod.KMEANS, ClusteringMethod.KMEANSANDCORRECTLATER -> { clustering.clusterKmeans(problemSpace.nodeMap) }
        }
        logger.info("clustering done")

        logger.info("solving cluster TSP with ${clusterMap.size} clusters in concorde")
        val clusterPath = tSPForCluster.provideClusterPathConcorde(clusterMap)
        logger.info("solving cluster TSP done")

        if (ConfigProvider.config.clustering == ClusteringMethod.KMEANSANDCORRECTLATER) {
            logger.info("correcting cluster sizes")
            clusterCorrecter.correctClusterSizes(clusterPath, ConfigProvider.config.clusterSize)
        }

        tSPForCluster.setDistancesToNextClusterAndProvideStartNodes(clusterPath)

        when (ConfigProvider.config.budgetDistribution) {
            BudgetDistributionMethod.ELZEIN -> {
                budgetCalculator.calculateBudgetElzein(clusterPath, problemSpace.metaData.costLimit.toDouble())
            }
            BudgetDistributionMethod.ELZEINWITHMIN -> {
                budgetCalculator.calculateBudgetElzeinWithMin(clusterPath, problemSpace.metaData.costLimit.toDouble())
            }
            BudgetDistributionMethod.NAIVE -> {
                budgetCalculator.calculateBudgetNaive(clusterPath, problemSpace.metaData.costLimit.toDouble())
            }
        }


        logger.info("solving clusters with ${ConfigProvider.config.solver}")
        try {
            val clusters = clusterPath.map { cluster ->
                when (ConfigProvider.config.solver) {
                    Solver.gurobi -> {
                        solveWithGurobi(cluster, cluster.budget.toInt())
                    }

                    Solver.ea4op -> {
                        solveWithEa4op(
                            cluster,
                            problemSpace,
                            cluster.budget.toInt(),
                            "src/main/resources/a280-$gen-50-cluster-${cluster.id}.oplib"
                        )
                    }
                }
                cluster
            }

            val endTime = System.nanoTime()
            val duration = (endTime - startTime) / 1_000_000_000.0
            visualizer.plotGraph(problemSpace.nodeMap, clusterPath, folderName, true)
            return evaluater.evaluateResult(problemSpace, clusters, duration, folderName)
        }catch (e: Exception){
            throw e
        }finally {
            visualizer.plotGraph(problemSpace.nodeMap, clusterPath, folderName, false)
        }
    }

    private fun solveWithGurobi(cluster: Cluster, costLimit: Int) {
        try {
            val startNodeIndex = cluster.nodes.indexOf(cluster.startNodes.first())
            val preparedList =
                listOf(cluster.nodes[startNodeIndex]) + cluster.nodes.filterIndexed { index, _ -> index != startNodeIndex } + listOf(
                    cluster.endNodes.first()
                )
            val solution = gurobiOpSolverClient.solve(objectMapper, preparedList, costLimit)
            cluster.solutionList = problemParser.addSolutionToProblem(
                preparedList,
                solution,
                cluster.startNodes.first(),
                cluster.endNodes.first()
            )
            logger.info("Cluster ${cluster.id} solved with solution: ${cluster.solutionList.map { it.id }}")
        } catch (e: Exception) {
            logger.error("Cluster ${cluster.id} failed with error: ${e.message}")
            throw e
        }
    }

    private fun solveWithEa4op(cluster: Cluster, problemSpace: ProblemSpace, costLimit: Int, path: String) {
        try {
            problemWriter!!.writeCluster(
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