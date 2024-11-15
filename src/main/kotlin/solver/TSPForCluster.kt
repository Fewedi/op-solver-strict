package masterthesis.solver

import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.tour.GreedyHeuristicTSP
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.MaskSubgraph
import solver.Node
import solver.Problem

class TSPForCluster {

    fun provideClusterPath(clusters: Map<Node, Int>, problem: Problem): GraphPath<ClusterNode, DefaultWeightedEdge> {
        val graph = provideGraphForCluster(clusters, problem.graph)
        addEdgesToGraph(graph)
        return GreedyHeuristicTSP<ClusterNode, DefaultWeightedEdge>().getTour(graph)
    }

    private fun addEdgesToGraph(graph: Graph<ClusterNode, DefaultWeightedEdge>) {
        graph.vertexSet().forEach { node ->
            graph.vertexSet().forEach { otherNode ->
                if (node != otherNode) {
                    val edge = graph.addEdge(node, otherNode)
                    if (edge != null) {
                        graph.setEdgeWeight(edge, node.distanceTo(otherNode))
                    }
                }
            }
        }
    }

    private fun provideGraphForCluster(
        clusters: Map<Node, Int>,
        graph: Graph<Node, DefaultWeightedEdge>
    ): Graph<ClusterNode, DefaultWeightedEdge> {
        return clusters.values.distinct().map { cluster ->
            val clusterNodes: Set<Node> = clusters.filter { it.value == cluster }.keys
            val test = AsSubgraph<Node, DefaultWeightedEdge>(graph, clusterNodes)

            ClusterNode(
                cluster,
                clusterNodes.map { it.x }.average(),
                clusterNodes.map { it.y }.average(),
                clusterNodes.size,
                subGraph = MaskSubgraph<solver.Node, DefaultWeightedEdge>(
                    graph,
                    { node -> !clusterNodes.contains(node) },
                    { edge -> false }
                )
            )

        }.let { clusterNodes ->
            DefaultUndirectedWeightedGraph<ClusterNode, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)
                .apply {
                    clusterNodes.forEach { addVertex(it) }
                }
        }
    }
}