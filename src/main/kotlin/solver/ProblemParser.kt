package solver

import solver.Problem
import org.jgrapht.Graph
import org.jgrapht.generate.CompleteGraphGenerator
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.GraphWalk
import java.io.File

class ProblemParser {

    fun readProblem(): Problem {
        val path = "src/main/resources/op-solver/build/OPLib/instances/gen1/eil101-gen1-50.oplib"
        val file = File(path).readLines()
        val problem = Problem(path).apply {
            nodeMap = mutableMapOf()
        }
        val graph: Graph<Node, DefaultWeightedEdge> = DefaultUndirectedWeightedGraph(DefaultWeightedEdge::class.java)
        var mode = ReadingMode.META

        file.forEach { line ->
            if (line.trim() == "NODE_COORD_SECTION")
                mode = ReadingMode.NODES
            else if (line.trim() == "DEPOT_SECTION")
                mode = ReadingMode.DEPOT
            else if (line.trim() == "NODE_SCORE_SECTION")
                mode = ReadingMode.SCORES
            else if (mode == ReadingMode.META) {
                val entry = line.split(":").apply {
                    line.trim()
                }
                if (entry.size == 2) {
                    when (entry.first().uppercase().trim()) {
                        "NAME" -> problem.name = entry[1]
                        "COMMENT" -> problem.comment = entry[1]
                        "TYPE" -> problem.type = entry[1]
                        "DIMENSION" -> problem.dimension = entry[1]
                        "COST_LIMIT" -> problem.costLimit = entry[1].trim()
                        "EDGE_WEIGHT_TYPE" -> problem.edgeWeightType = entry[1]
                        else -> throw IllegalArgumentException("Unknown entry: ${entry.first()} in file $path")
                    }
                } else {
                    throw IllegalArgumentException("Unknown entry: $line in file $path")
                }
            } else if (mode == ReadingMode.NODES) {
                val entry = line.split(" ")
                if (entry.size == 3) {
                    val id = entry[0].toInt()
                    val x = entry[1].toDouble()
                    val y = entry[2].toDouble()
                    val node = Node(id, x, y)
                    graph.addVertex(node)
                    problem.nodeMap[id] = node
                } else {
                    throw IllegalArgumentException("Unknown entry: $line in file $path")
                }
//            } else if (mode == ReadingMode.SCORES) {
//                val entry = line.split(" ")
//                if (entry.size == 2) {
//                    val id = entry[0].toInt()
//                    val score = entry[1].toDouble()
//                    val node = graph.vertexSet().find { it.id == id }!!
//                    node.revenue = score
//                } else {
//                    throw IllegalArgumentException("Unknown entry: $line in file $path")
//                }
            }
        }
        val graphGenerator = CompleteGraphGenerator<Node, DefaultWeightedEdge>()
        graphGenerator.generateGraph(graph)
        graph.edgeSet().forEach { edge ->
            graph.setEdgeWeight(edge, graph.getEdgeSource(edge).distanceTo(graph.getEdgeTarget(edge)))
        }
        problem.graph = graph
        return problem
    }

    fun addSolutionToProblem(problem: Problem, solution: Solution, cluster: Int? = null) {

        if (cluster == null) {
            solution.sol.cycle.map {
                problem.nodeMap[it]
            }.let {
                problem.walk = GraphWalk(problem.graph, it, 0.0)
            }
        } else {
            try {
                if(solution.sol.cycle.first() == 1) {solution.sol.cycle.removeFirst()}
                solution.sol.cycle.map {
                    problem.clusterLists[cluster]?.get(it-2) ?: throw IllegalArgumentException("Node $it not found in cluster $cluster")
                }.let {
                    problem.clusterWalks[cluster] = GraphWalk(problem.graph, it, 0.0)
                }
            }catch (e: Exception){
                println("Error: $e")
                println("Stacktrace: ${e.stackTrace}")


            }
        }
    }

    enum class ReadingMode{
        META,
        NODES,
        SCORES,
        DEPOT
    }
}