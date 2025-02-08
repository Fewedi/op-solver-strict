package masterthesis

import masterthesis.config.*
import masterthesis.evaluation.*
import masterthesis.investigation.BudgetComparison
import masterthesis.solver.legacy.CleanupService
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

class MetaHandler {

    private val logger = LoggerFactory.getLogger(MetaHandler::class.java)

    private val cleanupService = CleanupService()
    private val csvClient = CsvClient()
    private val solver = Solver()
    private val budgetComparison = BudgetComparison()

    fun runAll() {
        cleanupService.finalCleanUp()
        val gen = "gen3"
        val folderName = "OPLib/instances/$gen" // The folder in your resources
        val fileNames = getTestSet(folderName, gen)

        if (ConfigProvider.config.bothRevenueDistribution) {
            setNewConfig(RevenueDistributionType.FLAT)
            manageRun(fileNames, gen)
            setNewConfig(RevenueDistributionType.RANDOM)
            manageRun(fileNames, gen)
        } else {
            manageRun(fileNames, gen)
        }
    }

    private fun manageRun(fileNames: List<String>, gen: String) {

        when (ConfigProvider.config.mode) {
            Mode.RUN -> {
                val results = runTestCases(fileNames, solver, gen)
                csvClient.writeCsv(results, getResultName("results"))
            }
            Mode.PARAMETERSEARCH -> {
                val valueList: MutableList<Double> = mutableListOf()
                var value = ConfigProvider.config.parameterTuning!!.startValue
                while (value <= ConfigProvider.config.parameterTuning!!.endValue) {
                    valueList.add(value)
                    value += ConfigProvider.config.parameterTuning!!.stepSize
                }
                val results = runParameterSearch(valueList, fileNames, solver, gen)
                csvClient.writeCsvParamBased(results, getResultName("parameters"), valueList)
            }
            Mode.CLUSTERINVESTIGATION -> {
                budgetComparison.prepareMultipleRuns(fileNames, gen)
            }
        }
    }

    private fun getResultName(prefix: String): String {
        val budgetDistribution = when(ConfigProvider.config.budgetDistribution) {
            BudgetDistributionMethod.CONSIDEROUTLIERS -> "co"
            BudgetDistributionMethod.CONSIDERCLUSTERMEAN -> "cm"
            BudgetDistributionMethod.ELZEIN -> "e"
            BudgetDistributionMethod.ELZEINWITHMIN -> "em"
            BudgetDistributionMethod.NAIVE -> "n"
        }
        val flatness = ConfigProvider.config.revenueDistribution.name.lowercase()
        return "${prefix}_${flatness}_$budgetDistribution.csv"
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
            val results = runTestCases(fileNames, solver, gen)
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

    private fun runTestCases(fileNames: List<String>, solver: Solver, gen: String): List<Any> {
        val results = fileNames.map { fileName ->
            logger.info("Solving $fileName")

            val results = mutableListOf<Result>()
            for (i in 1..ConfigProvider.config.runs) {
                try {
                    results.add(solver.solve(fileName, gen))
                } catch (e: Exception) {
                    logger.error("Failed to solve $fileName", e)
                    results.add(Result(fileName, 0, emptyList(), 0, false, 0, 0.0, 0.0))
                }
            }
            if (ConfigProvider.config.runs == 1) {
                mapResult(results.first())
            } else {
                aggregateResults(results)
            }
        }
        return results
    }

    private fun setNewConfig(newValue: Double) {
        ConfigProvider.setConfig(
            ConfigProvider.config.copy(
                budgetWeight = newValue
            )
        )
    }

    private fun setNewConfig(newValue: RevenueDistributionType) {
        ConfigProvider.setConfig(
            ConfigProvider.config.copy(
                revenueDistribution = newValue
            )
        )
    }


    private fun getTestSet(folderName: String, gen: String): List<String> {

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
            when (ConfigProvider.config.testSet) {
                TestSet.ONE -> listOf("eil101-$gen-50")
                TestSet.HARD -> listOf("rd400-$gen-50")
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
            budgetSpentAvg = successfulResults.sumOf { it.budgetSpent } / maxOf(results.filter { it.successful }.size, 1),
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
}
