package masterthesis

object DataCapturing {
    val clusterSizes: MutableList<Int> = mutableListOf()
    val nodesInDeadCluster: MutableList<Double> = mutableListOf()
    val revenueInDeadCluster: MutableList<Double> = mutableListOf()
    val clusterPercentageIncluded: MutableList<Double> = mutableListOf()
    val clustersIncludedCompletely: MutableList<Double> = mutableListOf()
    val percentageBudgetUnused: MutableList<Double> = mutableListOf()

    val clusterSizesInstance: MutableList<Int> = mutableListOf()
    val nodesInDeadClusterInstance: MutableList<Double> = mutableListOf()
    val revenueInDeadClusterInstance: MutableList<Double> = mutableListOf()
    val clustersInstance: MutableList<Int> = mutableListOf()
    val clustersInPathInstance: MutableList<Int> = mutableListOf()
    val clustersIncludedCompletelyInstance: MutableList<Int> = mutableListOf()
    val percentageBudgetUnusedInstance: MutableList<Double> = mutableListOf()

    fun addClusterSize(sizes: List<Int>) {
        clusterSizes.addAll(sizes)
        clusterSizesInstance.addAll(sizes)
    }

    fun addNodesInDeadCluster(amount: Double) {
        nodesInDeadCluster.add(amount)
        nodesInDeadClusterInstance.add(amount)
    }

    fun addRevenueInDeadCluster(amount: Double) {
        revenueInDeadCluster.add(amount)
        revenueInDeadClusterInstance.add(amount)
    }

    fun addClustersInPath(clusters: Int, clustersInPath: Int) {
        clustersInstance.add(clusters)
        clustersInPathInstance.add(clustersInPath)
        clusterPercentageIncluded.add(clustersInPath.toDouble() / clusters.toDouble())
    }

    fun addClustersIncludedCompletely(clusters: Int, allClusters: Int) {
        clustersIncludedCompletely.add(clusters.toDouble() / allClusters.toDouble())
        clustersIncludedCompletelyInstance.add(clusters)
    }

    fun addPercentageBudgetUnused(usedBudget: Double, budget: Double) {
        percentageBudgetUnused.add((budget - usedBudget) / budget)
        percentageBudgetUnusedInstance.add((budget - usedBudget) / budget)
    }

    fun cleanup() {
        clusterSizesInstance.clear()
        nodesInDeadClusterInstance.clear()
        revenueInDeadClusterInstance.clear()
    }
}