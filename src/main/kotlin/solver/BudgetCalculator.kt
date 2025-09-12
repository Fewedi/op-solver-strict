package masterthesis.solver

import masterthesis.config.ConfigProvider
import masterthesis.solver.model.Cluster
import org.jetbrains.kotlinx.dataframe.math.median
import org.jetbrains.kotlinx.dataframe.math.medianOrNull
import org.slf4j.LoggerFactory
import kotlin.math.sqrt

class BudgetCalculator {

    val logger = LoggerFactory.getLogger(BudgetCalculator::class.java)


    fun calculateBudget1(clusters: List<Cluster>, budget: Double, method: (List<Cluster>) -> List<Double>, minBudgetMethod: (Cluster) -> Double, useMaxValue: Boolean) {
        var minDistances = clusters.map { cluster ->
            minBudgetMethod(cluster)
        }

        if (minDistances.sum() > budget) {
            logger.warn("Budget $budget is smaller than the sum of minimum required distances ${minDistances.sum()}, assigning minimum distances as budget")
            minDistances = clusters.map { cluster ->
                getMinDistancesStrict(cluster)
            }
        }
        var leftOverBudget = budget - minDistances.sum()
        val weights = method(clusters)

        if (useMaxValue) {
            clusters.forEachIndexed { index, cluster ->
                cluster.budget = maxOf(minDistances[index], minOf(cluster.maxBudget, minDistances[index] + leftOverBudget * weights[index]))
            }
            leftOverBudget = budget - clusters.sumOf { it.budget }
            var nonFullClusters = clusters.filter { it.budget < it.maxBudget }
            while (leftOverBudget > 5 && nonFullClusters.isNotEmpty()) {
                logger.info("Distributing left over budget $leftOverBudget to ${nonFullClusters.size} non full clusters")
                val newWeights = method(nonFullClusters)
                nonFullClusters.forEachIndexed { index, cluster ->
                    cluster.budget = minOf(cluster.maxBudget, cluster.budget + leftOverBudget * newWeights[index])
                }
                leftOverBudget = budget - clusters.sumOf { it.budget }
                nonFullClusters = clusters.filter { it.budget < it.maxBudget }
                if (nonFullClusters.isEmpty()) {
                    clusters.forEach { it.budget += leftOverBudget / clusters.size.toDouble() }
                    logger.warn("All clusters are full but there is still budget left over, distributing evenly")
                    break
                }
            }

        } else {
            clusters.forEachIndexed { index, cluster ->
                cluster.budget = maxOf(minDistances[index], minDistances[index] + leftOverBudget * weights[index])
            }
        }
    }

    fun calculateBudget(
        clusters: List<Cluster>,
        budget: Double,
        method: (List<Cluster>) -> List<Double>,
        minBudgetMethod: (Cluster) -> Double,
        useMaxValue: Boolean
    ) {
        var minDistances = clusters.map { cluster -> minBudgetMethod(cluster) }

        if (minDistances.sum() > budget) {
            logger.warn(
                "Budget $budget is smaller than the sum of minimum required distances ${minDistances.sum()}, " +
                        "assigning minimum distances as budget"
            )
            minDistances = clusters.map { cluster -> getMinDistancesStrict(cluster) }
        }

        var leftOverBudget = budget - minDistances.sum()

        if (useMaxValue) {
            // initialize with minimum budgets
            clusters.forEachIndexed { index, cluster -> cluster.budget = minDistances[index] }

            var nonFullClusters: List<Cluster> = clusters
            do {
                nonFullClusters = clusters.filter { it.budget < it.maxBudget }
                if (nonFullClusters.isEmpty()) break

                val weights = method(nonFullClusters)

                nonFullClusters.forEachIndexed { i, cluster ->
                    val share = leftOverBudget * weights[i]
                    cluster.budget = minOf(cluster.maxBudget, cluster.budget + share)
                }

                nonFullClusters = clusters.filter { it.budget < it.maxBudget }
                leftOverBudget = budget - clusters.sumOf { it.budget }

            } while (leftOverBudget > 5 && nonFullClusters.isNotEmpty())

            if (nonFullClusters.isEmpty() && leftOverBudget > 0) {
                val add = leftOverBudget / clusters.size.toDouble()
                clusters.forEach { it.budget += add }
                logger.warn("All clusters are full but there is still budget left over, distributing evenly")
            }

        } else {
            val weights = method(clusters)
            clusters.forEachIndexed { index, cluster ->
                cluster.budget = maxOf(minDistances[index], minDistances[index] + leftOverBudget * weights[index])
            }
        }
    }

    fun calculateWeightNaive(clusters: List<Cluster>): List<Double> {
        return List(clusters.size) { 1.0 / clusters.size }
    }

    fun calculateWeightElzein(clusters: List<Cluster>): List<Double> {
        val delta = clusters.map { cluster ->
            val ui = cluster.nodes.map { it.revenue!! }.median()
            cluster.nodes.size * ui
        }
        val deltaSum = delta.sum()
        return List(clusters.size) { index ->
            delta[index].toDouble() / deltaSum.toDouble()
        }
    }

