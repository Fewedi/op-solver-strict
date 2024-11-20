package masterthesis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import masterthesis.solver.CleanupService
import masterthesis.solver.Clustering
import masterthesis.solver.TSPForCluster
import solver.GkobeagaOpSolverClient
import solver.ProblemParser
import solver.ProblemWriter
import solver.Visualizer

class Solver {

    fun solve() {

        val cleanupService = CleanupService()
        val problemParser = ProblemParser()
        val clustering = Clustering()
        val problemWriter = ProblemWriter()
        val objectMapper = jacksonObjectMapper().apply {
            setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
        }
        val gkobeagaOpSolverClient = GkobeagaOpSolverClient()
        val tSPForCluster = TSPForCluster()
        val visualizer = Visualizer()


        cleanupService.cleanUp()
        val problemSpace = problemParser.readProblemSpace()
        val clusterMap = clustering.cluster(problemSpace.nodeMap, problemSpace.metaData.costLimit.toInt())
        val clusterPath = tSPForCluster.provideClusterPath(clusterMap).apply {
            vertexList.removeFirst()
        }
        val clusters = clusterPath.vertexList.map { cluster ->
            val path = "src/main/resources/a280-gen1-50-cluster-${cluster.id}.oplib"
            problemWriter.writeCluster(
                problemSpace.metaData,
                problemSpace.distanceMatrix,
                cluster,
                path,
                (problemSpace.metaData.costLimit.toInt() * cluster.size) / clusterPath.vertexList.size
            )
            gkobeagaOpSolverClient.solve(path, objectMapper)?.let { solution ->
                cluster.solutionList = problemParser.addSolutionToProblem(cluster.solutionMap, solution)
                cluster
            }

        }

        visualizer.plotGraph(problemSpace.nodeMap, clusters.filterNotNull())
    }
}