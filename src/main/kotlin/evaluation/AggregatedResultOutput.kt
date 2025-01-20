package masterthesis.evaluation

data class AggregatedResultOutput(
    val name: String,
    val size: Int,
    val budget: Int,
    val successfulAmount: Int,
    val revenueMin: Int,
    val revenueAvg: Int,
    val revenueMax: Int,
    val budgetSpentAvg: Double,
    val timeMin: Double,
    val timeAvg: Double,
    val timeMax: Double,
)
