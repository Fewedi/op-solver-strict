package masterthesis.solver

import masterthesis.solver.model.Cluster
import org.slf4j.LoggerFactory

class ClusterCorrecter {

    val logger = LoggerFactory.getLogger(ClusterCorrecter::class.java)

    fun correctClusterSizes(clusterPath: List<Cluster>, maxClusterSize: Int): List<Cluster> {
        clusterPath.forEachIndexed { index, cluster ->
            if (cluster.nodes.size > maxClusterSize && index < clusterPath.size - 1) {
                pushNodesToNextCluster(cluster, clusterPath[index + 1], cluster.nodes.size - maxClusterSize)
            }
        }
        for (i in clusterPath.size - 1 downTo 1) {
            if (clusterPath[i].nodes.size > maxClusterSize) {
                pushNodesToNextCluster(clusterPath[i], clusterPath[i - 1], clusterPath[i].nodes.size - maxClusterSize)
            }
        }
        logger.info("Corrected cluster sizes to ${clusterPath.map { it.nodes.size }}")

        require(clusterPath.all { it.nodes.size < maxClusterSize + 1 }) { "Cluster sizes could not be corrected with ${clusterPath.map { it.nodes.size }} and ${clusterPath.map { it.budget }}" }
        return clusterPath
    }

    fun mergeSmallClusters(clusterPath: List<Cluster>, maxClusterSize: Int): List<Cluster> {

        val nextClusters = mutableListOf<Cluster>()
        clusterPath.forEachIndexed { index, cluster ->
            if (index < clusterPath.size - 1 && cluster.nodes.size + clusterPath[index + 1].nodes.size <= maxClusterSize) {
                cluster.nodes.addAll(clusterPath[index + 1].nodes)
                cluster.endNodes.clear()
                cluster.endNodes.addAll(clusterPath[index + 1].endNodes)
                clusterPath[index + 1].nodes.clear()
                nextClusters.add(cluster)
            } else if (cluster.nodes.isNotEmpty()) {
                nextClusters.add(cluster)
            }
        }
        require(clusterPath.all { it.nodes.size <= maxClusterSize + 1}) { "maxClusterSize = ${maxClusterSize + 1}: Cluster sizes merged with ${clusterPath.map { it.nodes.size }} and ${clusterPath.map { it.budget }}" }
        return nextClusters.toList()
    }

    private fun pushNodesToNextCluster(currentCluster: Cluster, nextCluster: Cluster, amountToPush: Int) {
        if (currentCluster.isStart) {
            currentCluster.nodes.filter { !currentCluster.startNodes.contains(it) }
        } else {
            currentCluster.nodes
        }.sortedBy { it.distanceTo(nextCluster) }.take(amountToPush).forEach {
            nextCluster.nodes.add(it)
            currentCluster.nodes.remove(it)
            it.cluster = nextCluster.id
        }
    }

}