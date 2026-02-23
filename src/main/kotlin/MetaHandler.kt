package masterthesis

import masterthesis.config.*
import masterthesis.evaluation.*
import masterthesis.evaluation.model.AggregatedResult
import masterthesis.evaluation.model.ExperimentSpecification
import masterthesis.evaluation.model.ParameterSearchResult
import masterthesis.evaluation.model.Result
import masterthesis.evaluation.model.ResultOutput
import masterthesis.investigation.BudgetComparison
import masterthesis.solver.legacy.CleanupService
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

class MetaHandler {

    private val logger = LoggerFactory.getLogger(MetaHandler::class.java)

    private val cleanupService = CleanupService()
    private val csvClient = CsvClient()
    private val solver = Solver()
    private val budgetComparison = BudgetComparison()
    private val resultMerger = ResultMerger()

    private var maxRun = -1
    private var currentRun = 0

    fun runAll() {
        cleanupService.finalCleanUp()
        val gen = when (ConfigProvider.config.instance.revenueDistribution) {
            RevenueDistributionType.FLAT -> "gen1"
            RevenueDistributionType.RANDOM -> "gen2"
        }
        val fileNames = when (ConfigProvider.config.instance.origin) {
            Origin.OPLIB -> {
                val folderName = "OPLib/instances/$gen"
                getTestSetOplib(folderName, gen)
            }
            Origin.REF -> getTestSetRef()
        }

        if (ConfigProvider.config.bothRevenueDistribution) {
            setNewConfig(RevenueDistributionType.FLAT)
            manageRun(fileNames, gen)
            setNewConfig(RevenueDistributionType.RANDOM)
            manageRun(fileNames, gen)
        } else {
            manageRun(fileNames, gen)
        }
    }

    private fun getParameterList(): List<Double> {
        val valueList: MutableList<Double> = mutableListOf()
        var value = ConfigProvider.config.parameterTuning!!.startValue
        while (value <= ConfigProvider.config.parameterTuning!!.endValue) {
            valueList.add(value)
            value += ConfigProvider.config.parameterTuning!!.stepSize
        }
        return valueList
    }

    private fun calculateRunAmount(fileNames: List<String>): Int {
        var maxRun = fileNames.size * ConfigProvider.config.runs
        if (ConfigProvider.config.mode == Mode.PARAMETERSEARCH) {
            val valueList = getParameterList()
            maxRun *= valueList.size
        }
        logger.info("Max runs: $maxRun")
        return maxRun
    }

    private fun manageRun(fileNames: List<String>, gen: String) {

        maxRun = calculateRunAmount(fileNames)
        when (ConfigProvider.config.mode) {
            Mode.RUN -> {
                val experimentString = getExperimentString()
                logger.info("----- Running experiment: $experimentString -----")
                val results = runTestCases(fileNames, solver, gen, experimentString)
                csvClient.writeCsv(results, "${experimentString.toFileNameString()}.csv", "results/")
            }
            Mode.PARAMETERSEARCH -> {
                val valueList = getParameterList()
                val experimentString = getExperimentString().toFileNameString(
                    ExperimentSpecification.Parameter.fromString(ConfigProvider.config.parameterTuning!!.parameter))
                val paramString = getParamString()
                val results = runParameterSearch(valueList, fileNames, solver, gen)
                csvClient.writeCsvParamBased(results, "param_${experimentString}_${paramString}.csv", valueList)
            }
            Mode.CLUSTERINVESTIGATION -> {
                budgetComparison.prepareMultipleRuns(fileNames, gen)
            }
            Mode.COMPARERESULTS -> {
                resultMerger.mergeResults(csvClient)
            }
        }
    }

    private fun getParamString(): String {
        val start = ConfigProvider.config.parameterTuning?.startValue ?: 0.0
        val end = ConfigProvider.config.parameterTuning?.endValue ?: 0.0
        val step = ConfigProvider.config.parameterTuning?.stepSize ?: 0.0
        return "${ConfigProvider.config.parameterTuning?.parameter?.lowercase()}_${start}_${end}_${step}"
    }

