package masterthesis.solver.model

import kotlin.math.sqrt

data class Node(
    val id: Int,
    val x: Double,
    val y: Double,
    var revenue: Int = 1,
    var startNode: Boolean = false,
    var endNode: Boolean = false,
    val distanceMap: MutableMap<Int, Double> = mutableMapOf(),
    var writtenPosition: Int = -1,
    var cluster: Int = -1,
) {
    fun distanceTo(other: Node): Double {
        val xDiff = x - other.x
        val yDiff = y - other.y
        return sqrt(xDiff * xDiff + yDiff * yDiff)
    }
}