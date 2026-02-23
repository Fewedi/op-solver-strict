package masterthesis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import masterthesis.config.*
import masterthesis.config.Solver
import masterthesis.evaluation.Evaluater
import masterthesis.evaluation.Visualizer
import masterthesis.evaluation.model.Result
import masterthesis.solver.*
import masterthesis.solver.legacy.*
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.ProblemSpace
import org.slf4j.LoggerFactory
import solver.ProblemParser

class Solver {

    private val logger = LoggerFactory.getLogger(Solver::class.java)
    private val cleanupService = CleanupService()
    private val problemParser = ProblemParser()
    private val clustering = Clustering()
    private val problemWriter: ProblemWriter? = when (ConfigProvider.config.algorithm.ea4op?.startEntries) {
        0 -> ProblemWriterWithDummy()
        1 -> ProblemWriterNoDummy()
        else -> null
    }
    private val objectMapper = jacksonObjectMapper().apply {
        when (ConfigProvider.config.algorithm.solver) {
            Solver.gurobi -> setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.UPPER_CAMEL_CASE)
            Solver.ea4op -> setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
        }
    }
    private val gkobeagaOpSolverClient = GkobeagaOpSolverClient()
    private val gurobiOpSolverClient = GurobiClient()
    private val tSPForCluster = ClusterPathGenerator()
    private val visualizer = Visualizer()
    private val evaluater = Evaluater()
    private val budgetCalculator = BudgetCalculator()
    private val clusterCorrecter = ClusterCorrecter()
    private val clusterEliminator = ClusterEliminator()
    private val startNodeProvider = StartNodeProvider()
    private val convexHullGrahamScan = ConvexHullGrahamScan()

    fun solve(folderName: String, gen: String): Result {

        cleanupService.cleanUp()

        val startTime = System.nanoTime()
        val problemSpace = problemParser.readProblemSpace(folderName, gen)
        val clusterSize = ConfigProvider.config.parameter.clusterSize - 1

        val budget = problemSpace.metaData.costLimit * ConfigProvider.config.instance.budgetFactor
        logger.info("clustering ${problemSpace.nodeMap.size} nodes with method: ${ConfigProvider.config.algorithm.clustering}")
        val statistic = ConfigProvider.config.algorithm.clusteringStatistic
        val clusterMap = when (ConfigProvider.config.algorithm.clustering) {
            ClusteringMethod.KMEANSUPPERBOUNDIGNOREOUTLIERS -> { clustering.clusterKmeansUpperBoundIncludeOutliers(problemSpace.nodeMap, statistic) }
            ClusteringMethod.KMEANS, ClusteringMethod.KMEANSANDCORRECTLATER -> { clustering.clusterKmeans(problemSpace.nodeMap, statistic) }
            ClusteringMethod.KMEANSSPLIT -> { clustering.clusterKmeansWithSplitting(problemSpace.nodeMap, statistic) }
            ClusteringMethod.KMEANSFLOW -> { clustering.clusterKmeansFlow(problemSpace.nodeMap, statistic) }
        }

        DataCapturing.addClusterSize(clusterMap.map { it.value.size })
        logger.info("clustering done")

        logger.info("solving cluster path with ${clusterMap.size} clusters")
        val originalClusterPath = when (ConfigProvider.config.algorithm.clusterConnector) {
            ClusterConnector.TSP -> { tSPForCluster.provideClusterPathConcorde(clusterMap, convexHullGrahamScan) }
            ClusterConnector.OP -> { tSPForCluster.provideClusterPathOp(clusterMap, budget, objectMapper, gurobiOpSolverClient, problemParser, convexHullGrahamScan) }
        }
        logger.info("solving cluster path done")

        val correctedClusterPath = when (ConfigProvider.config.algorithm.clustering) {
            ClusteringMethod.KMEANSANDCORRECTLATER -> { clusterCorrecter.correctClusterSizes(originalClusterPath, clusterSize) }
            else -> { originalClusterPath }
        }

        tSPForCluster.setDistancesToNextClusterAndProvideStartNodes(correctedClusterPath, startNodeProvider)

        val revenueMean = problemSpace.nodeMap.values.map { it.revenue!! }.average()

        val clusterPath = when (ConfigProvider.config.algorithm.clusterElimination) {
            ClusterEliminationMethod.BASEDEPR -> { clusterEliminator.eliminateUnnecessaryClusters(correctedClusterPath.toMutableList(), budget, revenueMean, budgetCalculator, startNodeProvider) }
            ClusterEliminationMethod.SPARSITYDEPR -> { clusterEliminator.eliminateUnnecessaryClustersConsiderSparsity(correctedClusterPath.toMutableList(), budget, revenueMean, budgetCalculator, startNodeProvider) }
            ClusterEliminationMethod.LASTDEPR -> { clusterEliminator.eliminateClustersFromBack(correctedClusterPath.toMutableList(), budget, budgetCalculator, startNodeProvider) }
            ClusterEliminationMethod.LAST -> { clusterEliminator.eliminateClusters(correctedClusterPath.toMutableList(), budget, budgetCalculator, startNodeProvider, clusterEliminator::removeLastCluster)}
            ClusterEliminationMethod.DISTANCE -> { clusterEliminator.eliminateClusters(correctedClusterPath.toMutableList(), budget, budgetCalculator, startNodeProvider, clusterEliminator::removeClusterByConnectionEffort)}
            ClusterEliminationMethod.SPARSITY -> { clusterEliminator.eliminateClusters(correctedClusterPath.toMutableList(), budget, budgetCalculator, startNodeProvider, clusterEliminator::removeClusterBySparsity)}
            ClusterEliminationMethod.NONE -> { correctedClusterPath }
        }
        DataCapturing.addClusterFractionData(correctedClusterPath.size, clusterPath.size
            , correctedClusterPath.sumOf { it.revenue }, clusterPath.sumOf { it.revenue })
        logger.info("cluster elimination removed ${correctedClusterPath.size - clusterPath.size} clusters")

        budgetCalculator.calculateBudget(
            clusterPath,
            budget,
            when (ConfigProvider.config.algorithm.budgetDistribution) {
                BudgetDistributionMethod.ELZEIN -> { budgetCalculator::calculateWeightElzein }
                BudgetDistributionMethod.CONSIDERSPARSENESS -> { budgetCalculator::calculateWeightSparseness }
                BudgetDistributionMethod.CONSIDERDETOUR -> { budgetCalculator::calculateWeightDetourEasy }
                BudgetDistributionMethod.NAIVE -> { budgetCalculator::calculateWeightNaive }
            },
            when (ConfigProvider.config.algorithm.budgetMinCalculation) {
                BudgetMinCalculationMethod.MIN -> { budgetCalculator::getMinDistances }
                BudgetMinCalculationMethod.CENTER -> { budgetCalculator::getMinDistancesToClusterMean }
                BudgetMinCalculationMethod.NONE -> { _ : Cluster -> 0.0 }
            },
            ConfigProvider.config.algorithm.useMaxBudget
        )


        logger.info("solving clusters with ${ConfigProvider.config.algorithm.solver}")
        try {
            val clusters = clusterPath.map { cluster ->
                when (ConfigProvider.config.algorithm.solver) {
                    Solver.gurobi -> {
                        solveWithGurobi(cluster, cluster.budget)
                    }

                    Solver.ea4op -> {
                        solveWithEa4op(
                            cluster,
                            problemSpace,
                            cluster.budget,
                            "src/main/resources/a280-$gen-50-cluster-${cluster.id}.oplib"
                        )
                    }
                }
                cluster
            }

            val endTime = System.nanoTime()
            val duration = (endTime - startTime) / 1_000_000_000.0
            return evaluater.evaluateResult(problemSpace, clusters, duration, folderName,clusterPath, visualizer)
        }catch (e: Exception){
            visualizer.plotGraph(problemSpace.nodeMap, clusterPath, folderName, 0)
            throw e
        }
    }

    fun solveWithGurobi(cluster: Cluster, budget: Double) {
        try {
            val startNodeIndex = cluster.nodes.indexOf(cluster.startNodes.first())
            val preparedList =
                listOf(cluster.nodes[startNodeIndex]) + cluster.nodes.filterIndexed { index, _ -> index != startNodeIndex } + listOf(
                    cluster.endNodes.first()
                )
            val solution = gurobiOpSolverClient.solve(objectMapper, preparedList, budget)
            cluster.solutionList = problemParser.addSolutionToProblem(
                preparedList,
                solution,
                cluster.startNodes.first(),
                cluster.endNodes.first()
            )
            logger.info("Cluster ${cluster.id} solved with solution: ${cluster.solutionList.map { it.id }}")
        } catch (e: Exception) {
            logger.error("Cluster ${cluster.id} failed with error: ${e.message}")
            throw e
        }
    }

    private fun solveWithEa4op(cluster: Cluster, problemSpace: ProblemSpace, budget: Double, path: String) {
        try {
            problemWriter!!.writeCluster(
                problemSpace.metaData,
                problemSpace.distanceMatrix,
                cluster,
                path,
                budget
            )
            val solution = gkobeagaOpSolverClient.solve(path, objectMapper)
            cluster.solutionList = problemParser.addSolutionToProblem(cluster.solutionMap, solution!!)

        } catch (e: Exception) {
            logger.error("Cluster ${cluster.id} failed with error: ${e.message}")
        }
    }
}