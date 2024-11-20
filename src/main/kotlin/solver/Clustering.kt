package masterthesis.solver

import masterthesis.solver.model.Node
import org.slf4j.LoggerFactory
import solver.Visualizer
import kotlin.math.sqrt

class Clustering {

    private val logger = LoggerFactory.getLogger(Visualizer::class.java)


    fun cluster(nodeMap: Map<Int, Node>, costLimit: Int): Map<Int, List<Node>> {
        val nodes = nodeMap.values
        val meanClusterSize = 20
        val k = nodes.size / meanClusterSize
        val clusters = applyKmeans(nodes, k)
        return clusters.entries.groupBy({ it.value }, { it.key })
    }

    private fun applyKmeans(nodes: Collection<Node>, k: Int): Map<Node, Int> {
        var centroids = nodes.shuffled().take(k).map { TempNode(it.x, it.y) }
        val clusters = mutableMapOf<Node, Int>()

        for (i in 0 until 100) {
            nodes.forEach { node ->
                val closestCentroid = centroids.minByOrNull { it.distanceTo(node) }
                clusters[node] = centroids.indexOf(closestCentroid)
            }

            centroids = updateCentroids(clusters, k)
        }
        nodes.forEach { node ->
            node.cluster = clusters.getOrElse(node) {
                logger.error("Node $node not found in clusters")
                -1
            }
        }
        return clusters
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