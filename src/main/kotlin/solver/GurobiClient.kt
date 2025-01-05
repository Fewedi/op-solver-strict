package masterthesis.solver

import com.fasterxml.jackson.databind.ObjectMapper
import com.gurobi.gurobi.*
import masterthesis.solver.model.GurobiSolution
import masterthesis.solver.model.Node

class GurobiClient {

    private val logger = org.slf4j.LoggerFactory.getLogger(GurobiClient::class.java)

    private fun getTaskToAdd(
        solution: List<Int>,
        costM: Array<DoubleArray>,
        nodes: List<Node>,
        endNodeIndex: Int,
        newestNodeIndex: Int,
        meanRevenue: Double
    ): Int? {
        val scores = MutableList(nodes.size) { 0.0 }
        for (i in nodes.indices) {
            // Check if task is already in the solution
            scores[i] = if (i in solution || i == endNodeIndex) {
                Double.NEGATIVE_INFINITY
            } else {
                costM[newestNodeIndex][i] * nodes[i].revenue - costM[i][endNodeIndex] * meanRevenue
            }
        }
        return scores.withIndex()
            .maxByOrNull { it.value } // Find the entry with the highest value
            ?.index
    }

    private fun getInitialSolution(
        nodes: List<Node>,
        timeBudget: Double,
        costM: Array<DoubleArray>,
        startNodeIndex: Int,
        finalNodeIndex: Int
    ): List<Int> {

        val solution: MutableList<Int> = mutableListOf() // Start with task 0
        var t = 0.0

        val meanRevenue = nodes.map { it.revenue }.average()
        var toAdd = startNodeIndex
        while ((
                    solution.size <= 1 ||
                            t + costM[solution.last()][toAdd] + costM[toAdd][finalNodeIndex] < timeBudget
                    ) && solution.size < nodes.size + 1
        ) {
            if(solution.isNotEmpty()) t += costM[solution.last()][toAdd]
            solution.add(toAdd)

            toAdd = getTaskToAdd(solution, costM, nodes, finalNodeIndex, toAdd, meanRevenue)
                ?: break
        }
        solution.add(finalNodeIndex)
        return solution
    }

