package masterthesis.config

data class RootConfig (
    val activeProfile: Profile, // To store the active profile name
    val profiles: Map<Profile, ProfileConfig>
)

data class ProfileConfig (
    val mode: Mode,
    val analysis: Analysis,
    val bothRevenueDistribution: Boolean,
    val runs: Int,
    val parameterTuning: ParameterTuning?,
    val instance: Instance,
    val algorithm: Algorithm,
    val parameter: Parameter,
)

data class Analysis (
    val resultPath: String,
)

data class Parameter (
    val clusterEliminationThreshold: Double,
    val clusterEliminationRevenueWeight: Double,
    val clusterEliminationSparsityWeight: Double,
    val budgetWeight: Double,
    val clusterSize: Int
)

data class Algorithm (
    val solver: Solver,
    val ea4op: Ea4opConfig?,
    val gurobi: GurobiConfig?,
    val clustering: ClusteringMethod,
    val clusteringStatistic: AggregationMethod,
    val budgetDistribution: BudgetDistributionMethod,
    val clusterConnector: ClusterConnector,
    val tspCostMatrix: Symmetric,
    val clusterElimination: ClusterEliminationMethod
)

data class Instance (
    val origin: Origin,
    val testSet: TestSet,
    val budgetFactor: Double,
    val revenueDistribution: RevenueDistributionType,
)

data class ParameterTuning (
    val parameter: String,
    val startValue: Double,
    val endValue: Double,
    val stepSize: Double
)

data class Ea4opConfig (
    val startEntries: Int,
    val dummyStartNode: DummyStartNode,
    val dummyStartNodeMethod: AggregationMethod,
    val dummyStartNodeFactor: Double,
)

data class GurobiConfig (
    val clusterStartNode: AggregationMethod
)

enum class ClusterConnector {
    TSP,
    OP
}

enum class ClusterEliminationMethod {
    BASEDEPR,
    SPARSITYDEPR,
    LASTDEPR,
    LAST,
    DISTANCE,
    SPARSITY,
    NONE
}

enum class Symmetric {
    MAKESYMMETRIC,  // Possible values: makeSymmetric, nothing
    NOTHING
}

enum class Mode {
    RUN,
    PARAMETERSEARCH,
    CLUSTERINVESTIGATION,
    COMPARERESULTS,
}

enum class TestSet {
    ONE,
    HARD,
    BASE,
    ALL,
    TRAIN
}

enum class Solver {
    gurobi,
    ea4op
}

enum class Profile {
    clusterInvestigation,
    gurobiRun
}

enum class DummyStartNode {
    CLUSTER,
    NONE
}

enum class AggregationMethod {
    MEAN,
    MEDIAN
}

enum class ClusteringMethod {
    KMEANS,
    KMEANSUPPERBOUNDIGNOREOUTLIERS,
    KMEANSSPLIT,
    KMEANSFLOW,
    //deprecated
    KMEANSANDCORRECTLATER
}

enum class Origin{
    REF,
    OPLIB
}

enum class RevenueDistributionType {
    RANDOM,
    FLAT
}

enum class BudgetDistributionMethod {
    ELZEIN,
    ELZEINWITHMIN,
    CONSIDEROUTLIERS,
    CONSIDERCLUSTERMEAN,
    NAIVE
}