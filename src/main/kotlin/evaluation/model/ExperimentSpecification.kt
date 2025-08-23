package masterthesis.evaluation.model

import masterthesis.DataCapturing
import masterthesis.config.ClusterConnector
import masterthesis.config.ClusterEliminationMethod
import masterthesis.config.ConfigProvider
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
    val eliminationRevenueWeight: BigDecimal,
    val eliminationSparsityWeight: BigDecimal,
    val r: BigDecimal,
    val budgetDist: String
){
    override fun toString(): String {
        return "E = ($set,$mode,$runs,$budgetFactor,$k,$clustering,$elimination,$r,$budgetDist)"
    }
    fun toFileNameString(paramSearchFor: Parameter = Parameter.NONE): String {
        return when (paramSearchFor) {
            Parameter.CLUSTER_ELIMINATION_FACTOR -> {
                if (ConfigProvider.config.algorithm.clusterConnector == ClusterConnector.OP) {
                    "results_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_{PARAM}_${budgetDist}"
                } else {
                    when (ConfigProvider.config.algorithm.clusterElimination) {
                        ClusterEliminationMethod.LAST -> "results_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_PARAM_${budgetDist}"
                        ClusterEliminationMethod.DISTANCE -> "results_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_PARAM_${eliminationRevenueWeight.int()}_${budgetDist}"
                        ClusterEliminationMethod.SPARSITY -> "results_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_PARAM_${eliminationRevenueWeight.int()}_${eliminationSparsityWeight.int()}_PARAM_${budgetDist}"
                        else -> "DEPR_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_PARAM_${budgetDist}"
                    }
                }
            }
            Parameter.CLUSTER_ELIMINATION_REVENUE_THRESHOLD -> {
                when (ConfigProvider.config.algorithm.clusterElimination) {
                    ClusterEliminationMethod.DISTANCE -> "results_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_${r.int()}_PARAM_${budgetDist}"
                    ClusterEliminationMethod.SPARSITY -> "results_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_${r.int()}_PARAM_${eliminationSparsityWeight.int()}_${budgetDist}"
                    else -> "DEPR_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_${r.int()}_${budgetDist}"
                }
            }
            Parameter.CLUSTER_ELIMINATION_SPARSITY_THRESHOLD -> "results_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_${r.int()}_${eliminationRevenueWeight.int()}_PARAM_${budgetDist}"
            Parameter.NONE -> "results_${set}_${mode}_${runs}_${budgetFactor.int()}_${k}_${clustering}_${elimination}_${r.int()}_${budgetDist}"
        }
    }

    private fun BigDecimal.int() = this.movePointRight(2).toInt()

    enum class Parameter(parameterName: String){
        CLUSTER_ELIMINATION_FACTOR ("clusterEliminationThreshold"),
        CLUSTER_ELIMINATION_REVENUE_THRESHOLD("clusterEliminationRevenueWeight"),
        CLUSTER_ELIMINATION_SPARSITY_THRESHOLD("clusterEliminationSparsityWeight"),
        NONE ("");

        companion object {
            fun fromString(name: String): Parameter {
                return values().firstOrNull { it.name == name } ?: NONE
            }
        }
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
            eliminationRevenueWeight = eliminationRevenueWeight,
            eliminationSparsityWeight = eliminationSparsityWeight,
            budgetDist = budgetDist,
            avgClusterSize = DataCapturing.clusterSizesInstance.averageToPrint(),
            avgNodesInDeadCluster = DataCapturing.nodesInDeadClusterInstance.averageToPrint(),
            avgRevenueInDeadCluster = DataCapturing.revenueInDeadClusterInstance.averageToPrint(),
            avgClusters = DataCapturing.clustersInstance.averageToPrint(),
            avgClustersInPath = DataCapturing.clustersInPathInstance.averageToPrint(),
            avgClustersIncludedCompletely = DataCapturing.clustersIncludedCompletelyInstance.averageToPrint(),
            avgPercentageBudgetUnused = DataCapturing.percentageBudgetUnusedInstance.averageToPrint(),
            avgClustersExcluded = DataCapturing.clustersExcludedInstance.averageToPrint(),
            avgPercentageClusterExcluded = DataCapturing.fractionClustersExcludedInstance.averageToPrint(),
            avgPercentageClusterRevenueExcluded = DataCapturing.fractionClusterRevenueExcludedInstance.averageToPrint()
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
            eliminationRevenueWeight = eliminationRevenueWeight,
            eliminationSparsityWeight = eliminationSparsityWeight,
            budgetDist = budgetDist,
            avgClusterSize = DataCapturing.clusterSizes.averageToPrint(),
            avgNodesInDeadCluster = DataCapturing.nodesInDeadCluster.averageToPrint(),
            avgRevenueInDeadCluster = DataCapturing.revenueInDeadCluster.averageToPrint(),
            avgClusterPercentageIncluded = DataCapturing.clusterPercentageIncluded.averageToPrint(),
            avgClustersIncludedCompletely = DataCapturing.clustersIncludedCompletely.averageToPrint(),
            avgPercentageBudgetUnused = DataCapturing.percentageBudgetUnused.averageToPrint(),
            avgPercentageClusterExcluded = DataCapturing.fractionClustersExcluded.averageToPrint(),
            avgPercentageClusterRevenueExcluded = DataCapturing.fractionClusterRevenueExcluded.averageToPrint()
        )
    }

    private fun List<Number>.averageToPrint(): BigDecimal {
        return this.map { it.toDouble() }
            .average()
            .getResultBigDecimal()
    }

    private fun Double.getResultBigDecimal(): BigDecimal {
        return if (this.isNaN()) BigDecimal.ZERO else BigDecimal(this).setScale(2, RoundingMode.HALF_UP)
    }

}
