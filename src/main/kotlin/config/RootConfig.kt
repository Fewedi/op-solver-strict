package masterthesis.config

data class RootConfig (
    val activeProfile: Profile, // To store the active profile name
    val profiles: Map<Profile, ProfileConfig>
)

data class ProfileConfig (
    val mode: Mode,
    val bothRevenueDistribution: Boolean,
    val runs: Int,
    val parameterTuning: ParameterTuning?,
    val instance: Instance,
    val algorithm: Algorithm,
    val parameter: Parameter,
)

data class Parameter (
    val clusterOpBudget: Double,
    val clusterEliminationThreshold: Double,
    val budgetWeight: Double,
    val clusterSize: Int
)

data class Algorithm (
    val solver: Solver,
    val ea4op: Ea4opConfig?,
    val gurobi: GurobiConfig?,
    val clustering: ClusteringMethod,
    val budgetDistribution: BudgetDistributionMethod,
    val clusterConnector: ClusterConnector,
    val clusterElimination: ClusterEliminationMethod
)

data class Instance (
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
    val dummyStartNodeMethod: AgregationMethod,
    val dummyStartNodeFactor: Double,
)

data class GurobiConfig (
    val clusterStartNode: AgregationMethod
)

enum class ClusterConnector {
    TSP,
    OP
}

enum class ClusterEliminationMethod {
    BASE,
    SPARSITY,
    LAST,
    NONE
}

enum class Mode {
    RUN,
    PARAMETERSEARCH,
    CLUSTERINVESTIGATION
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

enum class AgregationMethod {
    MEAN,
    MEDIAN,
    NONE
}

enum class ClusteringMethod {
    KMEANS,
    KMEANSANDCORRECTLATER,
    KMEANSUPPERBOUND,
    KMEANSUPPERBOUNDIGNOREOUTLIERS,
    KMEANSCAPACITATED,
    KMEANSCAPACITATEDCUSTOM,
    KMEANSSPLIT
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