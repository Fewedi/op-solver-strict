package masterthesis.solver

import com.google.ortools.Loader
import com.google.ortools.graph.MinCostFlow
import com.google.ortools.graph.MinCostFlowBase
import masterthesis.DataCapturing
import masterthesis.config.AggregationMethod
import masterthesis.config.ConfigProvider
import masterthesis.solver.model.Node
import org.jetbrains.kotlinx.dataframe.math.median
import org.slf4j.LoggerFactory
import kotlin.collections.map
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

class Clustering {

    private val logger = LoggerFactory.getLogger(Clustering::class.java)

    fun clusterKmeansFlow(nodeMap: Map<Int, Node>, statistic: AggregationMethod): Map<Int, List<Node>> {
        val nodes = nodeMap.values
        val meanClusterSize = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / meanClusterSize.toDouble()).toInt()
        return kmeansConstraint(nodes.toList(), k, statistic)
    }

    private fun kmeansConstraint(nodes: List<Node>, k: Int, statistic: AggregationMethod): Map<Int, List<Node>> {
        val max = ConfigProvider.config.parameter.clusterSize
        val nSamples = nodes.size
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        Loader.loadNativeLibraries()
        val clusters = mutableMapOf<Node, Int>()


        for (i in 0 until 100) {
            val minCostFlow = MinCostFlow()
            val distances = euclideanDistances(
                nodes.map { doubleArrayOf(it.x, it.y) }.toTypedArray(),
                centroids.map { doubleArrayOf(it.x, it.y) }.toTypedArray()
            )

            val nC = centroids.size
            val xix = (0 until nSamples).toList()
            val cDummyIx = (xix.last() + 1 until xix.last() + 1 + nC).toList()
            val cIx = (cDummyIx.last() + 1 until cDummyIx.last() + 1 + nC).toList()
            val artIx = cIx.last() + 1

            //edges

            val edgesXCDummy = xix.flatMap { x -> cDummyIx.map { c -> intArrayOf(x, c) } }
            val edgesCDummyC = cDummyIx.zip(cIx) { cDummy, c -> intArrayOf(cDummy, c) }
            val edgesCArt = cIx.map { c -> intArrayOf(c, artIx) }

            val edges = listOf(edgesXCDummy, edgesCDummyC, edgesCArt).flatten()

            //costs
            val costsXCDummy = distances.flatMap { it.asIterable() }
            val costs = costsXCDummy + List(edges.size - costsXCDummy.size) { 0.0 }

            //Capacities - can set for max-k
            val capacitiesCDummyC = List(nC) { max }
            val capNon = nSamples  // The total supply and therefore wont restrict flow
            val capacities = List(edgesXCDummy.size) { 1 } + capacitiesCDummyC + List(nC) { capNon }

            //Sources and sinks
            val suppliesX = List(nSamples) { 1.0 }
            val suppliesC = List(nC) { 0.0 }
            val suppliesArt = -1 * (nSamples)  // Demand node
            val supplies = suppliesX + List(nC) { 0.0 } + suppliesC + listOf(suppliesArt)

            //Add each edge with associated capacities and cost
            for (j in edges.indices) {
                minCostFlow.addArcWithCapacityAndUnitCost(
                    edges[j][0],
                    edges[j][1],
                    capacities[j].toLong(),
                    costs[j].toLong()
                )
            }
            for (j in supplies.indices) {
                minCostFlow.setNodeSupply(j, supplies[j].toLong())
            }
            if (minCostFlow.solve() != MinCostFlowBase.Status.OPTIMAL)
                logger.error("There was an issue with the min cost flow input.")

            //Assignment
            val flowValues = Array(nSamples * nC) { i ->
                val flow = minCostFlow.getFlow(i)
                flow
            }
            val labelsM = Array(nSamples) { i ->
                flowValues.sliceArray(i * nC until (i + 1) * nC)
            }
            labelsM.forEachIndexed() { index, row ->
                clusters[nodes[index]] = row.indices.maxByOrNull { row[it] } ?: -1
            }

            val newCentroids = updateCentroids(clusters, k, statistic)
            if (newCentroids.all { it in centroids }) {
                break
            } else {
                centroids = newCentroids
            }
        }
        return clusters.entries.groupBy({ it.value }, { it.key }).apply {
            entries.forEach() { entry ->
                val nodes = entry.value
                val index = entry.key
                nodes.forEach() { it.cluster = index }
            }
        }
    }

    // Function to calculate the Euclidean distance between two points
    private fun euclideanDistance(point1: DoubleArray, point2: DoubleArray): Double {
        return sqrt(point1.zip(point2) { a, b -> (a - b).pow(2) }.sum())
    }

    // Function to calculate Euclidean distances between all points in X and C
    private fun euclideanDistances(
        x: Array<DoubleArray>,
        c: Array<DoubleArray>,
        squared: Boolean = false
    ): Array<DoubleArray> {
        val distances = Array(x.size) { DoubleArray(c.size) }
        for (i in x.indices) {
            for (j in c.indices) {
                val dist = euclideanDistance(x[i], c[j])
                distances[i][j] = if (squared) dist.pow(2) else dist
            }
        }
        return distances
    }

    fun clusterKmeansUpperBoundIgnoreOutliers(
        nodeMap: Map<Int, Node>,
        statistic: AggregationMethod
    ): Map<Int, List<Node>> {
        val nodes = nodeMap.values.toList().sortedByDescending { if (it.startNode) Int.MAX_VALUE else it.revenue }
        val max = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / max.toDouble()).toInt()
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        val resultList = List(k) { mutableListOf<Node>() }
        val deadCluster = mutableListOf<Node>()

        for (i in 0 until 100) {
            deadCluster.clear()
            resultList.map { it.clear() }
            nodes.forEach { node ->
                val closestCentroid = centroids.minBy { it.distanceTo(node) }
                if (resultList[centroids.indexOf(closestCentroid)].size < max) {
                    resultList[centroids.indexOf(closestCentroid)].add(node)
                } else {
                    deadCluster.add(node)
                }
            }

            val newCentroids = updateCentroids(resultList, statistic)

            if (newCentroids.all { it in centroids }) {
                break
            } else {
                centroids = newCentroids
            }
        }
        DataCapturing.addNodesInDeadCluster(deadCluster.size.toDouble()/nodes.size.toDouble())
        DataCapturing.addRevenueInDeadCluster(deadCluster.sumOf { it.revenue!! }.toDouble() / nodes.sumOf { it.revenue!! }.toDouble())
        logger.info("Clustering did not consider ${deadCluster.size} of ${nodes.size} nodes")

        return resultList.mapIndexed { index, finalNodes ->
            finalNodes.forEach() { it.cluster = index }
            index to finalNodes
        }.toMap()
    }

    fun clusterKmeans(nodeMap: Map<Int, Node>, statistic: AggregationMethod): Map<Int, List<Node>> {
        val nodes = nodeMap.values
        val meanClusterSize = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / meanClusterSize.toDouble()).toInt()
        return kmeans(nodes.toList(), k, statistic)
    }

    private fun kmeans(nodes: List<Node>, k: Int, statistic: AggregationMethod): Map<Int, List<Node>> {
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        val clusters = mutableMapOf<Node, Int>()
        for (i in 0 until 100) {
            nodes.forEach { node ->
                val closestCentroid = centroids.minByOrNull { it.distanceTo(node) }
                clusters[node] = centroids.indexOf(closestCentroid)
            }

            val newCentroids = updateCentroids(clusters, k, statistic)
            if (newCentroids.all { it in centroids }) {
                break
            } else {
                centroids = newCentroids
            }
        }
        return clusters.entries.groupBy({ it.value }, { it.key })
    }

    fun clusterKmeansWithSplitting(nodeMap: Map<Int, Node>, statistic: AggregationMethod): Map<Int, List<Node>> {
        val nodes = nodeMap.values
        val maxClusterSize = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / maxClusterSize.toDouble()).toInt()
        var clusters = kmeans(nodes.toList(), k, statistic).values.toList()
        while (clusters.any { it.size > maxClusterSize }) {
            clusters = clusters.map {
                if (it.size < maxClusterSize) {
                    listOf(it)
                } else {
                    val newK = ceil(it.size.toDouble() / maxClusterSize.toDouble()).toInt()
                    kmeans(it, newK, statistic).values
                }
            }.flatten().toList()
        }
        return clusters.mapIndexed { index, finalNodes ->
            finalNodes.forEach() { it.cluster = index }
            index to finalNodes
        }.toMap()
    }

    private fun updateCentroids(clusters: Map<Node, Int>, k: Int, metric: AggregationMethod): List<TempNode> {
        return when (metric) {
            AggregationMethod.MEAN -> {
                (0 until k).map { clusterIndex ->
                    val clusterNodes = clusters.filter { it.value == clusterIndex }.keys
                    val x = clusterNodes.map { it.x }.average()
                    val y = clusterNodes.map { it.y }.average()
                    TempNode(x, y)
                }
            }

            AggregationMethod.MEDIAN -> {
                (0 until k).map { clusterIndex ->
                    val clusterNodes = clusters.filter { it.value == clusterIndex }.keys
                    val x = clusterNodes.map { it.x }.median()
                    val y = clusterNodes.map { it.y }.median()
                    TempNode(x, y)
                }
            }
        }

    }

    private fun updateCentroids(resultList: List<List<Node>>, metric: AggregationMethod): List<TempNode> {
        return when (metric) {
            AggregationMethod.MEAN -> {
                resultList.map { cluster ->
                    val x = cluster.map { it.x }.average()
                    val y = cluster.map { it.y }.average()
                    TempNode(x, y)
                }
            }

            AggregationMethod.MEDIAN -> {
                resultList.map { cluster ->
                    val x = cluster.map { it.x }.median()
                    val y = cluster.map { it.y }.median()
                    TempNode(x, y)
                }
            }
        }
    }

    data class TempNode(
        val x: Double,
        val y: Double
    ) {
        fun distanceTo(other: Node): Double {
            val xDiff = x - other.x
            val yDiff = y - other.y
            return sqrt(xDiff * xDiff + yDiff * yDiff)
        }
    }
}