package masterthesis.solver

import masterthesis.solver.config.AgregationMethod
import masterthesis.solver.config.ConfigProvider
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import org.jgrapht.Graph
import org.jgrapht.alg.tour.GreedyHeuristicTSP
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.slf4j.LoggerFactory
import solver.ProblemParser

class TSPForCluster {

    private val logger = LoggerFactory.getLogger(ProblemParser::class.java)
    private val startNodeProvider = StartNodeProvider()

    fun provideClusterPath(clusters: Map<Int, List<Node>>): List<Cluster> {
        val graph = provideGraphForCluster(clusters)
        addEdges(graph)
        return GreedyHeuristicTSP<Cluster, DefaultWeightedEdge>().getTour(graph)
            .vertexList.apply {
                if (first() == last()) {
                    removeLast()
                }
                val startIndexOfCluster = indexOfFirst { it.isStart }
                for (i in 0 until startIndexOfCluster) {
                    add(first())
                    removeFirst()
                }
                forEachIndexed { index, cluster ->
                    if (cluster != last()) {
                        cluster.nextCluster = get(index + 1)
                        if (ConfigProvider.config.dummyStartNodeMethod == AgregationMethod.MEAN) {
                            cluster.nodes.forEach { node ->
                                node.distanceToNextCluster = node.distanceTo(cluster.nextCluster!!)
                            }
                        }
                    }
                    if (cluster != first()) {
                        cluster.prevCluster = get(index - 1)
                        if (ConfigProvider.config.dummyStartNodeMethod == AgregationMethod.MEAN) {
                            cluster.nodes.forEach { node ->
                                node.distanceToPrevCluster = node.distanceTo(cluster.prevCluster!!)
                            }
                        }
                    }
                    cluster.startNodes.add(startNodeProvider.findStartNode(cluster))
                }
                forEach {
                    val finalNode = startNodeProvider.findEndNode(it)
                    it.endNodes.add(finalNode)
                }
            }
    }

    private fun provideGraphForCluster(clusters: Map<Int, List<Node>>): Graph<Cluster, DefaultWeightedEdge> {
        return clusters.map { cluster ->
            Cluster(
                id = cluster.key,
                cluster.value.map { it.x }.average(),
                cluster.value.map { it.y }.average(),
                cluster.value.toMutableList(),
                cluster.value.size,
                cluster.value.any { it.startNode },
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