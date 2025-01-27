package masterthesis.solver

import masterthesis.solver.model.Cluster
import org.jetbrains.kotlinx.dataframe.math.median
import org.slf4j.LoggerFactory

class BudgetCalculator {

    val logger = LoggerFactory.getLogger(BudgetCalculator::class.java)

    fun calculateBudgetElzein(clusters: List<Cluster>, budget: Double) {
        val weights = calculateWeightElzein(clusters)
        clusters.forEachIndexed { index, cluster ->
            cluster.budget = budget * weights[index]
        }
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

    fun calculateBudgetElzeinWithMin(clusters: List<Cluster>, budget: Double) {
        val minDistances = clusters.map { cluster ->
            getMinDistances(cluster)
        }
        val leftoverBudget = budget - minDistances.sum()
        val weights = calculateWeightElzein(clusters)
        clusters.forEachIndexed { index, cluster ->
            cluster.budget = leftoverBudget * weights[index] + minDistances[index]
        }
    }

    fun calculateBudgetConsiderOutliers(clusters: List<Cluster>, budget: Double, weightPercentage: Double) {
        val minDistances = clusters.map { cluster ->
            getMinDistances(cluster)
        }
        val leftoverBudget = budget - minDistances.sum()
        val distanceFactorsAbsolut = clusters.mapIndexed { index, cluster ->
            if (cluster.endNodes.first().id == -1) {
                cluster.nodes.sumOf {
                    it.revenue.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) * 2 - minDistances[index])
                } / cluster.nodes.size
            } else {
                cluster.nodes.sumOf {
                    it.revenue.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) + it.distanceTo(cluster.endNodes.first()) - minDistances[index])
                } / cluster.nodes.size
            }
        }
        val distanceFactorsAbsolutSum = distanceFactorsAbsolut.sum()
        val relativeDistanceFactors = distanceFactorsAbsolut.map { (it) / distanceFactorsAbsolutSum }
        val weightElzein = calculateWeightElzein(clusters)
        clusters.forEachIndexed { index, cluster ->
            cluster.budget =
                minDistances[index] +
                        weightPercentage * leftoverBudget * relativeDistanceFactors[index] +
                        (1 - weightPercentage) * leftoverBudget * weightElzein[index]
        }
    }

    fun calculateBudgetConsiderClusterMean(clusters: List<Cluster>, budget: Double, weightPercentage: Double) {
        val (minDistances, leftoverBudget) = clusters.map { cluster ->
            getMinDistancesToClusterMean(cluster)
        }.let { minDistances ->
            val leftoverBudget = budget - minDistances.sum()
            if (leftoverBudget < 0) {
                val minDistances2 = clusters.map { cluster ->
                    getMinDistances(cluster)
                }
                Pair(minDistances2, budget - minDistances2.sum())
            }
            Pair(minDistances, leftoverBudget)
        }
        val distanceToClusterFactorAbsolut = clusters.map { cluster ->
            cluster.nodes.sumOf {
                it.revenue / (1 + it.distanceTo(cluster))
            }
        }
        val distanceToClusterFactorSum = distanceToClusterFactorAbsolut.sum()
        val distanceToClusterFactorRelative = distanceToClusterFactorAbsolut.map { it / distanceToClusterFactorSum }
        val weightElzein = calculateWeightElzein(clusters)

        clusters.forEachIndexed { index, cluster ->
            cluster.budget =
                minDistances[index] +
                        weightPercentage * leftoverBudget * distanceToClusterFactorRelative[index] +
                        (1 - weightPercentage) * leftoverBudget * weightElzein[index]
        }
    }

    fun calculateBudgetNaive(clusters: List<Cluster>, budget: Double) {
        clusters.forEach { it.budget = budget / clusters.size }
    }

    private fun getMinDistances(cluster: Cluster): Double {
        if (cluster.endNodes.first().id == -1) {
            return 0.0
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