package masterthesis.solver.legacy

import masterthesis.config.ConfigProvider
import masterthesis.solver.Clustering.TempNode
import masterthesis.solver.model.Node
import kotlin.collections.indexOf
import kotlin.math.ceil
import kotlin.math.min

class ClusteringDepr {
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
                    Triple(centroids.indexOf(closestCentroidOfNode), bestDistToCluster / node.revenue!!, node)
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
                    Triple(centroids.indexOf(closestCentroidOfNode), bestDistToCluster / node.revenue!!, node)
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

}