    fun calculateWeightSparseness(clusters: List<Cluster>): List<Double> {
        val elzein = calculateWeightElzein(clusters)
        val sparsness = clusters.map { cluster -> sqrt( cluster.convexSize.toDouble() + 1) }

        logger.info("Elzein = ${elzein.map { String.format("%.4f", it) }}")
        val minElzein = elzein.minOrNull()!!
        val maxElzein = elzein.maxOrNull()!!
        val minSparsness = sparsness.minOrNull()!!
        val maxSparsness = sparsness.maxOrNull()!!

        val normalizedElzein = elzein.map { normalize(it, minElzein,maxElzein) }
        val normalizedSparsness = sparsness.map { normalize(it, minSparsness,maxSparsness) }

        val sparsnessWeight = ConfigProvider.config.parameter.budgetWeight

        val weight = normalizedElzein.mapIndexed { index, sparsnessValue ->
            sparsnessValue + sparsnessWeight * normalizedSparsness[index]
        }
        val weightSum = weight.sum()
        return weight.map { it / weightSum  }
    }

    private fun normalize(value: Double, min: Double, max: Double): Double {
        return if (max == min) 1.0 else (value - min) / (max - min)
    }

    fun calculateWeightDetour(clusters: List<Cluster>): List<Double> {
        val minDistances = clusters.map { cluster ->
            getMinDistances(cluster)
        }
        val distanceFactors = clusters.mapIndexed { index, cluster ->
            if (cluster.endNodes.first().id == -1) {
                (cluster.nodes.filter { node -> !node.startNode && !node.endNode }.map {
                    it.revenue!!.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) * 2 - minDistances[index])
                }.medianOrNull() ?: 0.0) * cluster.nodes.size
            } else {
                (cluster.nodes.filter { node -> !node.startNode && !node.endNode }.map {
                    it.revenue!!.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) + it.distanceTo(cluster.endNodes.first()) - minDistances[index])
                }.medianOrNull() ?: 0.0) * cluster.nodes.size
            }
        }
        val elzein = calculateWeightElzein(clusters)

        val minElzein = elzein.minOrNull()!!
        val maxElzein = elzein.maxOrNull()!!
        val minDistanceFactors = distanceFactors.minOrNull()!!
        val maxDistanceFactors = distanceFactors.maxOrNull()!!

        val normalizedElzein = elzein.map { normalize(it, minElzein,maxElzein) }
        val normalizedSparsness = distanceFactors.map { normalize(it, minDistanceFactors,maxDistanceFactors) }

        val sparsnessWeight = ConfigProvider.config.parameter.budgetWeight

        val weight = normalizedElzein.mapIndexed { index, sparsnessValue ->
            sparsnessValue + sparsnessWeight * normalizedSparsness[index]
        }
        val weightSum = weight.sum()
        return weight.map { it / weightSum  }
    }


    fun calculateWeightDetourEasy(clusters: List<Cluster>): List<Double> {
        val minDistances = clusters.map { cluster ->
            getMinDistances(cluster)
        }
        val weight = clusters.mapIndexed { index, cluster ->
            if (cluster.endNodes.first().id == -1) {
                (cluster.nodes.filter { node -> !node.startNode && !node.endNode }.map {
                    it.revenue!!.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) * 2 - minDistances[index])
                }.medianOrNull() ?: 0.0) * cluster.nodes.size
            } else {
                (cluster.nodes.filter { node -> !node.startNode && !node.endNode }.map {
                    it.revenue!!.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) + it.distanceTo(cluster.endNodes.first()) - minDistances[index])
                }.medianOrNull() ?: 0.0) * cluster.nodes.size
            }
        }
        val weightSum = weight.sum()
        return weight.map { it / weightSum  }
    }

    fun getMinDistancesStrict(cluster: Cluster): Double {
        if (cluster.endNodes.isEmpty() || cluster.endNodes.first().id == -1) return 0.0
        return cluster.startNodes.first().distanceTo(cluster.endNodes.first())
    }

    fun getMinDistances(cluster: Cluster): Double {
        if (cluster.endNodes.isEmpty() || cluster.endNodes.first().id == -1) {
            return cluster.startNodes.first().distanceTo(cluster)
        }
        return cluster.startNodes.first().distanceTo(cluster.endNodes.first())
    }

    fun getMinDistancesToClusterMean(cluster: Cluster): Double {
        if (cluster.endNodes.first().id == -1) {
            return cluster.startNodes.first().distanceTo(cluster)
        }
        return cluster.startNodes.first().distanceTo(cluster) + cluster.endNodes.first().distanceTo(cluster)
    }






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
                it.revenue!!.toDouble() / (1 + it.distanceTo(cluster))
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
                    it.revenue!!.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) * 2 - minDistances[index])
                }.medianOrNull() ?: 0.0) * cluster.nodes.size
            } else {
                (cluster.nodes.filter { node -> !node.startNode && !node.endNode }.map {
                    it.revenue!!.toDouble() / (1 + it.distanceTo(cluster.startNodes.first()) + it.distanceTo(cluster.endNodes.first()) - minDistances[index])
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


}