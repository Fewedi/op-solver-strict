package masterthesis.evaluation.model

import masterthesis.DataCapturing
import java.math.BigDecimal
import java.math.RoundingMode


data class ExperimentSpecification(
    val set: String,
    val mode: String,
    val runs: Int,
    val budgetFactor: BigDecimal,
    val k: Int,
    val clustering: String,
    val elimination: String,
    val r: BigDecimal,
    val budgetDist: String
){
    override fun toString(): String {
        return "E = ($set,$mode,$runs,$budgetFactor,$k,$clustering,$elimination,$r,$budgetDist)"
    }
    fun toFileNameString(): String {
        return "results_${set}_${mode}_${runs}_${budgetFactor.movePointRight(2).toInt()}_${k}_${clustering}_${elimination}_${r.movePointRight(2).toInt()}_${budgetDist}.csv"
    }

    fun getExperimentResultsInstance(): ExperimentResultsInstance {
        return ExperimentResultsInstance(
            set = set,
            mode = mode,
            runs = runs,
            budgetFactor = budgetFactor,
            k = k,
            clustering = clustering,
            elimination = elimination,
            r = r,
            budgetDist = budgetDist,
            avgClusterSize = DataCapturing.clusterSizesInstance.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgNodesInDeadCluster = DataCapturing.nodesInDeadClusterInstance.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgRevenueInDeadCluster = DataCapturing.revenueInDeadClusterInstance.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgClusters = DataCapturing.clustersInstance.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgClustersInPath = DataCapturing.clustersInPathInstance.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgClustersIncludedCompletely = DataCapturing.clustersIncludedCompletelyInstance.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgPercentageBudgetUnused = DataCapturing.percentageBudgetUnusedInstance.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        )
    }

    fun getExperimentResultsGlobal(): ExperimentResultsGlobal {
        return ExperimentResultsGlobal(
            set = set,
            mode = mode,
            runs = runs,
            budgetFactor = budgetFactor,
            k = k,
            clustering = clustering,
            elimination = elimination,
            r = r,
            budgetDist = budgetDist,
            avgClusterSize = DataCapturing.clusterSizes.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgNodesInDeadCluster = DataCapturing.nodesInDeadCluster.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgRevenueInDeadCluster = DataCapturing.revenueInDeadCluster.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgClusterPercentageIncluded = DataCapturing.clusterPercentageIncluded.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgClustersIncludedCompletely = DataCapturing.clustersIncludedCompletely.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            avgPercentageBudgetUnused = DataCapturing.percentageBudgetUnused.average().toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        )
    }
}
