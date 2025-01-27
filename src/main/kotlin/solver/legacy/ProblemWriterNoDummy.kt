package masterthesis.solver.legacy

import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import masterthesis.solver.model.ProblemMetaData
import java.io.File

class ProblemWriterNoDummy : ProblemWriter {

    override fun writeCluster(
        metadata: ProblemMetaData,
        distanceMatrix: Array<DoubleArray>,
        cluster: Cluster,
        path: String,
        costLimit: Double
    ) {
        val nodeList: MutableList<Node> = mutableListOf()
        val clusterDistanceMatrix = createDistanceMatrix(cluster, distanceMatrix, nodeList)

        clusterDistanceMatrix[0][0] = 100000.0

        File(path).apply {
            writeText("NAME: ${metadata.name}-${cluster.id}\n")
            appendText("COMMENT: ${metadata.comment} \n")
            appendText("TYPE: OP\n")
            appendText("DIMENSION: ${cluster.size}\n")
            appendText("COST_LIMIT: ${costLimit * 5}\n")
            appendText("EDGE_WEIGHT_TYPE: EXPLICIT\n")
            appendText("EDGE_WEIGHT_FORMAT: FULL_MATRIX\n")
            appendText("EDGE_WEIGHT_SECTION\n")
            clusterDistanceMatrix.forEach { row ->
                row.forEach { value ->
                    appendText("${(value * 100).toInt()} ")
                }
                appendText("\n")
            }
            appendText("NODE_SCORE_SECTION\n")
            nodeList.forEach { node ->
                appendText("${node.writtenPosition} ${node.revenue}\n")
                cluster.solutionMap[node.writtenPosition] = node
            }
            appendText("DEPOT_SECTION\n")
            appendText("1\n")
            appendText("2\n")
            appendText("EOF\n")
        }
    }

    private fun createDistanceMatrix(cluster: Cluster, distanceMatrix: Array<DoubleArray>, nodeList: MutableList<Node>): Array<DoubleArray> {
        val clusterDistanceMatrix = Array(cluster.size) { DoubleArray(cluster.size) }
        cluster.nodes.forEachIndexed { index, myNode ->
            cluster.nodes.forEachIndexed { index2, myNode2 ->
                clusterDistanceMatrix[index][index2] = distanceMatrix[myNode.id][myNode2.id]
            }
            myNode.writtenPosition = index
            nodeList.add(myNode)
        }
        clusterDistanceMatrix[0][0] = 100000.0
        return clusterDistanceMatrix
    }
}
