package masterthesis.solver

import com.fasterxml.jackson.databind.ObjectMapper
import masterthesis.config.AggregationMethod
import masterthesis.config.ConfigProvider
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import org.slf4j.LoggerFactory
import solver.ProblemParser
import kotlin.math.sqrt

class ClusterPathGenerator {

    private val logger = LoggerFactory.getLogger(ClusterPathGenerator::class.java)

    fun provideClusterPathOp(clusters: Map<Int, List<Node>>, budget: Double, objectMapper: ObjectMapper, gurobiClient: GurobiClient, problemParser: ProblemParser, convexHullGrahamScan: ConvexHullGrahamScan): List<Cluster> {
        val clusterList = clusters.map { clusterEntry ->
            val convexHull = convexHullGrahamScan.scan(clusterEntry.value)
            val convexSize = if (convexHull.isEmpty()) 0.0 else convexHullGrahamScan.polygonArea(convexHull)
            Cluster(
                id = clusterEntry.key,
                x = clusterEntry.value.map { node -> node.x }.average(),
                y = clusterEntry.value.map { node -> node.y }.average(),
                nodes = clusterEntry.value.toMutableList(),
                size = clusterEntry.value.size,
                isStart = clusterEntry.value.any { node -> node.startNode },
                convexHull = convexHull,
                convexSize = convexSize,
                maxBudget = ConfigProvider.config.parameter.maxBudgetFactor * sqrt( clusterEntry.value.size * convexSize),
                revenue = clusterEntry.value.sumOf { if(it.revenue!! < 0) 0 else it.revenue!! }
            )
        }.sortedBy { it.id }

        val preparedList = (listOf(clusterList.find { it.isStart } ?: clusterList.first())
            + clusterList.filter { !it.isStart })

        val endNode = Node(-1, -1.0, -1.0, 0)
        val newBudget = budget * ConfigProvider.config.parameter.clusterEliminationThreshold
        val pseudoNodes = preparedList.map { cluster ->
            val revenue = cluster.nodes.sumOf { it.revenue!! }
            Node(cluster.id, cluster.x, cluster.y, revenue)
        }.toMutableList().apply {
            add( endNode)
        }.toList()

        val solution = gurobiClient.solve(objectMapper, pseudoNodes, newBudget)
        val finalPath = problemParser.addSolutionToProblem(
            pseudoNodes,
            solution,
            pseudoNodes.first(),
            endNode
        ).mapNotNull { if (it.id == -1) null else clusterList[it.id] }

        return if (finalPath.size < clusterList.size) {
            logger.info("Gurobi cluster solution excluded ${clusterList.size - finalPath.size} of ${clusterList.size} clusters with: ${finalPath.map { it.id }}")
            finalPath
        }else {
            logger.info("Gurobi cluster solution included all clusters, OP seems not necessary, using concorde to solve TSP")
            provideClusterPathConcorde(clusters, convexHullGrahamScan)
        }
    }

    fun provideClusterPathConcorde(clusters: Map<Int, List<Node>>, convexHullGrahamScan: ConvexHullGrahamScan): List<Cluster> {
        val clusterList = clusters.map { clusterEntry ->
            val convexHull = convexHullGrahamScan.scan(clusterEntry.value)
            val convexSize = if (convexHull.isEmpty()) 0.0 else convexHullGrahamScan.polygonArea(convexHull)
            Cluster(
                id = clusterEntry.key,
                x = clusterEntry.value.map { it.x }.average(),
                y = clusterEntry.value.map { it.y }.average(),
                nodes = clusterEntry.value.toMutableList(),
                size = clusterEntry.value.size,
                isStart = clusterEntry.value.any { it.startNode },
                convexHull = convexHull,
                convexSize = convexSize,
                maxBudget = ConfigProvider.config.parameter.maxBudgetFactor * sqrt( clusterEntry.value.size * convexSize),
                revenue = clusterEntry.value.sumOf { if(it.revenue!! < 0) 0 else it.revenue!! }
            )
        }
        val startCluster = clusterList.find { it.isStart } ?: clusterList.first()
        val finalPath = ConcordeClient().solve(
            clusterList,
            startCluster
        )

        logger.info("Concord solution: ${finalPath.map { it.id }}")

        return finalPath
    }

    fun setDistancesToNextClusterAndProvideStartNodes(finalPath: List<Cluster>, startNodeProvider: StartNodeProvider) {
        finalPath.forEachIndexed { index, cluster ->
            if (index != finalPath.size - 1) {
                cluster.nextCluster = finalPath[index + 1]
                if (ConfigProvider.config.algorithm.gurobi!!.clusterStartNode == AggregationMethod.MEAN) {
                    cluster.nodes.forEach { node ->
                        node.distanceToNextCluster = node.distanceTo(cluster.nextCluster!!)
                    }
                }
            }
            if (index != 0) {
                cluster.prevCluster = finalPath[index - 1]
                if (ConfigProvider.config.algorithm.gurobi!!.clusterStartNode == AggregationMethod.MEAN) {
                    cluster.nodes.forEach { node ->
                        node.distanceToPrevCluster = node.distanceTo(cluster.prevCluster!!)
                    }
                }
            }
            cluster.startNodes.add(startNodeProvider.findStartNode(cluster))
        }
        finalPath.forEach { cluster ->
            cluster.endNodes.add(startNodeProvider.findEndNode(cluster))
        }
    }
}