    private fun getResultName(prefix: String): String {
        val budgetDistribution = ConfigProvider.config.algorithm.budgetDistribution.let {
            when (it) {
                BudgetDistributionMethod.CONSIDERDETOUR -> "cd"
                BudgetDistributionMethod.CONSIDERSPARSENESS -> "cs"
                BudgetDistributionMethod.ELZEIN -> "el"
                BudgetDistributionMethod.NAIVE -> "eq"
            }
        } + when (ConfigProvider.config.algorithm.budgetMinCalculation) {
            BudgetMinCalculationMethod.NONE -> ""
            BudgetMinCalculationMethod.MIN -> "ml"
            BudgetMinCalculationMethod.CENTER -> "c"
        } + if (ConfigProvider.config.algorithm.useMaxBudget) "mx" else ""
        val flatness = ConfigProvider.config.instance.revenueDistribution.name.lowercase()
        val paramName = ConfigProvider.config.parameterTuning?.parameter?.lowercase() ?: "none"
        val op = (ConfigProvider.config.parameter.clusterEliminationThreshold * 100).toInt().toString()
        val budget = (ConfigProvider.config.instance.budgetFactor * 100).toInt().toString()
        return "${prefix}_${flatness}_${budgetDistribution}_${paramName}_${op}_${budget}.csv"

    }

    private fun runParameterSearch(
        valueList: List<Double>,
        fileNames: List<String>,
        solver: Solver,
        gen: String
    ): List<ParameterSearchResult> {
        val resultsCase = fileNames.map { ParameterSearchResult(name = it, values = mutableListOf()) }
        valueList.map { newValue ->
            setNewConfig(newValue)
            val experimentString = getExperimentString()
            val results = runTestCases(fileNames, solver, gen, experimentString)
            results.forEach { result ->
                when (result) {
                    is ResultOutput ->
                        resultsCase.find { result.name == it.name }?.values?.add(result.revenue)
                    is AggregatedResult -> resultsCase.find { result.name == it.name }?.values?.add(result.revenueAvg)
                    else -> {
                        logger.error("Unknown result type")
                    }
                }
            }
        }
        return resultsCase
    }

    private fun runTestCases(fileNames: List<String>, solver: Solver, gen: String, experimentSpecification: ExperimentSpecification): List<Any> {
        val results = fileNames.map { fileName ->
            logger.info("Solving $fileName")

            val results = mutableListOf<Result>()
            for (i in 1..ConfigProvider.config.runs) {
                try {
                    currentRun++
                    logger.info("----------------------------------------------")
                    logger.info("--------------- RUN $currentRun OF $maxRun ---------------")
                    logger.info("----------------------------------------------")
                    val r = Runtime.getRuntime()
                    logger.info("heap: max=${r.maxMemory() / 1024 / 1024}MB, allocated=${r.totalMemory() / 1024 / 1024}MB, free=${r.freeMemory() / 1024 / 1024}MB")
                    results.add(solver.solve(fileName, gen))
                } catch (e: Exception) {
                    logger.error("Failed to solve $fileName", e)
                    results.add(Result(fileName, 0, emptyList(), 0, false, 0, 0.0, 0.0))
                }
            }

            logger.info("Mean cluster size: ${DataCapturing.clusterSizes.average()}")
            logger.info("Mean nodes in dead cluster to all nodes: ${DataCapturing.nodesInDeadCluster.average()}")
            logger.info("Mean revenue in dead cluster to overall revenue: ${DataCapturing.revenueInDeadCluster.average()}")

            csvClient.writeLineToCsv(experimentSpecification.getExperimentResultsInstance(), fileName)
            DataCapturing.cleanup()

            if (ConfigProvider.config.runs == 1) {
                mapResult(results.first())
            } else {
                aggregateResults(results)
            }
        }
        csvClient.writeLineToCsv(experimentSpecification.getExperimentResultsGlobal(), "global")
        DataCapturing.finalCleanup()
        return results
    }

