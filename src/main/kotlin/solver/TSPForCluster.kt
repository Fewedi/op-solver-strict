package masterthesis.solver

import masterthesis.solver.config.AgregationMethod
import masterthesis.solver.config.ConfigProvider
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import org.slf4j.LoggerFactory
import solver.ProblemParser

class TSPForCluster {

    private val logger = LoggerFactory.getLogger(ProblemParser::class.java)
    private val startNodeProvider = StartNodeProvider()

    fun provideClusterPathConcorde(clusters: Map<Int, List<Node>>): List<Cluster> {
        val clusterList = clusters.map {
            Cluster(
                id = it.key,
                x = it.value.map { it.x }.average(),
                y = it.value.map { it.y }.average(),
                nodes = it.value.toMutableList(),
                size = it.value.size,
                isStart = it.value.any { it.startNode },
            )
        }
        val startCluster = clusterList.find { it.isStart } ?: clusterList.first()
        val finalPath = ConcordeClient().solve(
            clusterList,
            startCluster
        )
        finalPath.forEachIndexed { index, cluster ->
            if (index != finalPath.size - 1) {
                cluster.nextCluster = finalPath[index + 1]
                if (ConfigProvider.config.dummyStartNodeMethod == AgregationMethod.MEAN) {
                    cluster.nodes.forEach { node ->
                        node.distanceToNextCluster = node.distanceTo(cluster.nextCluster!!)
                    }
                }
            }
            if (index != 0) {
                cluster.prevCluster = finalPath[index - 1]
                if (ConfigProvider.config.dummyStartNodeMethod == AgregationMethod.MEAN) {
                    cluster.nodes.forEach { node ->
                        node.distanceToPrevCluster = node.distanceTo(cluster.prevCluster!!)
                    }
                }
            }
            cluster.startNodes.add(startNodeProvider.findStartNode(cluster))
        }
        finalPath.forEach { cluster ->
            cluster.endNodes.add(startNodeProvider.findEndNode(cluster))
        }

        logger.info("Concord solution: ${finalPath.map { it.id }}")

        return finalPath
    }
}