    private fun setupModel(
        nodes: List<Node>,
        costMatrix: Array<DoubleArray>,
        budget: Double,
        model: GRBModel,
        initialSolution: List<Int>
    ) {
        val startNodeIndex = 0
        val endNodeIndex = nodes.size - 1
        val x = Array(nodes.size) { i ->
            Array(nodes.size) { j ->
                model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x[$i][$j]")
            }
        }
        val u = Array(nodes.size) { i ->
            model.addVar(1.0, (nodes.size).toDouble(), 0.0, GRB.INTEGER, "u[$i]")
        }

        for (i in 0 until initialSolution.size - 1) {
            x[initialSolution[i]][initialSolution[i + 1]][GRB.DoubleAttr.Start] = 1.0
        }

        // 0
        // The reward of each node in the tour should be maximized
        val obj = GRBLinExpr().apply {
            for (i in 0 until nodes.size) {
                for (j in 0 until nodes.size) {
                    addTerm(nodes[j].revenue.toDouble(), x[i][j])
                }
            }
        }
        model.setObjective(obj, GRB.MAXIMIZE)

        // 1
        // one outgoing edge from the starting node
        val startConstraintExpression = GRBLinExpr().apply {
            for (j in 1 until nodes.size) {
                addTerm(1.0, x[startNodeIndex][j])
            }
        }
        model.addConstr(startConstraintExpression, GRB.EQUAL, 1.0, "startConstraint")

        // no incoming edge to the starting node
        val startConstraintIncomingExpression = GRBLinExpr().apply {
            for (i in 0 until nodes.size) {
                addTerm(1.0, x[i][startNodeIndex])
            }
        }
        model.addConstr(startConstraintIncomingExpression, GRB.EQUAL, 0.0, "startConstraintIncoming")

        // one incoming edge from the end node
        val endConstraintExpression = GRBLinExpr().apply {
            for (i in 0 until nodes.size - 1) {
                addTerm(1.0, x[i][endNodeIndex])
            }
        }
        model.addConstr(endConstraintExpression, GRB.EQUAL, 1.0, "endConstraint")

        // no outgoing edge from the end node
        val endConstraintOutgoingExpression = GRBLinExpr().apply {
            for (j in 0 until nodes.size) {
                addTerm(1.0, x[endNodeIndex][j])
            }
        }
        model.addConstr(endConstraintOutgoingExpression, GRB.EQUAL, 0.0, "endConstraintOutgoing")

        // 2
        // each node should have exactly one incoming and one outgoing edge or none
        for (k in 1 until nodes.size - 1) {
            if (k == startNodeIndex || k == endNodeIndex) continue
            val maxOutgoingEdgesExpression = GRBLinExpr().apply {
                for (j in 1 until nodes.size) {
                    addTerm(1.0, x[k][j])
                }
            }
            model.addConstr(maxOutgoingEdgesExpression, GRB.LESS_EQUAL, 1.0, "maxOutgoingEdgesConstraint[$k]")
            val maxIncomingEdgesExpression = GRBLinExpr().apply {
                for (i in 0 until nodes.size - 1) {
                    addTerm(1.0, x[i][k])
                }
            }
            model.addConstr(maxIncomingEdgesExpression, GRB.LESS_EQUAL, 1.0, "maxIncomingEdgesConstraint[$k]")
            model.addConstr(
                maxOutgoingEdgesExpression,
                GRB.EQUAL,
                maxIncomingEdgesExpression,
                "inAndOutEdgesConstraint[$k]"
            )
        }

        // 3
        // The total time of the tour should not exceed the budget
        val tourLengthExpression = GRBLinExpr().apply {
            for (i in 0 until nodes.size) {
                for (j in 1 until nodes.size) {
                    addTerm(costMatrix[i][j], x[i][j])
                }
            }
        }
        model.addConstr(tourLengthExpression, GRB.LESS_EQUAL, budget, "tourLengthConstraint")

        // 4 done implicit in definition of u

        // 5
        // Subtour elimination by implementing the Miller-Tucker-Zemlin constraints (read the paper)
        // u_i - u_j + 1 <= (n-1) * (1 - x_ij) for all i,j in 2 .. n
        // = N - 1 + (-N + 1) * x_ij           - implemented
        for (i in 1 until nodes.size) {
            for (j in 1 until nodes.size) {
                val leftSideExpression = GRBLinExpr().apply {
                    addTerm(1.0, u[i])
                    addTerm(-1.0, u[j])
                    addConstant(1.0)
                }
                val rightSideException = GRBLinExpr().apply {
                    addTerm(-nodes.size + 1.0, x[i][j])
                    addConstant(nodes.size - 1.0)
                }
                model.addConstr(
                    leftSideExpression,
                    GRB.LESS_EQUAL,
                    rightSideException,
                    "subtourEliminationConstraint[$i][$j]"
                )
            }
        }

        // 6 done implicit in definition of x
    }

    fun solve(objectMapper: ObjectMapper, nodes: List<Node>, budget: Int): GurobiSolution {
        val isLastCluster = nodes.last().id == -1
        val costMatrix = Array(nodes.size) { i ->
            DoubleArray(nodes.size) { j ->
                if ((i == nodes.size - 1 || j == nodes.size - 1) && isLastCluster) {
                    0.0
                } else {
                    nodes[i].distanceTo(nodes[j])
                }
            }
        }

        val env = GRBEnv()
        env.start()
        val model = GRBModel(env)
        model[GRB.IntParam.OutputFlag] = 0
        model[GRB.DoubleParam.MIPGap] = 0.05

        val initialSolution = getInitialSolution(nodes, budget.toDouble(), costMatrix, 0, nodes.size - 1)
        setupModel(nodes, costMatrix, budget.toDouble(), model, initialSolution)

        model.optimize()
        val gurobiSolution = model.jsonSolution.let {
            logger.info("Gurobi solution: $it")
            objectMapper.readValue(it, GurobiSolution::class.java)
        }
        return gurobiSolution
    }
}