package masterthesis.solver.model

import kotlin.math.sqrt

data class Cluster(
    val id: Int,
    val x: Double,
    val y: Double,
    val nodes: MutableList<Node>,
    val size: Int,
    val isStart: Boolean,
    val solutionMap: MutableMap<Int, Node> = mutableMapOf(),
    var nextCluster: Cluster? = null,
    var prevCluster: Cluster? = null,
    val startNodes: MutableList<Node> = mutableListOf(),
    val endNodes: MutableList<Node> = mutableListOf(),
    var budget: Double = -1.0,
    val convexHull: List<Node>,
    val convexSize: Double,
    val revenue: Int,
) {
    lateinit var solutionList: List<Node>

    fun distanceTo(otherNode: Cluster): Double {
        val xDiff = x - otherNode.x
        val yDiff = y - otherNode.y
        return sqrt(xDiff * xDiff + yDiff * yDiff)
    }
}
