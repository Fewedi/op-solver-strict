package masterthesis.evaluation.model

import java.math.BigDecimal

data class ExperimentResultsInstance(
    val set: String,
    val mode: String,
    val runs: Int,
    val budgetFactor: BigDecimal,
    val k: Int,
    val clustering: String,
    val elimination: String,
    val r: BigDecimal,
    val budgetDist: String,
    val avgClusterSize: BigDecimal,
    val avgNodesInDeadCluster: BigDecimal,
    val avgRevenueInDeadCluster: BigDecimal,
    val avgClusters: BigDecimal,
    val avgClustersInPath: BigDecimal,
    val avgClustersIncludedCompletely: BigDecimal,
    val avgPercentageBudgetUnused: BigDecimal,
){
    override fun toString(): String {
        return "E = ($set,$mode,$runs,$budgetFactor,$k,$clustering,$elimination,$r,$budgetDist)"
    }
}