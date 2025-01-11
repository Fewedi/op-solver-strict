package masterthesis.evaluation


import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import org.jetbrains.kotlinx.kandy.dsl.categorical
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.layers.points
import org.jetbrains.kotlinx.kandy.letsplot.settings.Symbol
import org.slf4j.LoggerFactory


class Visualizer {
    private val logger = LoggerFactory.getLogger(Visualizer::class.java)

    fun plotGraph(clusterMap: Map<Int, Node>, clusters: List<Cluster>, name: String) {

        val startPosX = clusters.map { it.startNodes.first().x }
        val startPosY = clusters.map { it.startNodes.first().y }
        val endPosX = clusters.map { it.endNodes.first().x }
        val endPosY = clusters.map { it.endNodes.first().y }

        val xs = clusterMap.values.map { it.x }
        val ys = clusterMap.values.map { it.y }
        val cluster = clusterMap.values.map { it.cluster }

        val paths = clusters.map { c ->
            logger.info("${c.id} ${c.solutionList.map { it.id } }")
            if (c.endNodes.first().id == -1) {
                c.solutionList
            }else {
                c.solutionList + listOf(c.endNodes.first())
            }.zipWithNext()
        }.flatten()

        plot {
            // Plot the first dataset (clusterDataSet)
            points {
                x(startPosX)
                y(startPosY)
                symbol = Symbol.CIRCLE_OPEN
            }

            points {
                x(endPosX)
                y(endPosY)
                symbol = Symbol.CROSS
            }

            // Plot the second dataset (dataset)
            points {
                x(xs)
                y(ys)
                color(cluster) {
                    scale = categorical()
                }
            }

            // Add the lines (paths) for the second dataset
            paths.forEach {
                line {
                    x(it.toList().map { it.x })
                    y(it.toList().map { it.y })
                }
            }
        }.save("$name.png")
    }

}