package masterthesis.solver

import masterthesis.config.ConfigProvider
import masterthesis.solver.model.Node
import org.slf4j.LoggerFactory
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

class Clustering {

    private val logger = LoggerFactory.getLogger(Clustering::class.java)

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
                val distancesToClusters = centroids.map { it.distanceTo(ri)}
                val closestCentroid = centroids
                    .filterIndexed { index, _ ->  clusterLists[index].size <= max  }
                    .minByOrNull { distancesToClusters[centroids.indexOf(it)] }

                val tmep = tempNodes.map { node ->
                    val distToClusters = centroids.map { it.distanceTo(node)}
                    val closestCentroidOfNode = centroids
                        .filterIndexed { index, _ ->  clusterLists[index].size <= max  }
                        .minByOrNull { distToClusters[centroids.indexOf(it)] }
                    val bestDistToCluster = distToClusters[centroids.indexOf(closestCentroidOfNode)]
                    Triple(centroids.indexOf(closestCentroidOfNode) ,bestDistToCluster / node.revenue, node)
                }

                tmep.filter { it.first == centroids.indexOf(closestCentroid) }
                    .sortedBy { it.second }
                    .take(max - clusterLists[centroids.indexOf(closestCentroid) -1].size)
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
            }else {
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
                val closestCentroid = centroids.filterIndexed { index, _ ->  resultList[index].size <= max }
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
        while (clusters.any {it.size >= meanClusterSize}) {
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