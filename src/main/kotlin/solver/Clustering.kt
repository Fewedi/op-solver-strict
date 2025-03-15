package masterthesis.solver

import com.google.ortools.Loader
import com.google.ortools.graph.MinCostFlow
import com.google.ortools.graph.MinCostFlowBase
import masterthesis.config.ConfigProvider
import masterthesis.solver.model.Node
import org.slf4j.LoggerFactory
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class Clustering {

    private val logger = LoggerFactory.getLogger(Clustering::class.java)

    fun clusterKmeansFlow(nodeMap: Map<Int, Node>): Map<Int, List<Node>> {
        val nodes = nodeMap.values
        val meanClusterSize = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / meanClusterSize.toDouble()).toInt()
        return kmeansConstraint(nodes.toList(), k)
    }

    private fun kmeansConstraint(nodes: List<Node>, k: Int): Map<Int, List<Node>> {
        val max = ConfigProvider.config.parameter.clusterSize
        val nSamples = nodes.size
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        Loader.loadNativeLibraries();
        val clusters = mutableMapOf<Node, Int>()


        for (i in 0 until 100) {

            val minCostFlow = MinCostFlow()
            val distances = euclideanDistances(
                nodes.map { doubleArrayOf(it.x, it.y) }.toTypedArray(),
                centroids.map { doubleArrayOf(it.x, it.y) }.toTypedArray()
            )

            val nX = nSamples
            val nC = centroids.size
            val xix = (0 until nX).toList()
            val cDummyIx = (xix.last() + 1 until xix.last() + 1 + nC).toList()
            val cIx = (cDummyIx.last() + 1 until cDummyIx.last() + 1 + nC).toList()
            val artIx = cIx.last() + 1

            //edges

            val edgesXCDummy = xix.flatMap { x -> cDummyIx.map { c -> intArrayOf(x, c) } }
            val edgesXCDummy1 = cartesianProduct(xix.toIntArray(), cDummyIx.toIntArray())
            val edgesCDummyC = cDummyIx.zip(cIx) { cDummy, c -> intArrayOf(cDummy, c) }
            val edgesCArt = cIx.map { c -> intArrayOf(c, artIx) }

            val edges = listOf(edgesXCDummy, edgesCDummyC, edgesCArt).flatten()

            //costs
            val costsXCDummy = distances.flatMap { it.asIterable() }
            val costs = costsXCDummy + List(edges.size - costsXCDummy.size) { 0.0 }

            //Capacities - can set for max-k
            val capacitiesCDummyC = List(nC) { max }
            val capNon = nX  // The total supply and therefore wont restrict flow
            val capacities = List(edgesXCDummy.size) { 1 } + capacitiesCDummyC + List(nC) { capNon }

            //Sources and sinks
            val suppliesX = List(nX) { 1.0 }
            val suppliesC = List(nC) { 0.0 }
            val suppliesArt = -1 * (nX)  // Demand node
            val supplies = suppliesX + List(nC) { 0.0 } + suppliesC + listOf(suppliesArt)

            val nEdges = edges.size
            val nNodes = supplies.size

            //Add each edge with associated capacities and cost
            for (j in 0 until nEdges) {
                //logger.info("Edge: ${edges[j][0]} -> ${edges[j][1]}    ${capacities[j]}    ${costs[j]}")
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
            val flowValues = Array(nX * nC) { i ->
                val flow = minCostFlow.getFlow(i)
                val cost = minCostFlow.getFlow(i) * minCostFlow.getUnitCost(i)
                //logger.info("${minCostFlow.getTail(i)} -> ${minCostFlow.getHead(i)}    ${minCostFlow.getFlow(i)}  / ${minCostFlow.getCapacity( i)}     $cost")
                flow
            }
            val labelsM = Array(nX) { i ->
                flowValues.sliceArray(i * nC until (i + 1) * nC)
            }
            labelsM.forEachIndexed() { index, row ->
                clusters[nodes[index]] = row.indices.maxByOrNull { row[it] } ?: -1
            }
            val clusterDist = clusters.values.groupBy { it }.mapValues { it.value.size }
            logger.info("Cluster distribution: $clusterDist")
            val newCentroids = updateCentroids(clusters, k)
            if (newCentroids.all { it in centroids }) {
                break
            } else {
                centroids = newCentroids
            }
        }
        return clusters.entries.groupBy({ it.value }, { it.key }).apply {
            values.forEachIndexed { index, nodes ->
                nodes.forEach() { it.cluster = index }
            }
        }
    }

    // Function to calculate the Euclidean distance between two points
    private fun euclideanDistance(point1: DoubleArray, point2: DoubleArray): Double {
        return sqrt(point1.zip(point2) { a, b -> (a - b).toDouble().pow(2) }.sum())
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

    private fun cartesianProduct(arr1: IntArray, arr2: IntArray): List<IntArray> {
        return arr1.flatMap { x -> arr2.map { y -> intArrayOf(x, y) } }
    }

    fun clusterCapacitated(nodeMap: Map<Int, Node>): Map<Int, List<Node>> {
        val nodes = nodeMap.values.toList()
        val max = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / max.toDouble()).toInt()
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        var resultMap: Map<Int, List<Node>> = emptyMap()
        var i = 0
        while (i++ < 100) {
            val tempNodes = nodes.toMutableList()
            val clusterLists = List(k) { mutableListOf<Node>() }
            while (tempNodes.isNotEmpty()) {
                val ri = tempNodes.random()
                val distancesToClusters = centroids.map { it.distanceTo(ri) }
                val closestCentroid = centroids
                    .filterIndexed { index, _ -> clusterLists[index].size <= max }
                    .minByOrNull { distancesToClusters[centroids.indexOf(it)] }

                val tmep = tempNodes.map { node ->
                    val distToClusters = centroids.map { it.distanceTo(node) }
                    val closestCentroidOfNode = centroids
                        .filterIndexed { index, _ -> clusterLists[index].size <= max }
                        .minByOrNull { distToClusters[centroids.indexOf(it)] }
                    val bestDistToCluster = distToClusters[centroids.indexOf(closestCentroidOfNode)]
                    Triple(centroids.indexOf(closestCentroidOfNode), bestDistToCluster / node.revenue, node)
                }

                tmep.filter { it.first == centroids.indexOf(closestCentroid) }
                    .sortedBy { it.second }
                    .take(max - clusterLists[centroids.indexOf(closestCentroid) - 1].size)
                    .map { it.third }.let {
                        clusterLists[centroids.indexOf(closestCentroid)].addAll(it)
                        tempNodes.removeAll(it)
                    }
            }
            val newCentroids = clusterLists.map { cluster ->
                val x = cluster.map { it.x }.average()
                val y = cluster.map { it.y }.average()
                TempNode(x, y)
            }
            if (newCentroids.all { it in centroids } || i == 99) {
                resultMap = clusterLists.mapIndexed { index, finalNodes ->
                    finalNodes.forEach() { it.cluster = index }
                    index to finalNodes
                }.toMap()
            } else {
                centroids = newCentroids
            }
        }
        return resultMap
    }

    fun clusterCapacitatedCustom(nodeMap: Map<Int, Node>): Map<Int, List<Node>> {
        val nodes = nodeMap.values.toList()
        val max = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / max.toDouble()).toInt()
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        var resultMap: Map<Int, List<Node>> = emptyMap()
        var i = 0

        while (i++ < 100) {
            val tempNodes = nodes.toMutableList()
            val clusterLists = List(k) { mutableListOf<Node>() }
            while (tempNodes.isNotEmpty()) {
                tempNodes.map { node ->
                    clusterLists.filterIndexed { index, _ -> clusterLists[index].size >= max }
                    val distToClusters = centroids.map { it.distanceTo(node) }
                    val closestCentroidOfNode = centroids
                        .filterIndexed { index, _ -> clusterLists[index].size < max }
                        .minByOrNull { distToClusters[centroids.indexOf(it)] }
                    val bestDistToCluster = distToClusters[centroids.indexOf(closestCentroidOfNode)]
                    Triple(centroids.indexOf(closestCentroidOfNode), bestDistToCluster / node.revenue, node)
                }.groupBy { triple -> triple.first }
                    .map { clusterCandidates -> clusterCandidates.value.sortedBy { triple -> triple.second } }
                    .let { clusters ->
                        val amountOfValuesToSefForCluster = clusters.mapIndexed() { _, cluster ->
                            min(max - clusterLists[cluster.first().first].size, cluster.size)
                        }.min()
                        clusters.map { cluster ->
                            val finalList = cluster.take(amountOfValuesToSefForCluster)
                            clusterLists[cluster.first().first].addAll(finalList.map { it.third })
                            tempNodes.removeAll(finalList.map { it.third })
                        }
                    }

            }
            val newCentroids = clusterLists.map { cluster ->
                val x = cluster.map { it.x }.average()
                val y = cluster.map { it.y }.average()
                TempNode(x, y)
            }
            if (newCentroids.all { it in centroids } || i == 99) {
                resultMap = clusterLists.mapIndexed { index, finalNodes ->
                    finalNodes.forEach() { it.cluster = index }
                    index to finalNodes
                }.toMap()
            } else {
                centroids = newCentroids
            }
        }
        return resultMap
    }

    fun clusterKmeansUpperBoundIgnoreOutliers(nodeMap: Map<Int, Node>): Map<Int, List<Node>> {
        val nodes = nodeMap.values.toList().sortedByDescending { it.revenue }
        val max = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / max.toDouble()).toInt()
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        val resultList = MutableList(k) { mutableListOf<Node>() }
        val deadCluster = mutableListOf<Node>()

        for (i in 0 until 100) {
            resultList.map { it.clear() }
            nodes.forEach { node ->
                val closestCentroid = centroids.minBy { it.distanceTo(node) }
                if (resultList[centroids.indexOf(closestCentroid)].size <= max) {
                    resultList[centroids.indexOf(closestCentroid)].add(node)
                } else {
                    deadCluster.add(node)
                }
            }

            val newCentroids = resultList.map { cluster ->
                val x = cluster.map { it.x }.average()
                val y = cluster.map { it.y }.average()
                TempNode(x, y)
            }

            if (newCentroids.all { it in centroids }) {
                break
            } else {
                centroids = newCentroids
            }
            deadCluster.clear()
        }
        val startNode = nodes.first { it.startNode }
        if (deadCluster.contains(startNode)) {
            resultList.add(mutableListOf(startNode))
        }

        logger.info("Clustering did not consider ${deadCluster.size} of ${nodes.size} nodes")

        return resultList.mapIndexed { index, finalNodes ->
            finalNodes.forEach() { it.cluster = index }
            index to finalNodes
        }.toMap()
    }


    fun clusterKmeansUpperBound(nodeMap: Map<Int, Node>): Map<Int, List<Node>> {
        val nodes = nodeMap.values.toList()
        val max = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / max.toDouble()).toInt()
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        val resultList = List(k) { mutableListOf<Node>() }

        for (i in 0 until 100) {
            resultList.map { it.clear() }
            nodes.forEach { node ->
                val closestCentroid = centroids.filterIndexed { index, _ -> resultList[index].size <= max }
                    .minByOrNull { it.distanceTo(node) }
                resultList[centroids.indexOf(closestCentroid)].add(node)
            }

            val newCentroids = resultList.map { cluster ->
                val x = cluster.map { it.x }.average()
                val y = cluster.map { it.y }.average()
                TempNode(x, y)
            }

            if (newCentroids.all { it in centroids }) {
                break
            } else {
                centroids = newCentroids
            }
        }
        return resultList.mapIndexed { index, finalNodes ->
            finalNodes.forEach() { it.cluster = index }
            index to finalNodes
        }.toMap()
    }

    fun clusterKmeans(nodeMap: Map<Int, Node>): Map<Int, List<Node>> {
        val nodes = nodeMap.values
        val meanClusterSize = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / meanClusterSize.toDouble()).toInt()
        return kmeans(nodes.toList(), k)
    }

    private fun kmeans(nodes: List<Node>, k: Int): Map<Int, List<Node>> {
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        val clusters = mutableMapOf<Node, Int>()
        for (i in 0 until 100) {
            nodes.forEach { node ->
                val closestCentroid = centroids.minByOrNull { it.distanceTo(node) }
                clusters[node] = centroids.indexOf(closestCentroid)
            }

            val newCentroids = updateCentroids(clusters, k)
            if (newCentroids.all { it in centroids }) {
                break
            } else {
                centroids = newCentroids
            }
        }
        return clusters.entries.groupBy({ it.value }, { it.key })
    }

    fun clusterKmeansWithSplitting(nodeMap: Map<Int, Node>): Map<Int, List<Node>> {
        val nodes = nodeMap.values
        val meanClusterSize = ConfigProvider.config.parameter.clusterSize
        val k = ceil(nodes.size.toDouble() / meanClusterSize.toDouble()).toInt()
        var clusters = kmeans(nodes.toList(), k).values.toList()
        while (clusters.any { it.size >= meanClusterSize }) {
            clusters = clusters.map {
                if (it.size < meanClusterSize) {
                    listOf(it)
                } else {
                    val newK = ceil(it.size.toDouble() / meanClusterSize.toDouble()).toInt()
                    kmeans(it, newK).values
                }
            }.flatten().toList()
        }
        return clusters.mapIndexed { index, finalNodes ->
            finalNodes.forEach() { it.cluster = index }
            index to finalNodes
        }.toMap()
    }

    private fun updateCentroids(clusters: Map<Node, Int>, k: Int): List<TempNode> {
        return (0 until k).map { clusterIndex ->
            val clusterNodes = clusters.filter { it.value == clusterIndex }.keys
            val x = clusterNodes.map { it.x }.average()
            val y = clusterNodes.map { it.y }.average()
            TempNode(x, y)
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