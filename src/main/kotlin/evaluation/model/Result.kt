package masterthesis.evaluation.model

import masterthesis.solver.model.Node

data class Result(
    val name: String,
    val size: Int,
    val finalPath: List<Node>,
    val budget: Int,
    val successful: Boolean,
    val revenue: Int,
    val budgetSpent: Double,
    val time: Double,
)