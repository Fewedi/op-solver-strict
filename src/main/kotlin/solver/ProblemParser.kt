package solver

import masterthesis.config.ConfigProvider
import masterthesis.config.Origin
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
        val path = when (ConfigProvider.config.instance.origin) {
            Origin.OPLIB -> "src/main/resources/op-solver/build/OPLib/instances/$gen/$folderName.oplib"
            Origin.REF -> {
                if (gen == "gen1") {
                    "src/main/resources/OP_instances_elzein/OP_FlatUtilities/$folderName"
                } else if (gen == "gen2") {
                    "src/main/resources/OP_instances_elzein/OP_RandomUtilities/$folderName"
                } else {
                    logger.error("Unknown generation: $gen")
                    throw IllegalArgumentException("Unknown generation: $gen")
                }
            }
        }

        val file = File(path).readLines()
        val problem = ProblemMetaData(path)

        val nodeMap = HashMap<Int, Node>()

        var mode = ReadingMode.META

        var readStartNode = false
        file.forEach { line ->
            when (line.trim()) {
                "NODE_COORD_SECTION" -> mode = ReadingMode.NODES
                "NODE_COORDINATES" -> mode = ReadingMode.NODES //ref
                "DEPOT_SECTION" -> mode = ReadingMode.DEPOT
                "NODE_SCORE_SECTION" -> {
                    mode = ReadingMode.SCORES
                    logger.warn("Scores are not read")
                }
                "NODE_SCORES" -> {
                    mode = ReadingMode.SCORES //ref
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
                                startNode = id == 0)
                            nodeMap[id] = node
                        } else {
                            logger.error("Unknown entry: $line in file $path")
                        }
                    }

                    ReadingMode.SCORES -> {
                        val entry = line.split(" ")
                        if (entry.size == 2) {
                            val id = entry[0].toInt() - 1
                            nodeMap[id]?.let {
                                it.revenue = entry[1].toInt()
                            } ?: run {logger.error("Node with id $id not found in map while reading scores") }
                        }
                    }
                    ReadingMode.DEPOT -> {
                        if (!readStartNode) {
                            val entry = line.split(" ")
                            if (entry.size == 1) {
                                val id = entry[0].toInt() - 1
                                nodeMap[id]?.let {
                                    it.startNode = true
                                    readStartNode = true
                                    problem.startNode = nodeMap[entry[0].toInt()] ?: run {
                                        logger.error("Start node with id ${entry[0]} not found in map while reading depot")
                                        throw IllegalArgumentException("Start node with id ${entry[0]} not found in map while reading depot")
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
        if (!readStartNode) {
            nodeMap[0]?.let {
                it.startNode = true
                problem.startNode = it
            } ?: run {
                logger.error("No start node found in problem space.")
                throw IllegalArgumentException("No start node found in problem space.")
            }
        }
        val distanceMatrix = createDistanceMatrix(nodeMap)
        if (nodeMap.values.any { it.revenue == null || it.revenue!! < 0}) {
            logger.error("Some nodes have no revenue or negative revenue. This is not allowed.")
            throw IllegalArgumentException("Some nodes have no revenue or negative revenue. This is not allowed.")
        }
        return ProblemSpace(problem, nodeMap, distanceMatrix)
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
            "COST_LIMIT" -> metaData.costLimit = entry[1].trim().toInt().toDouble()
            "BUDGET" -> metaData.costLimit = entry[1].trim().toDouble()
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