    private fun setNewConfig(newValue: Any) {
        val oldParamConfigParameter = ConfigProvider.config.parameter

        val constructor = oldParamConfigParameter::class.primaryConstructor ?: throw IllegalArgumentException("No primary constructor found")

        val params = constructor.parameters.associateWith { param ->
            if (param.name == ConfigProvider.config.parameterTuning!!.parameter) {
                newValue
            } else {
                oldParamConfigParameter::class.memberProperties
                    .first { it.name == param.name }
                    .apply { isAccessible = true }
                    .getter.call(oldParamConfigParameter)
            }
        }

        val newParameter = constructor.callBy(params)
        val newConfig = ConfigProvider.config.copy(
            parameter = newParameter
        )
        ConfigProvider.setConfig(newConfig)
    }

    private fun getTestSetRef(): List<String> {

        val baseList = listOf(
            "eil101",
            "gil262",
            "pr299",
            "lin318",
            "rd400",
            "d493",
            "u574",
            "u724",
            "pcb1173",
            "fl1400",
            "pr2392"
        )

        return baseList.filter {
            when (ConfigProvider.config.instance.testSet) {
                TestSet.ONE -> listOf("eil101")
                TestSet.HARD -> listOf("fl1400")
                TestSet.BASE -> baseList
                TestSet.TRAIN -> baseList - baseList.toSet() // dont
                TestSet.ALL -> baseList
            }.contains(it)
        }
    }
    private fun getTestSetOplib(folderName: String, gen: String): List<String> {

        val classLoader = Thread.currentThread().contextClassLoader
        val fileNames = classLoader.getResource(folderName)?.let { folder ->
            File(folder.toURI()).listFiles()?.map { it.name.split(".")[0] } ?: emptyList()
        } ?: throw IllegalArgumentException("Folder '$folderName' not found!")


        val baseList = listOf(
            "eil101-$gen-50",
            "gil262-$gen-50",
            "pr299-$gen-50",
            "lin318-$gen-50",
            "rd400-$gen-50",
            "d493-$gen-50",
            "u574-$gen-50",
            "u724-$gen-50",
            "pcb1173-$gen-50",
            "fl1400-$gen-50",
            "pr2392-$gen-50"
        )

        return fileNames.filter {
            when (ConfigProvider.config.instance.testSet) {
                TestSet.ONE -> listOf("eil101-$gen-50")
                TestSet.HARD -> listOf("fl1400-$gen-50","lin318-$gen-50")
                TestSet.BASE -> baseList
                TestSet.TRAIN -> fileNames - baseList.toSet()
                TestSet.ALL -> fileNames
            }.contains(it)
        }
    }

    private fun aggregateResults(results: List<Result>): AggregatedResult {
        val successfulResults = results.filter { it.successful }
            .ifEmpty { listOf(results.first()) }
        return AggregatedResult(
            name = results.first().name,
            size = successfulResults.let { it.ifEmpty { results } }.first().size,
            budget = successfulResults.let { it.ifEmpty { results } }.first().budget,
            successfulAmount = successfulResults.size,
            revenueMin = successfulResults.minOf { it.revenue },
            revenueAvg = successfulResults.sumOf { it.revenue } / maxOf(results.filter { it.successful }.size, 1),
            revenueMax = successfulResults.maxOf { it.revenue },
            budgetSpentAvg = successfulResults.sumOf { it.budgetSpent } / maxOf(
                results.filter { it.successful }.size,
                1
            ),
            timeMin = successfulResults.minOf { it.time },
            timeAvg = (successfulResults.sumOf { it.time } / maxOf(results.filter { it.successful }.size, 1)).let {
                BigDecimal(it).setScale(
                    2,
                    RoundingMode.HALF_UP
                ).toDouble()
            },
            timeMax = successfulResults.maxOf { it.time }
        )
    }

