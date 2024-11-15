package masterthesis.solver

import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.MaskSubgraph
import solver.Node
import kotlin.math.sqrt

data class ClusterNode(
    val cluster: Int,
    val x: Double,
    val y: Double,
    val size: Int,
    val startCluster: Boolean = false,
    val subGraph: MaskSubgraph<Node, DefaultWeightedEdge>
){
    fun distanceTo(other: ClusterNode): Double {
        val xDiff = x - other.x
        val yDiff = y - other.y
        return sqrt(xDiff * xDiff + yDiff * yDiff)
    }
}
