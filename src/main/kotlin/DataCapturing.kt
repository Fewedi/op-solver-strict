package masterthesis

object DataCapturing {
    val clusterSizes: MutableList<Int> = mutableListOf()
    val nodesInDeadCluster: MutableList<Double> = mutableListOf()
    val revenueInDeadCluster: MutableList<Double> = mutableListOf()
    val clusterPercentageIncluded: MutableList<Double> = mutableListOf()
    val clustersIncludedCompletely: MutableList<Double> = mutableListOf()
    val percentageBudgetUnused: MutableList<Double> = mutableListOf()
    val fractionClustersExcluded: MutableList<Double> = mutableListOf()
    val fractionClusterRevenueExcluded: MutableList<Double> = mutableListOf()
    var defaultToEqualBudget: Int = 0
    var budgetSpilloverBudgetFirstIteration: MutableList<Double> = mutableListOf()
    var budgetSpilloversFirstIteration: MutableList<Double> = mutableListOf()
    var budgetSpillovers: MutableList<Double> = mutableListOf()
    var budgetSpilloverIterations: MutableList<Int> = mutableListOf()

    val clusterSizesInstance: MutableList<Int> = mutableListOf()
    val nodesInDeadClusterInstance: MutableList<Double> = mutableListOf()
    val revenueInDeadClusterInstance: MutableList<Double> = mutableListOf()
    val clustersInstance: MutableList<Int> = mutableListOf()
    val clustersInPathInstance: MutableList<Int> = mutableListOf()
    val clustersIncludedCompletelyInstance: MutableList<Int> = mutableListOf()
    val percentageBudgetUnusedInstance: MutableList<Double> = mutableListOf()
    val clustersExcludedInstance: MutableList<Int> = mutableListOf()
    val fractionClustersExcludedInstance: MutableList<Double> = mutableListOf()
    val fractionClusterRevenueExcludedInstance: MutableList<Double> = mutableListOf()
    var defaultToEqualBudgetInstance: Int = 0
    val budgetSpilloverBudgetFirstIterationInstance:  MutableList<Double> = mutableListOf()
    val budgetSpilloversFirstIterationInstance: MutableList<Double> = mutableListOf()
    val budgetSpilloversInstance: MutableList<Double> = mutableListOf()
    val budgetSpilloverIterationsInstance: MutableList<Int> = mutableListOf()

    fun addClusterSize(sizes: List<Int>) {
        clusterSizes.addAll(sizes)
        clusterSizesInstance.addAll(sizes)
    }

    fun addClusterFractionData(amountClusters: Int, amountClustersInPath: Int, revenueClusters: Int, revenueClustersInPath: Int) {
        val currentClustersExcluded = amountClusters - amountClustersInPath
        val currentFractionClustersExcluded = currentClustersExcluded.toDouble() / amountClusters.toDouble()
        val currentFractionClusterRevenueExcluded = (revenueClusters - revenueClustersInPath).toDouble() / revenueClusters.toDouble()
        clustersExcludedInstance.add(currentClustersExcluded)
        fractionClustersExcludedInstance.add(currentFractionClustersExcluded)
        fractionClusterRevenueExcludedInstance.add(currentFractionClusterRevenueExcluded)
        fractionClustersExcluded.add(currentFractionClustersExcluded)
        fractionClusterRevenueExcluded.add(currentFractionClusterRevenueExcluded)
    }

    fun addAmountClusterBudgetSpillOverFirstIteration(amountClusters: Int, budget: Double, leftOverBudget: Double, spilloversHappened: Int) {
        budgetSpilloverBudgetFirstIteration.add(leftOverBudget / budget)
        budgetSpilloverBudgetFirstIterationInstance.add(leftOverBudget / budget)
        budgetSpilloversFirstIterationInstance.add(spilloversHappened.toDouble() / amountClusters.toDouble() )
        budgetSpilloversFirstIteration.add( spilloversHappened.toDouble() / amountClusters.toDouble())
    }

    fun addOverallAmountClusterBudgetSpillOver(amountClusters: Int, spilloversHappened: Int, additionalIterations: Int) {
        budgetSpilloversInstance.add(spilloversHappened.toDouble() / amountClusters.toDouble())
        budgetSpillovers.add( spilloversHappened.toDouble() / amountClusters.toDouble())
        budgetSpilloverIterationsInstance.add(additionalIterations)
        budgetSpilloverIterations.add(additionalIterations)
    }


    fun budgetDistDefaulted() {
        defaultToEqualBudget++
        defaultToEqualBudgetInstance++
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
        clustersInstance.clear()
        clustersInPathInstance.clear()
        clustersIncludedCompletelyInstance.clear()
        percentageBudgetUnusedInstance.clear()
        clustersExcludedInstance.clear()
        fractionClustersExcludedInstance.clear()
        fractionClusterRevenueExcludedInstance.clear()
        defaultToEqualBudgetInstance = 0
        budgetSpilloverBudgetFirstIterationInstance.clear()
        budgetSpilloversFirstIterationInstance.clear()
        budgetSpilloversInstance.clear()
        budgetSpilloverIterationsInstance.clear()
    }

    fun finalCleanup() {
        clusterSizes.clear()
        nodesInDeadCluster.clear()
        revenueInDeadCluster.clear()
        clusterPercentageIncluded.clear()
        clustersIncludedCompletely.clear()
        percentageBudgetUnused.clear()
        fractionClustersExcluded.clear()
        fractionClusterRevenueExcluded.clear()
        defaultToEqualBudget = 0
        budgetSpilloverBudgetFirstIteration.clear()
        budgetSpilloversFirstIteration.clear()
        budgetSpillovers.clear()
        budgetSpilloverIterations.clear()
        cleanup()
    }
}