package masterthesis.solver.model

import kotlin.math.sqrt

data class Node(
    val id: Int,
    val x: Double,
    val y: Double,
    val revenue: Int,
    var startNode: Boolean = false,
    var endNode: Boolean = false,
    val distanceMap: MutableMap<Int, Double> = mutableMapOf(),
    var writtenPosition: Int = -1,
    var cluster: Int = -1,
    var distanceToPrevCluster: Double = 0.0,
    var distanceToNextCluster: Double = 0.0
) {

    fun distanceTo(other: Node): Double {
        val xDiff = x - other.x
        val yDiff = y - other.y
        return sqrt(xDiff * xDiff + yDiff * yDiff)
    }

    fun distanceTo(cluster: Cluster): Double {
        val xDiff = x - cluster.x
        val yDiff = y - cluster.y
        return sqrt(xDiff * xDiff + yDiff * yDiff)
    }
}