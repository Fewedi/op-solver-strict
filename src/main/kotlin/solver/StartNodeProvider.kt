package masterthesis.solver

import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node

class StartNodeProvider {


    fun findStartNode(currentCluster: Cluster): Node {
        if (currentCluster.prevCluster == null) {
            return currentCluster.nodes.first()
        }
        return currentCluster.nodes.minByOrNull { node ->
            node.distanceTo(currentCluster.prevCluster!!)
        } ?: throw IllegalStateException("No node found")
    }

    fun findEndNode(currentCluster: Cluster): Node {

        if (currentCluster.nextCluster == null) {
            return Node(-1, -1.0, -1.0)
        } else {
            return currentCluster.nextCluster!!.startNodes.first()
        }
    }
}