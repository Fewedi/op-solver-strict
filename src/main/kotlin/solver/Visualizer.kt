package solver


import org.jetbrains.kotlinx.kandy.dsl.categorical
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.layers.points


class Visualizer {

    fun plotGraph(problem: Problem) {
        val xs = problem.graph.vertexSet().map { it.x }

        val ys = problem.graph.vertexSet().map { it.y }

        val xpath = problem.walk.vertexList.map { it.x }
        val ypath = problem.walk.vertexList.map { it.y }

        plot {
            points {
                x(xs)
                y(ys)
            }
            line {
                x(xpath)
                y(ypath)
            }
        }.save("graph.png")
    }

    fun plotPaths(problem: Problem) {

        val nodes = problem.graph.vertexSet().toList()

        val xs = nodes.map { it.x }
        val ys = nodes.map { it.y }
        val cluster = nodes.map { it.cluster }

        val dataset = mapOf("xs" to xs, "ys" to ys, "cluster" to cluster)


        val xpaths = problem.clusterWalks.keys.map { clusterN ->
            clusterN to problem.clusterWalks[clusterN]!!.vertexList.map { it.x }
        }
        val ypaths = problem.clusterWalks.keys.map { clusterN ->
            clusterN to problem.clusterWalks[clusterN]!!.vertexList.map { it.y }
        }

        plot(dataset) {
            points {
                x("xs")
                y("ys")
                color("cluster") {
                    scale = categorical()
                }
            }
            line {
                x(xpaths.first().second)
                y(ypaths.first().second)
            }
            line {
                x(xpaths.last().second)
                y(ypaths.last().second)
            }

        }.save("graph.png")
    }
}