package masterthesis.solver

import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node

class StartNodeProvider {


    fun findStartNode(currentCluster: Cluster): Node {
        return if (currentCluster.prevCluster == null) {
            currentCluster.nodes.first()
        } else {
            currentCluster.nodes.minByOrNull { node ->
                node.distanceTo(currentCluster.prevCluster!!)
            } ?: throw IllegalStateException("No node found")
        }
    }

    fun findEndNode(currentCluster: Cluster): Node {
        return if (currentCluster.nextCluster == null) {
            Node(-1, -1.0, -1.0)
        } else {
            currentCluster.nextCluster!!.startNodes.first()
        }
    }
}