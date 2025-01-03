package masterthesis.solver

import masterthesis.solver.config.AgregationMethod
import masterthesis.solver.config.ConfigProvider
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.FinalNodesMode
import masterthesis.solver.model.Node
import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.tour.GreedyHeuristicTSP
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.slf4j.LoggerFactory
import solver.ProblemParser

class TSPForCluster {

    private val logger = LoggerFactory.getLogger(ProblemParser::class.java)
    private val startNodeProvider = StartNodeProvider()

    fun provideClusterPath(clusters: Map<Int, List<Node>>): GraphPath<Cluster, DefaultWeightedEdge> {
        val graph = provideGraphForCluster(clusters)
        addEdges(graph)
        return GreedyHeuristicTSP<Cluster, DefaultWeightedEdge>().getTour(graph).apply {
            vertexList.forEachIndexed { index, cluster ->
                if (cluster != endVertex) {
                    cluster.nextCluster = vertexList[index + 1]
                    if (ConfigProvider.config.dummyStartNodeMethod == AgregationMethod.MEAN) {
                        cluster.nodes.forEach { node ->
                            node.distanceToNextCluster = node.distanceTo(cluster.nextCluster!!)
                        }
                    }
                }
                if (cluster != startVertex) {
                    cluster.prevCluster = vertexList[index - 1]
                    if (ConfigProvider.config.dummyStartNodeMethod == AgregationMethod.MEAN) {
                        cluster.nodes.forEach { node ->
                            node.distanceToPrevCluster = node.distanceTo(cluster.prevCluster!!)
                        }
                    }
                }
                cluster.startNodes.add(startNodeProvider.findStartNode(cluster))
            }
            vertexList.removeLast()
            vertexList.forEach{
                //val finalNode = startNodeProvider.findEndNode(it) ?: it.nodes.last()
                it.endNodes.add(it.nodes.last())
                //it.endNodes.add(finalNode)
                //if (it != endVertex)it.nodes.add(finalNode) // todo: account for final cluster
                it.finalNodesMode = if (it == endVertex) FinalNodesMode.SAME_CLUSTER else FinalNodesMode.SEPARATE_CLUSTER

            }
            logger.info("Cluster path: ${vertexList.map { it.id }}")
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
                FinalNodesMode.SEPARATE_CLUSTER
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