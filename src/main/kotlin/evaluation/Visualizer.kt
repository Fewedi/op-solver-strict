package masterthesis.evaluation


import masterthesis.config.ConfigProvider
import masterthesis.config.Mode
import masterthesis.investigation.ClusterMetric
import masterthesis.solver.model.Cluster
import masterthesis.solver.model.Node
import org.jetbrains.kotlinx.dataframe.math.median
import org.jetbrains.kotlinx.kandy.dsl.categorical
import org.jetbrains.kotlinx.kandy.dsl.continuous
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.layers.points
import org.jetbrains.kotlinx.kandy.letsplot.multiplot.plotBunch
import org.jetbrains.kotlinx.kandy.letsplot.settings.Symbol
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import kotlin.math.min
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class Visualizer {
    private val logger = LoggerFactory.getLogger(Visualizer::class.java)

    fun plotClusterMetrics(clusters: List<ClusterMetric>, xMetric: String, yMetric: String) {

        val xProp = ClusterMetric::class.memberProperties.firstOrNull { it.name == xMetric }
        val yProp = ClusterMetric::class.memberProperties.firstOrNull { it.name == yMetric }

        val xs = when (xProp?.get(clusters.first())) {
            is BigDecimal -> {
                clusters.map { xProp?.get(it) as BigDecimal }.map { it.toDouble() }
            }

            is Int -> {
                clusters.map { xProp?.get(it) as Int }.map { it.toDouble() }
            }

            else -> {
                clusters.map { xProp?.get(it) as Number }.map { it.toDouble() }
            }
        }
        val ys = when (yProp?.get(clusters.first())) {
            is BigDecimal -> {
                clusters.map { yProp?.get(it) as BigDecimal }.map { it.toDouble() }
            }

            is Int -> {
                clusters.map { yProp?.get(it) as Int }.map { it.toDouble() }
            }

            else -> {
                clusters.map { yProp?.get(it) as Number }.map { it.toDouble() }
            }
        }
        val xmax = xs.median() * 2
        val ymax = ys.median() * 2
        val instances = clusters.map { it._name }

        plot {
            points {
                x(xs) {
                    scale = continuous(0.0..xmax)
                }
                y(ys) {
                    scale = continuous(0.0..ymax)
                }
                color(instances) {
                    scale = categorical()
                }
            }
            layout {
                style {
                    xAxisLabel = xMetric
                    yAxisLabel = yMetric
                }
            }
        }.save("cluster-metrics-$xMetric-$yMetric.png")
    }

    fun plotClusterMetrics(clusters: List<ClusterMetric>, xMetrics: List<String>, yMetric: String) {


        plotBunch {

            xMetrics.forEachIndexed { index, xMetric ->
                val xProp = ClusterMetric::class.memberProperties.firstOrNull { it.name == xMetric }
                val yProp = ClusterMetric::class.memberProperties.firstOrNull { it.name == yMetric }

                val xs = when (xProp?.get(clusters.first())) {
                    is BigDecimal -> {
                        clusters.map { xProp?.get(it) as BigDecimal }.map { it.toDouble() }
                    }

                    is Int -> {
                        clusters.map { xProp?.get(it) as Int }.map { it.toDouble() }
                    }

                    else -> {
                        clusters.map { xProp?.get(it) as Number }.map { it.toDouble() }
                    }
                }
                val ys = when (yProp?.get(clusters.first())) {
                    is BigDecimal -> {
                        clusters.map { yProp?.get(it) as BigDecimal }.map { it.toDouble() }
                    }

                    is Int -> {
                        clusters.map { yProp?.get(it) as Int }.map { it.toDouble() }
                    }

                    else -> {
                        clusters.map { yProp?.get(it) as Number }.map { it.toDouble() }
                    }
                }
                val xmax = min(xs.median() * 4, xs.max())
                val ymax = ys.median() * 2
                val instances = clusters.map { it._name }

                add(
                    plot {
                        points {
                            x(xs) {
                                scale = continuous(0.0..xmax)
                            }
                            y(ys) {
                                scale = continuous(0.0..ymax)
                            }
                            color(instances) {
                                scale = categorical()
                            }
                        }
                        layout {
                            style {
                                xAxisLabel = xMetric
                                yAxisLabel = yMetric
                            }
                        }
                    },
                    x = index.mod(4) * 450,
                    y = index.div(4) * 400,
                )
            }

        }.save("cluster-metrics-$yMetric.png")

    }


    fun plotGraph(clusterMap: Map<Int, Node>, clusters: List<Cluster>, name: String, totalRevenue: Int) {

        val startPosX = clusters.map { it.startNodes.first().x }
        val startPosY = clusters.map { it.startNodes.first().y }
        val endPosX = clusters.filter { it.endNodes.first().id != -1 }.map { it.endNodes.first().x }
        val endPosY = clusters.filter { it.endNodes.first().id != -1 }.map { it.endNodes.first().y }

        val xs = clusterMap.values.map { it.x }
        val ys = clusterMap.values.map { it.y }
        val cluster = clusterMap.values.map { it.cluster }
        val revenue = clusterMap.values.map { it.revenue }

        val paths = if (totalRevenue > 0) {
            clusters.map { c ->
                logger.info("${c.id} ${c.solutionList.map { it.id }}")
                if (c.endNodes.first().id == -1) {
                    c.solutionList
                } else {
                    c.solutionList + listOf(c.endNodes.first())
                }.zipWithNext()
            }.flatten()
        } else {
            emptyList()
        }

        File("lets-plot-images/$name").mkdirs()
        val shortName = name.split("-").first().trim()
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
                alpha(revenue) {
                    scale = continuous(range = (0.1..1.0))
                }
            }

            // Add the lines (paths) for the second dataset
            paths.forEach {
                line {
                    x(it.toList().map { it.x })
                    y(it.toList().map { it.y })
                }
            }
        }.save(getName(name, shortName, totalRevenue))
    }

    private fun getName(name: String, shortName: String, totalRevenue: Int): String {

        val config = ConfigProvider.config


        return when (config.mode) {
            Mode.RUN -> {
                "$name/$shortName-$totalRevenue.png"
            }

            Mode.PARAMETERSEARCH -> {
                when (config.parameterTuning!!.parameter) {
                    "budgetFactor" -> "$name/$shortName-$totalRevenue-${config.budgetFactor}.png"
                    "clusterEliminationThreshold" -> "$name/$shortName-$totalRevenue-${config.clusterEliminationThreshold}.png"
                    "budgetWeight" -> "$name/$shortName-$totalRevenue-${config.budgetWeight}.png"
                    else -> "$name/$shortName-$totalRevenue.png"
                }
            }

            Mode.CLUSTERINVESTIGATION -> {
                val param = config.budgetWeight
                "$name/$shortName-$totalRevenue-$param.png"
            }
        }
    }
}