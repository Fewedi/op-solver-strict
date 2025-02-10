package masterthesis.solver

import masterthesis.solver.model.Cluster
import org.jetbrains.kotlinx.dataframe.math.median
import org.jetbrains.kotlinx.dataframe.math.medianOrNull
import org.slf4j.LoggerFactory

class BudgetCalculator {

    val logger = LoggerFactory.getLogger(BudgetCalculator::class.java)

    fun calculateBudgetElzein(clusters: List<Cluster>, budget: Double) {
        val weights = calculateWeightElzein(clusters)
        clusters.forEachIndexed { index, cluster ->
            cluster.budget = budget * weights[index]
        }
    }

    fun calculateBudgetElzeinWithMin(clusters: List<Cluster>, budget: Double): List<Double> {
        val minDistances = clusters.map { cluster ->
            getMinDistances(cluster)
        }
        val leftoverBudget = budget - minDistances.sum()
        val weights = calculateWeightElzein(clusters)
        clusters.forEachIndexed { index, cluster ->
            cluster.budget = leftoverBudget * weights[index] + minDistances[index]
        }
        return List(clusters.size) { index -> leftoverBudget * weights[index] + minDistances[index]}
    }

    fun calculateBudgetConsiderClusterMean(clusters: List<Cluster>, budget: Double, weightFactor: Double) {
        val minDistances = clusters.map { cluster ->
            getMinDistancesToClusterMean(cluster)
        }
        val leftoverBudget = budget - minDistances.sum()
        val distanceFactorsAbsolut = clusters.map { cluster ->
            (cluster.nodes.filter { node -> !node.startNode && !node.endNode }.map {
                it.revenue.toDouble() / (1 + it.distanceTo(cluster))
            }.medianOrNull() ?: 0.0) * cluster.nodes.size
        }
        val distanceFactorsAbsolutSum = distanceFactorsAbsolut.sum()
        val relativeDistanceFactors = distanceFactorsAbsolut.map { (it) / distanceFactorsAbsolutSum }
        clusters.forEachIndexed { index, cluster ->
            cluster.budget = minDistances[index] + leftoverBudget * relativeDistanceFactors[index]
        }
    }


    fun calculateBudgetConsiderDetours(clusters: List<Cluster>, budget: Double, weightFactor: Double) {
        val minDistances = clusters.map { cluster ->
            getMinDistances(cluster)
        }
        val leftoverBudget = budget - minDistances.sum()
        val distanceFactorsAbsolut = clusters.mapIndexed { index, cluster ->
            if (cluster.endNodes.first().id == -1) {
                (cluster.nodes.filter { node -> !node.startNode && !node.endNode }.map {
                    it.revenue.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) * 2 - minDistances[index])
                }.medianOrNull() ?: 0.0) * cluster.nodes.size
            } else {
                (cluster.nodes.filter { node -> !node.startNode && !node.endNode }.map {
                    it.revenue.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) + it.distanceTo(cluster.endNodes.first()) - minDistances[index])
                }.medianOrNull() ?: 0.0) * cluster.nodes.size
            }
        }

        val distanceFactorsAbsolutSum = distanceFactorsAbsolut.sum()
        val relativeDistanceFactors = distanceFactorsAbsolut.map { (it) / distanceFactorsAbsolutSum }
        clusters.forEachIndexed { index, cluster ->
            cluster.budget = minDistances[index] + leftoverBudget * relativeDistanceFactors[index]
        }
    }

    fun calculateBudgetNaive(clusters: List<Cluster>, budget: Double) {
        clusters.forEach { it.budget = budget / clusters.size }
    }

    private fun calculateWeightElzein(clusters: List<Cluster>): List<Double> {
        val delta = clusters.map { cluster ->
            val ui = cluster.nodes.map { it.revenue }.median()
            cluster.nodes.size * ui
        }
        val deltaSum = delta.sum()
        return List(clusters.size) { index ->
            delta[index].toDouble() / deltaSum.toDouble()
        }
    }

    fun getMinDistances(cluster: Cluster): Double {
        if (cluster.endNodes.isEmpty()) return 0.0
        if (cluster.endNodes.first().id == -1) {
            return cluster.startNodes.first().distanceTo(cluster)
        }
        return cluster.startNodes.first().distanceTo(cluster.endNodes.first())
    }

    private fun getMinDistancesToClusterMean(cluster: Cluster): Double {
        if (cluster.endNodes.first().id == -1) {
            return cluster.startNodes.first().distanceTo(cluster)
        }
        return cluster.startNodes.first().distanceTo(cluster) + cluster.endNodes.first().distanceTo(cluster)
    }
}