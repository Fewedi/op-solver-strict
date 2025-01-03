package masterthesis.solver.config

data class RootConfig (
    val activeProfile: Profile, // To store the active profile name
    val profiles: Map<Profile, ProfileConfig>
)

data class ProfileConfig (
    val solver: Solver,
    val clustering: ClusteringMethod,
    val startEntries: Int,
    val dummyStartNode: DummyStartNode,
    val dummyStartNodeMethod: AgregationMethod,
    val dummyStartNodeFactor: Double,
    val clusterSize: Int
)

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
    NONE
}