package masterthesis.solver

import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.tour.GreedyHeuristicTSP
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge

class TSPForCluster {

    fun provideClusterPath(clusters: Map<Int, Collection<Node>>): GraphPath<Cluster, DefaultWeightedEdge> {
        val graph = provideGraphForCluster(clusters)
        addEdges(graph)
        return GreedyHeuristicTSP<Cluster, DefaultWeightedEdge>().getTour(graph)
    }

    private fun provideGraphForCluster(clusters: Map<Int, Collection<Node>>): Graph<Cluster, DefaultWeightedEdge> {
        return clusters.map { cluster ->
            Cluster(
                id = cluster.key,
                cluster.value.map { it.x }.average(),
                cluster.value.map { it.y }.average(),
                cluster.value,
                cluster.value.size
            )
        }.let { clusterNodes ->
            DefaultUndirectedWeightedGraph<Cluster, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)
                .apply {
                    clusterNodes.forEach { addVertex(it) }
                }
        }
    }

    private fun addEdges(graph: Graph<Cluster, DefaultWeightedEdge>) {
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

}