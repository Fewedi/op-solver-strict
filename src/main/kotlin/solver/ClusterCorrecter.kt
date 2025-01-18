package masterthesis.solver

import masterthesis.solver.model.Cluster

class ClusterCorrecter {


    fun correctClusterSizes(clusterPath: List<Cluster>, maxClusterSize: Int) {
        clusterPath.forEachIndexed { index, cluster ->
            if (cluster.nodes.size > maxClusterSize) {
                pushNodesToNextCluster(cluster, clusterPath[index + 1], cluster.nodes.size - maxClusterSize)
            }
        }
        require(clusterPath.any { it.nodes.size > maxClusterSize }) { "Cluster sizes could not be corrected" }
    }

    private fun pushNodesToNextCluster(currentCluster: Cluster, nextCluster: Cluster, amountToPush: Int) {
        currentCluster.nodes.sortedBy { it.distanceTo(nextCluster) }.take(amountToPush).forEach {
            nextCluster.nodes.add(it)
            currentCluster.nodes.remove(it)
            it.cluster = nextCluster.id
        }
    }

}