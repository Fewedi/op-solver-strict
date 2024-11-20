package masterthesis.solver.model

import kotlin.math.sqrt

data class Cluster(
    val id: Int,
    val x: Double,
    val y: Double,
    val nodes: Collection<Node>,
    val size: Int,
    val solutionMap: MutableMap<Int, Node> = mutableMapOf(),
) {
    lateinit var solutionList: List<Node>

    fun distanceTo(otherNode: Cluster): Double {
        val xDiff = x - otherNode.x
        val yDiff = y - otherNode.y
        return sqrt(xDiff * xDiff + yDiff * yDiff)
    }
}
