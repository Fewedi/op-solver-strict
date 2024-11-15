package solver

import masterthesis.solver.ClusterNode
import java.io.File

class ProblemWriter {
    fun writeProblem(problem: Problem, path: String) {
        val file = File(path)
        problem.savedPath = path
        file.writeText("NAME: ${problem.name}\n")
        file.appendText("COMMENT: ${problem.comment}\n")
        file.appendText("TYPE: ${problem.type}\n")
        file.appendText("DIMENSION: ${problem.dimension}\n")
        file.appendText("COST_LIMIT: ${problem.costLimit.toInt().times(100).div(problem.clusterLists.size)}\n")
        file.appendText("EDGE_WEIGHT_TYPE: ${problem.edgeWeightType}\n")
        file.appendText("NODE_SCORE_SECTION\n")
        problem.graph.vertexSet().filter { it.revenue > 0.0 }.forEach { node ->
            file.appendText("${node.id} ${node.revenue}\n")
        }

        file.appendText("NODE_COORD_SECTION\n")
        problem.graph.vertexSet().forEach { node ->
            file.appendText("${node.id} ${node.x} ${node.y}\n")
        }

        file.appendText("DEPOT_SECTION\n")
        problem.graph.vertexSet().filter { it.isStartNode() }.forEach { node ->
            file.appendText("${node.id}\n")
        }
        file.appendText("-1\n")

    }

    fun writeCluster(problem: Problem, cluster: ClusterNode, path: String, costLimit: Int){
        if (cluster.subGraph.vertexSet().isEmpty()) {
            println("Cluster ${cluster.cluster} is empty")
            return
        }

        val nodeList = cluster.subGraph.vertexSet().toList().apply {
            forEachIndexed { index, node -> node.clusterId = index + 1 }
        }
        problem.clusterLists[cluster.cluster] = nodeList
        val matrix = Array(cluster.size + 1) { IntArray(cluster.size + 1) }
        for (i in 0..<cluster.size) {
            for (j in 0..<cluster.size) {
                if (i == j) {
                    matrix[i+1][i+1] = Int.MAX_VALUE
                } else {
                    matrix[i+1][j+1] =
                        (nodeList[i].distanceTo(nodeList[j]) * 100).toInt()
                }
            }
        }
        matrix[0][0] = Int.MAX_VALUE


        cluster.subGraph.edgeSet().forEach { edge ->
            val source = cluster.subGraph.getEdgeSource(edge)
            val target = cluster.subGraph.getEdgeTarget(edge)
            //matrix[source.id][target.id] = source.distanceTo(target)
            //matrix[target.id][source.id] = source.distanceTo(target)
        }


        val file = File(path)
        file.writeText("NAME: ${problem.name}-${cluster.cluster}\n")
        file.appendText("COMMENT: ${problem.comment} \n")
        file.appendText("TYPE: OP\n")
        file.appendText("DIMENSION: ${cluster.size + 1}\n")
        file.appendText("COST_LIMIT: 3000\n")
        //file.appendText("COST_LIMIT: ${problem.costLimit.toInt().times(100).div(problem.clusterLists.size)}\n")
        file.appendText("EDGE_WEIGHT_TYPE: EXPLICIT\n")
        file.appendText("EDGE_WEIGHT_FORMAT: FULL_MATRIX\n")
        file.appendText("EDGE_WEIGHT_SECTION\n")
        matrix.forEach { row ->
            row.forEach { value ->
                file.appendText("$value ")
            }
            file.appendText("\n")
        }
        file.appendText("NODE_SCORE_SECTION\n")
        file.appendText("0 0\n")
        nodeList.forEach { node ->
            file.appendText("${node.clusterId} ${node.revenue}\n")
        }
        file.appendText("DEPOT_SECTION\n")
        file.appendText("1\n")
        file.appendText("-1\n")
        file.appendText("EOF\n")


    }
}