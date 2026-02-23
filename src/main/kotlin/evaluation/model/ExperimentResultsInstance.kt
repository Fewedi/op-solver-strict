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
    val eliminationRevenueWeight: BigDecimal,
    val eliminationSparsityWeight: BigDecimal,
    val budgetDist: String,
    val budgetWeight: BigDecimal,
    val maxBudgetFactor: BigDecimal,
    val avgClusterSize: BigDecimal,
    val avgNodesInDeadCluster: BigDecimal,
    val avgRevenueInDeadCluster: BigDecimal,
    val avgClusters: BigDecimal,
    val avgClustersInPath: BigDecimal,
    val avgClustersIncludedCompletely: BigDecimal,
    val avgPercentageBudgetUnused: BigDecimal,
    val avgClustersExcluded: BigDecimal,
    val avgPercentageClusterExcluded: BigDecimal,
    val avgPercentageClusterRevenueExcluded: BigDecimal,
    val avgBudgetDistDefaulted: BigDecimal,
    val avgBudgetSpilloverBudgetFirstIterationInstance: BigDecimal,
    val avgBudgetSpilloversFirstIterationInstance: BigDecimal,
    val avgBudgetSpilloversInstance: BigDecimal,
    val avgBudgetSpilloverIterationsInstance: BigDecimal,
){
    override fun toString(): String {
        return "E = ($set,$mode,$runs,$budgetFactor,$k,$clustering,$elimination,$r,$budgetDist)"
    }
}