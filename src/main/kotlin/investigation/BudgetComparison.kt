package masterthesis.investigation

import masterthesis.Solver
import masterthesis.evaluation.CsvClient
import masterthesis.solver.Clustering
import masterthesis.solver.ClusterPathGenerator
import masterthesis.config.ClusteringMethod
import masterthesis.config.ConfigProvider
import masterthesis.evaluation.Visualizer
import masterthesis.solver.ConvexHullGrahamScan
import masterthesis.solver.StartNodeProvider
import masterthesis.solver.model.Cluster
import solver.ProblemParser
import java.math.BigDecimal
import java.math.RoundingMode

class BudgetComparison {

    private val problemParser = ProblemParser()
    private val clustering = Clustering()
    private val tSPForCluster = ClusterPathGenerator()
    private val solver = Solver()
    private val csvClient = CsvClient()
    private val visualizer = Visualizer()
    private val startNodeProvider = StartNodeProvider()
    private val convexHullGrahamScan = ConvexHullGrahamScan()

    fun prepareMultipleRuns(folderName: List<String>, gen: String) {
        val results = folderName.mapNotNull {
            try {
                compareBudgets(it, gen)
            } catch (e: Exception) {
                null
            }
        }
        csvClient.writeCsv(results.flatten(), "budget-comparison.csv", "results/")
        visualizer.plotClusterMetrics(results.flatten(), "_meanDistToMean", "additionalRevenueToBudget")
        visualizer.plotClusterMetrics(results.flatten(), listOf("_size", "_startToMean", "_endToMean", "_potentialRevenue", "_meanDistToMean", "_meanDetour", "budgetSmall", "budgetBig" ), "additionalRevenueToBudget")
        visualizer.plotClusterMetrics(results.flatten(), listOf("_size", "_startToMean", "_endToMean", "_potentialRevenue", "_meanDistToMean", "_meanDetour", "budgetSmall", "budgetBig" ), "revenueDif")
    }

    fun compareBudgets(folderName: String, gen: String): List<ClusterMetric>? {

        val problemSpace = problemParser.readProblemSpace(folderName, gen)

        val max = problemSpace.nodeMap.values.map { node -> problemSpace.nodeMap.values.map{ it.distanceTo(node) }}.flatten().max()

        val statistic = ConfigProvider.config.algorithm.clusteringStatistic
        val clusterMap = when (ConfigProvider.config.algorithm.clustering) {
            ClusteringMethod.KMEANSUPPERBOUNDIGNOREOUTLIERS -> { clustering.clusterKmeansUpperBoundIgnoreOutliers(problemSpace.nodeMap,statistic) }
            ClusteringMethod.KMEANS, ClusteringMethod.KMEANSANDCORRECTLATER -> { clustering.clusterKmeans(problemSpace.nodeMap,statistic) }
            ClusteringMethod.KMEANSSPLIT -> { clustering.clusterKmeansWithSplitting(problemSpace.nodeMap,statistic) }
            ClusteringMethod.KMEANSFLOW -> { clustering.clusterKmeansFlow(problemSpace.nodeMap,statistic) }
        }

        val clusterPath = tSPForCluster.provideClusterPathConcorde(clusterMap, convexHullGrahamScan)
        tSPForCluster.setDistancesToNextClusterAndProvideStartNodes(clusterPath, startNodeProvider)

        val bigBudgets = clusterPath.map { it.startNodes.first().distanceTo(it.endNodes.first()) * 3 }
        val smallBudgets = clusterPath.map { it.startNodes.first().distanceTo(it.endNodes.first()) * 1.5 }

        val bigResults = clusterPath.mapIndexed { index, cluster ->
            solver.solveWithGurobi(cluster, bigBudgets[index])
            cluster.solutionList.sumOf { it.revenue!! }
        }
        val smallResults = clusterPath.mapIndexed { index, cluster ->
            solver.solveWithGurobi(cluster, smallBudgets[index])
            cluster.solutionList.sumOf { it.revenue!! }
        }

        val clusterMetrics = clusterPath.mapIndexed { index, cluster ->
            getClusterMetrics(
                cluster,
                smallBudgets[index].toInt(),
                bigBudgets[index].toInt(),
                smallResults[index],
                bigResults[index],
                folderName,
                max
            )
        }
        return clusterMetrics
    }


    private fun getClusterMetrics(
        cluster: Cluster,
        budgetSmall: Int,
        budgetBig: Int,
        revenueSmall: Int,
        revenueBig: Int,
        name: String,
        max: Double
    ): ClusterMetric {

        val endToMean = if (cluster.endNodes.isNotEmpty()) {
            BigDecimal.valueOf(cluster.endNodes.map { it.distanceTo(cluster, max) }.average()).setScale(4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        val startToEnd = if (cluster.endNodes.isNotEmpty()) {
            BigDecimal.valueOf(cluster.startNodes.map { it.distanceTo(cluster.endNodes.first(), max) }.average()).setScale(4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        val meanDetour = if (cluster.endNodes.isNotEmpty()) {
            BigDecimal.valueOf(cluster.nodes.map {
                it.distanceTo(cluster.startNodes.first(), max) + it.distanceTo(cluster.endNodes.first(), max) - cluster.startNodes.first()
                    .distanceTo(cluster.endNodes.first(), max)
            }.average()).setScale(4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        val meanDetourRelativeToRevenue = if (cluster.endNodes.isNotEmpty()) {
            BigDecimal.valueOf(cluster.nodes.map {
                it.distanceTo(cluster.startNodes.first(), max) + it.distanceTo(cluster.endNodes.first(), max) - cluster.startNodes.first()
                    .distanceTo(cluster.endNodes.first(), max)
            }.sum() / cluster.solutionList.sumOf { it.revenue!! }).setScale(4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        return ClusterMetric(
            _name = name,
            _id = cluster.id,
            _x = BigDecimal.valueOf(cluster.x),
            _y = BigDecimal.valueOf(cluster.y),
            _isStart = cluster.isStart,
            _isEnd = cluster.endNodes.isEmpty(),
            _size = cluster.size,
            _startToMean = BigDecimal.valueOf(cluster.nodes.map { it.distanceTo(cluster, max) }.average()).setScale(4, RoundingMode.HALF_UP),
            _endToMean = endToMean,
            _startToEnd = startToEnd,
            _meanDistToMean = BigDecimal.valueOf(cluster.nodes.map { it.distanceTo(cluster, max) }.average()).setScale(4, RoundingMode.HALF_UP),
            _meanDetour = meanDetour,
            _potentialRevenue = cluster.nodes.sumOf { it.revenue!! },

            budgetSmall = budgetSmall,
            budgetBig = budgetBig,
            revenueSmall = revenueSmall,
            revenueBig = revenueBig,
            revenueSum = cluster.nodes.sumOf { it.revenue!!},
            revenueMean = BigDecimal.valueOf(cluster.nodes.map { it.revenue!! }.average()).setScale(4, RoundingMode.HALF_UP),
            revenueDif = revenueBig - revenueSmall,
            additionalRevenueToBudget = BigDecimal(revenueBig - revenueSmall).setScale(4, RoundingMode.HALF_UP).div(BigDecimal(budgetBig - budgetSmall).setScale(4, RoundingMode.HALF_UP)),
            meanDetourRelativeToRevenue = meanDetourRelativeToRevenue,
        )
    }
}