package masterthesis.evaluation

import masterthesis.DataCapturing
import masterthesis.evaluation.model.Result
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.ProblemSpace
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode

class Evaluater {

    private val logger = LoggerFactory.getLogger(Evaluater::class.java)
    fun evaluateResult(problemSpace: ProblemSpace, clusters: List<Cluster>, duration: Double, name: String,clusterPath: List<Cluster>, visualizer: Visualizer) : Result {


        var isValid = true
        val finalPath = clusters.map { it.solutionList }.flatten()
        val totalRevenue = finalPath.sumOf { it.revenue }
        val totalCost = finalPath.zipWithNext().sumOf { it.first.distanceTo(it.second) }

        DataCapturing.addClustersInPath(clusters.size, clusterPath.size)
        DataCapturing.addClustersIncludedCompletely(clusterPath.filter { it.size <= it.solutionList.size - 1 }.size , clusterPath.size)
        DataCapturing.addPercentageBudgetUnused(totalCost, problemSpace.metaData.costLimit.toDouble())

        visualizer.plotGraph(problemSpace.nodeMap, clusters, name, totalRevenue)
        logger.info("------ FINAL RESULTS ------")
        logger.info("Final path: ${finalPath.map { it.id }}")
        logger.info("Total revenue: $totalRevenue")

        if (finalPath.first().id != problemSpace.metaData.startNode.toInt() - 1) {
            isValid = false
            logger.error("First node is not the start node")
        }

        if (finalPath.distinct().size != finalPath.size) {
            isValid = false
            logger.error("Path contains duplicates")
        }

        if (totalCost > problemSpace.metaData.costLimit.toInt()) {
            isValid = false
            logger.error("Total cost $totalCost is higher than cost limit ${problemSpace.metaData.costLimit}")
        } else {
            logger.info("Total cost $totalCost is lower than cost limit ${problemSpace.metaData.costLimit}")
        }

        return Result(
            name,
            problemSpace.nodeMap.size,
            finalPath,
            problemSpace.metaData.costLimit.toDouble().toInt(),
            isValid,
            totalRevenue,
            BigDecimal(totalCost).setScale(2, RoundingMode.HALF_EVEN).toDouble(),
            BigDecimal(duration).setScale(2, RoundingMode.HALF_EVEN).toDouble(),
        )
    }
}