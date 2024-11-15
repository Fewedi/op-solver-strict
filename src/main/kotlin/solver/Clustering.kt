package masterthesis.solver

import solver.Node
import solver.Problem

class Clustering {

    fun cluster(problem: Problem): Map<Node, Int> {
        val meanClusterSize = 20
        val nodes = problem.graph.vertexSet()

        val k = nodes.size / meanClusterSize
        return kmeans(problem.graph.vertexSet(), k)
    }

    private fun kmeans(nodes: Set<Node>, k: Int): Map<Node, Int> {
        var centroids = nodes.shuffled().take(k)
        val clusters = mutableMapOf<Node, Int>()

        for (i in 0 until 100) {
            nodes.forEach { node ->
                val closestCentroid = centroids.minByOrNull { it.distanceTo(node) }
                clusters[node] = centroids.indexOf(closestCentroid)
            }

            centroids = updateCentroids(nodes, clusters, k)
        }
        nodes.forEach { node ->
            node.cluster = clusters[node]!!
        }
        return clusters
    }

    private fun updateCentroids(nodes: Set<Node>, clusters: Map<Node, Int>, k: Int): List<Node> {
        return (0 until k).map { clusterIndex ->
            val clusterNodes = clusters.filter { it.value == clusterIndex }.keys
            val x = clusterNodes.map { it.x }.average()
            val y = clusterNodes.map { it.y }.average()
            Node(-1, x, y)
        }
    }
}