package masterthesis.solver.config

data class RootConfig (
    val activeProfile: Profile, // To store the active profile name
    val profiles: Map<Profile, ProfileConfig>
)

data class ProfileConfig (
    val testSet: TestSet,
    val solver: Solver,
    val ea4op: Ea4opConfig?,
    val gurobi: GurobiConfig?,
    val clustering: ClusteringMethod,
    val budgetDistribution: BudgetDistributionMethod,
    val revenueDistribution: RevenueDistributionType,
    val clusterSize: Int
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

enum class TestSet {
    ONE,
    HARD,
    BASE,
    ALL
}

enum class Solver {
    gurobi,
    ea4op
}

enum class Profile {
    oneStartPoint,
    noStartPoints,
    dummyStartNodeCluster,
    gurobiWithFixedEndPoints
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
    KMEANSCAPACITATEDCUSTOM
}

enum class RevenueDistributionType {
    RANDOM,
    FLAT
}

enum class BudgetDistributionMethod {
    ELZEIN,
    ELZEINWITHMIN,
    NAIVE
}