    private fun mapResult(result: Result): ResultOutput {
        return ResultOutput(
            name = result.name,
            size = result.size,
            budget = result.budget,
            successful = result.successful,
            revenue = result.revenue,
            budgetSpent = result.budgetSpent,
            time = result.time
        )
    }

    private fun getExperimentString(): ExperimentSpecification{
        val set = ConfigProvider.config.instance.testSet.name.lowercase()
        val mode = ConfigProvider.config.instance.revenueDistribution.name.lowercase()
        val runs = ConfigProvider.config.runs
        val budgetFactor = BigDecimal.valueOf(ConfigProvider.config.instance.budgetFactor).setScale(2, RoundingMode.HALF_UP)
        val k = ConfigProvider.config.parameter.clusterSize
        val clustering = ConfigProvider.config.algorithm.clustering.let {
            when (it) {
                ClusteringMethod.KMEANS -> "k"
                ClusteringMethod.KMEANSUPPERBOUNDIGNOREOUTLIERS -> "nck"
                ClusteringMethod.KMEANSSPLIT -> "rk"
                ClusteringMethod.KMEANSFLOW -> "fbck"
                ClusteringMethod.KMEANSANDCORRECTLATER -> "clk"
            }
        } + ConfigProvider.config.algorithm.clusteringStatistic.let{
            when (it) {
                AggregationMethod.MEAN -> "mn"
                AggregationMethod.MEDIAN -> "md"
            }
        }
        val elimination = ConfigProvider.config.let {
            when (it.algorithm.clusterConnector) {
                ClusterConnector.OP -> "op"
                ClusterConnector.TSP -> {
                    when (it.algorithm.clusterElimination) {
                        ClusterEliminationMethod.LASTDEPR, ClusterEliminationMethod.LAST -> "tsprfb"
                        ClusterEliminationMethod.BASEDEPR, ClusterEliminationMethod.DISTANCE -> "tsprce"
                        ClusterEliminationMethod.SPARSITYDEPR, ClusterEliminationMethod.SPARSITY -> "tsprcs"
                        ClusterEliminationMethod.NONE -> "tspn-deprec"
                    }
                }
            }
        }
        val revenueWeight = BigDecimal.valueOf(ConfigProvider.config.parameter.clusterEliminationRevenueWeight).setScale(2, RoundingMode.HALF_UP)
        val sparsityWeight = BigDecimal.valueOf(ConfigProvider.config.parameter.clusterEliminationSparsityWeight).setScale(2, RoundingMode.HALF_UP)
        val r = BigDecimal.valueOf(ConfigProvider.config.parameter.clusterEliminationThreshold).setScale(2, RoundingMode.HALF_UP)
        val budgetDist = ConfigProvider.config.algorithm.budgetDistribution.let {
            when (it) {
                BudgetDistributionMethod.CONSIDERDETOUR -> "cd"
                BudgetDistributionMethod.CONSIDERSPARSENESS -> "cs"
                BudgetDistributionMethod.ELZEIN -> "el"
                BudgetDistributionMethod.NAIVE -> "eq"
            }
        } + when (ConfigProvider.config.algorithm.budgetMinCalculation) {
            BudgetMinCalculationMethod.NONE -> ""
            BudgetMinCalculationMethod.MIN -> "ml"
            BudgetMinCalculationMethod.CENTER -> "c"
        } + if (ConfigProvider.config.algorithm.useMaxBudget) "mx" else ""
        return ExperimentSpecification(
            set = set,
            mode = mode,
            runs = runs,
            budgetFactor = budgetFactor,
            k = k,
            clustering = clustering,
            elimination = elimination,
            r = r,
            budgetDist = budgetDist,
            eliminationRevenueWeight = revenueWeight,
            eliminationSparsityWeight = sparsityWeight
        )
    }
}
