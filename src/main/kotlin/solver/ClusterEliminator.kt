package masterthesis.solver

import masterthesis.config.ConfigProvider
import masterthesis.solver.model.Cluster

class ClusterEliminator {

    fun eliminateClustersFromBack(
        path: MutableList<Cluster>,
        budget: Double,
        budgetCalculator: BudgetCalculator,
        startNodeProvider: StartNodeProvider
    ): List<Cluster> {
        var change = true
        while (change) {

            val fixedPercentage = path.sumOf { budgetCalculator.getMinDistances(it) } / budget
            if (fixedPercentage > ConfigProvider.config.parameter.clusterEliminationThreshold
            ) {
                val cluster = path.removeLast()
                change = true
                updateNeighbour(cluster, startNodeProvider)
            } else {
                change = false
            }
        }
        return path
    }

    fun eliminateUnnecessaryClusters(
        path: MutableList<Cluster>,
        budget: Double,
        revenueMean: Double,
        budgetCalculator: BudgetCalculator,
        startNodeProvider: StartNodeProvider
    ): List<Cluster> {
        var change = true
        while (change) {
            val importance = getImportanceMeasure(path, budget, revenueMean, budgetCalculator)
            val minImportance = importance.minOrNull()!!
            if (pathNeedsCorrection(path, budget, budgetCalculator)
                || minImportance < ConfigProvider.config.parameter.clusterEliminationThreshold
            ) {
                updateNeighbour(
                    path[importance.indexOf(minImportance)],
                    startNodeProvider
                )
                path.removeAt(importance.indexOf(minImportance))
                change = true
            } else {
                change = false
            }
        }
        return path
    }

    fun eliminateUnnecessaryClustersConsiderSparsity(
        path: MutableList<Cluster>,
        budget: Double,
        revenueMean: Double,
        budgetCalculator: BudgetCalculator,
        startNodeProvider: StartNodeProvider
    ): List<Cluster> {
        var change = true
        val clusterSparsityRelative = getClusterSparsity(path)
        while (change) {
            val importance = getImportanceMeasure(path, budget, revenueMean, budgetCalculator, clusterSparsityRelative)
            val minImportance = importance.minOrNull()!!
            if (pathNeedsCorrection(path, budget, budgetCalculator)
                || minImportance < ConfigProvider.config.parameter.clusterEliminationThreshold
            ) {
                updateNeighbour(
                    path[importance.indexOf(minImportance)],
                    startNodeProvider
                )
                path.removeAt(importance.indexOf(minImportance))
                change = true
            } else {
                change = false
            }
        }
        return path
    }

    private fun pathNeedsCorrection(path: List<Cluster>, budget: Double, budgetCalculator: BudgetCalculator): Boolean {
        val minBudget = path.sumOf { budgetCalculator.getMinDistances(it) }
        return minBudget > budget
    }

    private fun updateNeighbour(cluster: Cluster, startNodeProvider: StartNodeProvider) {
        cluster.nextCluster?.apply {
            this.prevCluster = cluster.prevCluster
            this.startNodes.clear()
            this.startNodes.add(startNodeProvider.findStartNode(this))
        }
        cluster.prevCluster?.apply {
            this.nextCluster = cluster.nextCluster
            this.endNodes.clear()
            this.endNodes.add(startNodeProvider.findEndNode(this))
        }
    }

    private fun getClusterSparsity(path: List<Cluster>): List<Double> {
        val clusterSparsity = path.map { cluster ->
            cluster.nodes.map { node ->
                cluster.nodes.map { node2 -> node.distanceTo(node2) }
            }.flatten().average()
        }
        val clusterSparsityMean = clusterSparsity.average()
        return clusterSparsity.map { clusterSparsityMean / it }
    }

    private fun getImportanceMeasure(path: List<Cluster>, budget: Double, revenueMean: Double, budgetCalculator: BudgetCalculator, clusterSparsity: List<Double>? = null): List<Double> {
        val budgetPerCluster = budget / path.size

        val elzeinMesure = budgetCalculator.calculateWeightElzein(path).map { it * budget } // up
        return path.mapIndexed { index, cluster ->
            when (cluster) {
                path.first() -> Double.POSITIVE_INFINITY
                path.last() -> {
                    val distToPrev = cluster.distanceTo(cluster.prevCluster!!)
                    val potentialProfit = elzeinMesure[index] // up
                    val sparsity = clusterSparsity?.get(index) ?: 1.0
                    sparsity * potentialProfit * budgetPerCluster / (distToPrev + 1)
                }
                else -> {
                    val distToNext = cluster.distanceTo(cluster.nextCluster!!)
                    val distToPrev = cluster.distanceTo(cluster.prevCluster!!)
                    val distPrevToNext = cluster.prevCluster!!.distanceTo(cluster.nextCluster!!)
                    val potentialProfit = cluster.nodes.sumOf { it.revenue } / revenueMean // up
                    val lostBudget = distToNext + distToPrev - distPrevToNext // down
                    val sparsity = clusterSparsity?.get(index) ?: 1.0
                    sparsity * potentialProfit * budgetPerCluster / (lostBudget + 1)
                }
            }
        }
    }
}