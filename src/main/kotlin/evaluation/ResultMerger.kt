package masterthesis.evaluation

import masterthesis.config.ConfigProvider
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.api.mapIndexed
import org.jetbrains.kotlinx.dataframe.io.readCSV
import java.io.File

class ResultMerger {


    private fun getFilesOfFolder(): List<File> {
        val currentDir = System.getProperty("user.dir")
        val folder = File(currentDir, ConfigProvider.config.analysis.resultPath)
        return folder.listFiles()
            ?.filter { it.isFile && it.name != "comparison.csv"}
            ?: emptyList()
    }

    private fun findDifferingSubstring(strings: List<String>): List<String> {
        val split = strings.map { it.split("_") }
        val transposed = split[0].indices.map { i -> split.map { it[i] } }

        val differingIndices = transposed
            .mapIndexedNotNull { index, parts ->
                if (parts.distinct().size > 1) index else null
            }

        return split.map { parts ->
            differingIndices.joinToString("_") { parts[it] }
        }
    }

    fun mergeResults(csvClient: CsvClient) {
        val files = getFilesOfFolder()
        val fileNames = findDifferingSubstring(files.map { it.nameWithoutExtension })

        val results = files.mapIndexed { index, file ->
            fileNames[index] to DataFrame.readCSV(file)
        }
        val instances = results.first().second["name"].map { it.toString() }

        val bestAvgValues = instances.mapIndexed { index, _ ->
            val avgValues = results.map { it.second["revenueAvg"][index] as Int }
            val best = avgValues.maxOrNull()
            val bestIndex = avgValues.indexOf(best)
            results[bestIndex].first
        }
        val avgColumns = results.map {it.first to it.second["revenueAvg"] }

        csvClient.writeComparingCsv(
            headers = listOf("instance") + fileNames + listOf("best"),
            instances = instances.toList(),
            revenues = transpose(avgColumns.map { it.second.toList().map { value -> value as Int} }),
            bestInstance = bestAvgValues.toList(),
            relativePath = ConfigProvider.config.analysis.resultPath
        )
    }

    inline fun <reified T> transpose(xs: List<List<T>>): List<List<T>> {
        val cols = xs[0].size
        val rows = xs.size
        return List(cols) { j ->
            List(rows) { i ->
                xs[i][j]
            }
        }
    }
}