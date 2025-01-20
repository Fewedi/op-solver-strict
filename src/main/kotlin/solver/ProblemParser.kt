package solver

import masterthesis.solver.config.ConfigProvider
import masterthesis.solver.config.RevenueDistributionType
import masterthesis.solver.legacy.GkobeagaSolution
import masterthesis.solver.model.GurobiSolution
import masterthesis.solver.model.Node
import masterthesis.solver.model.ProblemMetaData
import masterthesis.solver.model.ProblemSpace
import org.slf4j.LoggerFactory
import java.io.File


class ProblemParser {


    private val logger = LoggerFactory.getLogger(ProblemParser::class.java)

    fun readProblemSpace(folderName: String, gen: String): ProblemSpace {
        val path = "src/main/resources/op-solver/build/OPLib/instances/$gen/$folderName.oplib"
        val file = File(path).readLines()
        val problem = ProblemMetaData(path)

        val nodeMap = HashMap<Int, Node>()

        val getRevenue = when (ConfigProvider.config.revenueDistribution) {
            RevenueDistributionType.RANDOM -> ::getRevenueRandom
            RevenueDistributionType.FLAT-> ::getRevenueFlat
        }

        var mode = ReadingMode.META

        var readStartNode = false
        file.forEach { line ->
            when (line.trim()) {
                "NODE_COORD_SECTION" -> mode = ReadingMode.NODES
                "DEPOT_SECTION" -> mode = ReadingMode.DEPOT
                "NODE_SCORE_SECTION" -> {
                    mode = ReadingMode.SCORES
                    logger.warn("Scores are not read")
                }

                else -> when (mode) {
                    ReadingMode.META -> {
                        val entry = line.split(":").apply {
                            line.trim()
                        }
                        if (entry.size == 2) {
                            mapMetaDataToObject(entry, problem)
                        } else {
                            logger.error("Unknown meta entry: $line in file $path")
                        }
                    }

                    ReadingMode.NODES -> {
                        val entry = line.split(" ")
                        if (entry.size == 3) {
                            val id = entry[0].toInt() - 1
                            val node = Node(
                                id = id,
                                x = entry[1].toDouble(),
                                y = entry[2].toDouble(),
                                revenue = getRevenue(),
                                startNode = id == 0)
                            nodeMap[id] = node
                        } else {
                            logger.error("Unknown entry: $line in file $path")
                        }
                    }

                    ReadingMode.SCORES -> Unit
                    ReadingMode.DEPOT -> {
                        if (!readStartNode) {
                            val entry = line.split(" ")
                            if (entry.size == 1) {
                                val id = entry[0].toInt() - 1
                                nodeMap[id]?.let {
                                    it.startNode = true
                                    readStartNode = true
                                    problem.startNode = entry[0]
                                }
                            }
                        }
                    }

                }
            }
        }
        val distanceMatrix = createDistanceMatrix(nodeMap)

        return ProblemSpace(problem, nodeMap, distanceMatrix)
    }

    private fun getRevenueRandom(): Int {
        return (1..100).random()
    }
    private fun getRevenueFlat(): Int {
        return 1
    }

    private fun createDistanceMatrix(nodeMap: Map<Int, Node>): Array<DoubleArray> {
        val distanceMatrix = Array(nodeMap.size) { DoubleArray(nodeMap.size) }

        for (xNode in nodeMap.values) {
            for (yNode in nodeMap.values) {
                if (xNode.id == yNode.id) {
                    distanceMatrix[xNode.id][yNode.id] = 1000000.0
                    xNode.distanceMap[yNode.id] = 1000000.0
                    continue
                }
                xNode.distanceTo(yNode)
                    .let {
                        distanceMatrix[xNode.id][yNode.id] = it
                        xNode.distanceMap[yNode.id] = it
                    }
            }
        }
        return distanceMatrix
    }

    private fun mapMetaDataToObject(entry: List<String>, metaData: ProblemMetaData) {
        when (entry.first().uppercase().trim()) {
            "NAME" -> metaData.name = entry[1]
            "COMMENT" -> metaData.comment = entry[1]
            "TYPE" -> metaData.type = entry[1]
            "DIMENSION" -> metaData.dimension = entry[1]
            "COST_LIMIT" -> metaData.costLimit = entry[1].trim()
            "EDGE_WEIGHT_TYPE" -> metaData.edgeWeightType = entry[1]
            else -> logger.error("Unknown node entry: ${entry.first()} in file")
        }
    }

    fun addSolutionToProblem(nodeMap: Map<Int, Node>, solution: GkobeagaSolution): List<Node> {
        if (solution.sol.cycle.first() == 1) {
            solution.sol.cycle.removeFirst()
        }
        solution.sol.cycle.map {
            nodeMap[it - 1]
        }.let {
            return it.filterNotNull()
        }
    }

    fun <T> addSolutionToProblem(nodeMap: List<T>, solution: GurobiSolution, startNode: T, finalNode: T): List<T> {
        val regex = Regex("""x\[(\d+)]\[(\d+)]""")
        val nodeSolution = mutableListOf<T>()
        val solutionMap = mutableMapOf<T, T>()
        solution.vars.forEach { entry ->
            regex.matchEntire(entry.varName).let { match ->
                if (match != null) {
                    solutionMap[nodeMap[match.groupValues[1].toInt()]!!] = nodeMap[match.groupValues[2].toInt()]!!
                }
            }
        }
        var currentNode = startNode
        while (nodeSolution.size < nodeMap.size && (currentNode != finalNode || nodeSolution.isEmpty())) {
            nodeSolution.add(currentNode)
            currentNode = solutionMap[currentNode]!!
        }
        return nodeSolution
    }

    enum class ReadingMode {
        META,
        NODES,
        SCORES,
        DEPOT
    }
}