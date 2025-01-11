package masterthesis.solver.model

data class Result(
    val name: String,
    val size: Int,
    val finalPath: List<Node>,
    val budget: Double,
    val successful: Boolean,
    val revenue: Int,
    val budgetSpent: Double,
    val time: Double,
)
