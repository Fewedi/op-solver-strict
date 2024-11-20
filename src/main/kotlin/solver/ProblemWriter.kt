package solver

import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import masterthesis.solver.model.ProblemMetaData
import java.io.File
import org.slf4j.LoggerFactory

class ProblemWriter {

    private val logger = LoggerFactory.getLogger(ProblemWriter::class.java)

    fun writeCluster(
        metadata: ProblemMetaData,
        distanceMatrix: Array<DoubleArray>,
        cluster: Cluster,
        path: String,
        costLimit: Int
    ) {

        val clusterDistanceMatrix = Array(cluster.size + 1) { DoubleArray(cluster.size + 1) }
        val nodeList: MutableList<Node> = mutableListOf()
        cluster.nodes.forEachIndexed { index, myNode ->
            cluster.nodes.forEachIndexed { index2, myNode2 ->
                clusterDistanceMatrix[index + 1][index2 + 1] = distanceMatrix[myNode.id][myNode2.id]
            }
            myNode.writtenPosition = index + 1
            nodeList.add(myNode)
        }
        clusterDistanceMatrix[0][0] = 100000.0

        File(path).apply {
            writeText("NAME: ${metadata.name}-${cluster.id}\n")
            appendText("COMMENT: ${metadata.comment} \n")
            appendText("TYPE: OP\n")
            appendText("DIMENSION: ${cluster.size + 1}\n")
            appendText("COST_LIMIT: ${costLimit * 3}\n")
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
            appendText("0 0\n")
            nodeList.forEach { node ->
                appendText("${node.writtenPosition} ${node.revenue}\n")
                cluster.solutionMap[node.writtenPosition] = node
            }
            appendText("DEPOT_SECTION\n")
            appendText("1\n")
            appendText("-1\n")
            appendText("EOF\n")
        }
    }

}