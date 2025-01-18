package masterthesis.solver

import masterthesis.solver.model.Cluster
import org.jetbrains.kotlinx.dataframe.math.median

class BudgetCalculator {

    fun calculateBudgetElzein(clusters: List<Cluster>, budget: Double) {
        val delta = clusters.map { cluster ->
            val ui = cluster.nodes.map { it.revenue }.median()
            cluster.nodes.size * ui
        }
        val deltaSum = delta.sum()
        clusters.forEachIndexed { index, cluster ->
            cluster.budget = budget * delta[index] / deltaSum
        }
    }

    fun calculateBudgetNaive(clusters: List<Cluster>, budget: Double) {
        clusters.forEach { it.budget = budget / clusters.size }
    }
}