package masterthesis.evaluation

data class ResultOutput(
    val name: String,
    val size: Int,
    val budget: Int,
    val successful: Boolean,
    val revenue: Int,
    val budgetSpent: Double,
    val time: Double,
)
