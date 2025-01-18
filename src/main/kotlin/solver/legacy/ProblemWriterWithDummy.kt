package masterthesis.solver.legacy

import masterthesis.solver.config.ConfigProvider
import masterthesis.solver.config.DummyStartNode
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import masterthesis.solver.model.ProblemMetaData
import java.io.File
import org.slf4j.LoggerFactory

class ProblemWriterWithDummy: ProblemWriter {

    private val logger = LoggerFactory.getLogger(ProblemWriterWithDummy::class.java)

    override fun writeCluster(
        metadata: ProblemMetaData,
        distanceMatrix: Array<DoubleArray>,
        cluster: Cluster,
        path: String,
        costLimit: Int
    ) {
        val nodeList: MutableList<Node> = mutableListOf()
        val clusterDistanceMatrix = createDistanceMatrix(cluster, distanceMatrix, nodeList)

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

    private fun createDistanceMatrix(cluster: Cluster, distanceMatrix: Array<DoubleArray>, nodeList: MutableList<Node>): Array<DoubleArray> {
        val clusterDistanceMatrix = Array(cluster.size + 1) { DoubleArray(cluster.size + 1) }
        cluster.nodes.forEachIndexed { index, myNode ->
            cluster.nodes.forEachIndexed { index2, myNode2 ->
                clusterDistanceMatrix[index + 1][index2 + 1] = distanceMatrix[myNode.id][myNode2.id]
            }
            myNode.writtenPosition = index + 1
            nodeList.add(myNode)
        }
        clusterDistanceMatrix[0][0] = 100000.0

        if (ConfigProvider.config.ea4op!!.dummyStartNode == DummyStartNode.CLUSTER){
            nodeList.forEach(){ node ->
                clusterDistanceMatrix[0][node.writtenPosition] = node.distanceToNextCluster * ConfigProvider.config.ea4op!!.dummyStartNodeFactor
                clusterDistanceMatrix[node.writtenPosition][0] = node.distanceToPrevCluster * ConfigProvider.config.ea4op!!.dummyStartNodeFactor
            }
        }
        return clusterDistanceMatrix
    }
}