package solver

import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.GraphWalk

data class Problem(val path: String) {
    lateinit var name: String
    lateinit var comment: String
    lateinit var type: String
    lateinit var dimension: String
    lateinit var costLimit: String
    lateinit var edgeWeightType: String
    lateinit var nodeMap: MutableMap<Int, Node>
    lateinit var graph: Graph<Node, DefaultWeightedEdge>
    lateinit var savedPath: String
    lateinit var walk: GraphWalk<Node, DefaultWeightedEdge>
    var clusterLists: MutableMap<Int,List<Node>> = mutableMapOf()
    var clusterWalks: MutableMap<Int,GraphWalk<Node, DefaultWeightedEdge>> = mutableMapOf()
}