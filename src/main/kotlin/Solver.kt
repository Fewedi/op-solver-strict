package masterthesis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import masterthesis.config.*
import masterthesis.config.Solver
import masterthesis.evaluation.Evaluater
import masterthesis.evaluation.Visualizer
import masterthesis.solver.*
import masterthesis.solver.legacy.*
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.ProblemSpace
import masterthesis.evaluation.Result
import org.jetbrains.kotlinx.dataframe.math.mean
import org.slf4j.LoggerFactory
import solver.ProblemParser

class Solver {

    private val logger = LoggerFactory.getLogger(Solver::class.java)
    private val cleanupService = CleanupService()
    private val problemParser = ProblemParser()
    private val clustering = Clustering()
    private val problemWriter: ProblemWriter? = when (ConfigProvider.config.ea4op?.startEntries) {
        0 -> ProblemWriterWithDummy()
        1 -> ProblemWriterNoDummy()
        else -> null
    }
    private val objectMapper = jacksonObjectMapper().apply {
        when (ConfigProvider.config.solver) {
            Solver.gurobi -> setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.UPPER_CAMEL_CASE)
            Solver.ea4op -> setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
        }
    }
    private val gkobeagaOpSolverClient = GkobeagaOpSolverClient()
    private val gurobiOpSolverClient = GurobiClient()
    private val tSPForCluster = TSPForCluster()
    private val visualizer = Visualizer()
    private val evaluater = Evaluater()
    private val budgetCalculator = BudgetCalculator()
    private val clusterCorrecter = ClusterCorrecter()
    private val clusterEliminator = ClusterEliminator()
    private val startNodeProvider = StartNodeProvider()

    fun solve(folderName: String, gen: String): Result {

        cleanupService.cleanUp()

        val startTime = System.nanoTime()
        val problemSpace = problemParser.readProblemSpace(folderName, gen)

        val budget = problemSpace.metaData.costLimit.toDouble() * ConfigProvider.config.budgetFactor
        logger.info("clustering ${problemSpace.nodeMap.size} nodes with method: ${ConfigProvider.config.clustering}")
        val clusterMap = when (ConfigProvider.config.clustering) {
            ClusteringMethod.KMEANSUPPERBOUND -> { clustering.clusterKmeansUpperBound(problemSpace.nodeMap) }
            ClusteringMethod.KMEANSUPPERBOUNDIGNOREOUTLIERS -> { clustering.clusterKmeansUpperBoundIgnoreOutliers(problemSpace.nodeMap) }
            ClusteringMethod.KMEANSCAPACITATEDCUSTOM -> { clustering.clusterCapacitatedCustom(problemSpace.nodeMap) }
            ClusteringMethod.KMEANSCAPACITATED -> { clustering.clusterCapacitated(problemSpace.nodeMap) }
            ClusteringMethod.KMEANS, ClusteringMethod.KMEANSANDCORRECTLATER -> { clustering.clusterKmeans(problemSpace.nodeMap) }
            ClusteringMethod.KMEANSSPLIT -> { clustering.clusterKmeansWithSplitting(problemSpace.nodeMap) }
        }
        logger.info("clustering done")

        logger.info("solving cluster TSP with ${clusterMap.size} clusters in concorde")
        val originalClusterPath = tSPForCluster.provideClusterPathConcorde(clusterMap)
        logger.info("solving cluster TSP done")

        val correctedClusterPath = when (ConfigProvider.config.clustering) {
            ClusteringMethod.KMEANSANDCORRECTLATER -> { clusterCorrecter.correctClusterSizes(originalClusterPath, ConfigProvider.config.clusterSize) }
            ClusteringMethod.KMEANSSPLIT -> { clusterCorrecter.mergeSmallClusters(originalClusterPath, ConfigProvider.config.clusterSize) }
            else -> { originalClusterPath }
        }

        tSPForCluster.setDistancesToNextClusterAndProvideStartNodes(correctedClusterPath, startNodeProvider)

        val revenueMean = problemSpace.nodeMap.values.map { it.revenue }.mean()

        val clusterPath = when (ConfigProvider.config.clusterElimination) {
            ClusterEliminationMethod.BASE -> { clusterEliminator.eliminateUnnecessaryClusters(correctedClusterPath.toMutableList(), budget, revenueMean, budgetCalculator, startNodeProvider) }
            ClusterEliminationMethod.SPARSITY -> { clusterEliminator.eliminateUnnecessaryClustersConsiderSparsity(correctedClusterPath.toMutableList(), budget, revenueMean, budgetCalculator, startNodeProvider) }
            ClusterEliminationMethod.LAST -> { clusterEliminator.eliminateClustersFromBack(correctedClusterPath.toMutableList(), budget, revenueMean, budgetCalculator, startNodeProvider) }
        }
        logger.info("cluster elimination removed ${correctedClusterPath.size - clusterPath.size} clusters")

        when (ConfigProvider.config.budgetDistribution) {
            BudgetDistributionMethod.ELZEIN -> { budgetCalculator.calculateBudgetElzein(
                clusterPath,
                budget
            ) }
            BudgetDistributionMethod.ELZEINWITHMIN -> { budgetCalculator.calculateBudgetElzeinWithMin(clusterPath, budget) }
            BudgetDistributionMethod.CONSIDEROUTLIERS -> { budgetCalculator.calculateBudgetConsiderDetours(clusterPath, budget,
                ConfigProvider.config.budgetWeight) }
            BudgetDistributionMethod.CONSIDERCLUSTERMEAN -> { budgetCalculator.calculateBudgetConsiderClusterMean(clusterPath, budget,
                ConfigProvider.config.budgetWeight) }
            BudgetDistributionMethod.NAIVE -> { budgetCalculator.calculateBudgetNaive(clusterPath, budget) }
        }

        logger.info("solving clusters with ${ConfigProvider.config.solver}")
        try {
            val clusters = clusterPath.map { cluster ->
                when (ConfigProvider.config.solver) {
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