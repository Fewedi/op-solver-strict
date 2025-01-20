package masterthesis.solver

import masterthesis.solver.model.Cluster
import org.jetbrains.kotlinx.dataframe.math.median
import org.slf4j.LoggerFactory

class BudgetCalculator {

    val logger = LoggerFactory.getLogger(BudgetCalculator::class.java)

    fun calculateBudgetElzein(clusters: List<Cluster>, budget: Double, minimumBudget: List<Double>? = null) {
        val delta = clusters.map { cluster ->
            val ui = cluster.nodes.map { it.revenue }.median()
            cluster.nodes.size * ui
        }
        val deltaSum = delta.sum()
        clusters.forEachIndexed { index, cluster ->
            logger.info("Cluster {} with budget {} and minimumBudget {}", cluster.id, (budget * delta[index] / deltaSum),(minimumBudget?.get(index) ?: 0.0))
            cluster.budget = (budget * delta[index] / deltaSum) + (minimumBudget?.get(index) ?: 0.0)
        }
    }

    fun calculateBudgetElzeinWithMin(clusters: List<Cluster>, budget: Double) {
        val minDistances = clusters.map { cluster ->
            if (cluster.endNodes.first().id == -1) {
                0.0
            } else {
                cluster.startNodes.first().distanceTo(cluster.endNodes.first())
            }
        }

        val leftoverBudget = budget - minDistances.sum()


        logger.info("Min distances: {} with budget {} and leftoverBudget {}", minDistances, budget, leftoverBudget)


        calculateBudgetElzein(clusters, leftoverBudget, minDistances)
    }

    fun calculateBudgetNaive(clusters: List<Cluster>, budget: Double) {
        clusters.forEach { it.budget = budget / clusters.size }
    }
}