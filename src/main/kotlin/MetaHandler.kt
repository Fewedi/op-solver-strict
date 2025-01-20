package masterthesis

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import masterthesis.evaluation.AggregatedResultOutput
import masterthesis.evaluation.Result
import masterthesis.evaluation.ResultOutput
import masterthesis.solver.config.ConfigProvider
import masterthesis.solver.config.TestSet
import masterthesis.solver.legacy.CleanupService
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.full.declaredMemberProperties

class MetaHandler {

    private val logger = LoggerFactory.getLogger(MetaHandler::class.java)

    private val cleanupService = CleanupService()

    fun runAll() {

        cleanupService.finalCleanUp()
        val gen = "gen3"
        val folderName = "OPLib/instances/$gen" // The folder in your resources

        val fileNames = getTestSet(folderName, gen)

        val solver = Solver()

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
        writeCsv(results, "results.csv")
    }

    private fun <T : Any> writeCsv(data: List<T>, fileName: String) {
        if (data.isEmpty()) {
            println("No data to write.")
            return
        }

        val kClass = data.first()::class
        val headers = kClass.declaredMemberProperties.map { it.name }

        csvWriter().open(fileName) {
            // Write header
            writeRow(headers)

            // Write rows
            data.forEach { item ->
                val row = kClass.declaredMemberProperties.map { prop ->
                    prop.getter.call(item)?.toString()?.replace(",", "\\,") ?: ""
                }
                writeRow(row)
            }
        }

        println("CSV written successfully to $fileName")
    }


    private fun getTestSet(folderName: String, gen: String): List<String> {

        val classLoader = Thread.currentThread().contextClassLoader
        val fileNames = classLoader.getResource(folderName)?.let { folder ->
            File(folder.toURI()).listFiles()?.map { it.name.split(".")[0] } ?: emptyList()
        } ?: throw IllegalArgumentException("Folder '$folderName' not found!")

        return fileNames.filter {
            when (ConfigProvider.config.testSet) {
                TestSet.ONE -> listOf("eil101-$gen-50")
                TestSet.HARD -> listOf("rd400-$gen-50")
                TestSet.BASE -> listOf(
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

                TestSet.ALL -> fileNames
            }.contains(it)
        }
    }

    private fun aggregateResults(results: List<Result>): AggregatedResultOutput {
        val successfulResults = results.filter { it.successful }
            .ifEmpty { listOf( results.first()) }
        return AggregatedResultOutput(
            name = results.first().name,
            size = successfulResults.let { it.ifEmpty { results } }.first().size,
            budget = successfulResults.let { it.ifEmpty { results } }.first().budget,
            successfulAmount = successfulResults.size,
            revenueMin = successfulResults.minOf { it.revenue },
            revenueAvg = successfulResults.sumOf { it.revenue } / results.size,
            revenueMax = successfulResults.maxOf { it.revenue },
            budgetSpentAvg = successfulResults.sumOf { it.budgetSpent } / results.size,
            timeMin = successfulResults.minOf { it.time },
            timeAvg = successfulResults.sumOf { it.time } / results.size,
            timeMax = successfulResults.maxOf { it.time }
        )
    }

    private fun mapResult(result: Result): ResultOutput{
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
