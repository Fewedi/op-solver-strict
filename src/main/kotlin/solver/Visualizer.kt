package solver


import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import org.jetbrains.kotlinx.kandy.dsl.categorical
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.layers.points
import org.slf4j.LoggerFactory


class Visualizer {
    private val logger = LoggerFactory.getLogger(Visualizer::class.java)

    fun plotGraph(clusterMap: Map<Int, Node>, clusters: List<Cluster>) {
        val xs = clusterMap.values.map { it.x }
        val ys = clusterMap.values.map { it.y }
        val cluster = clusterMap.values.map { it.cluster }

        val dataset = mapOf("xs" to xs, "ys" to ys, "cluster" to cluster)
        val paths = clusters.map { c ->
            logger.info("${c.id} ${c.solutionList.map { c.solutionList.map { it.id } }}")
            c.solutionList.zipWithNext()
        }.flatten()

        plot(dataset) {
            points {
                x("xs")
                y("ys")
                color("cluster") {
                    scale = categorical()
                }
            }
            paths.forEach {
                line {
                    x(it.toList().map { it.x })
                    y(it.toList().map { it.y })
                }
            }
        }.save("graph.png")
    }

}