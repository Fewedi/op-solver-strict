package masterthesis.evaluation

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import masterthesis.evaluation.model.ParameterSearchResult
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.reflect.full.declaredMemberProperties

class CsvClient {

    private val logger = LoggerFactory.getLogger(CsvClient::class.java)

    private fun ensureFolderExists(relativePath: String) {
        val currentDir = System.getProperty("user.dir")
        val folder = File(currentDir, relativePath)

        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    private fun ensureFileExists(fileName: String): Boolean {
        val currentDir = System.getProperty("user.dir")
        val file = File(currentDir, fileName)

        if (!file.exists()) {
            file.createNewFile()
            return true
        }
        return false
    }

    fun writeComparingCsv(headers: List<String>, instances: List<String>, revenues: List<List<Int>>, bestInstance: List<String>, relativePath: String) {
        ensureFolderExists(relativePath)

        csvWriter().open("$relativePath/comparison.csv") {
            writeRow(headers)
            instances.forEachIndexed{index, instance ->
                writeRow(listOf(instance) + revenues[index].map { it.toString() } + listOf(bestInstance[index]))
            }
        }

        logger.info("CSV written successfully to $relativePath/comparison.csv")
    }

    fun <T : Any> writeCsv(data: List<T>, fileName: String, relativePath: String) {
        if (data.isEmpty()) {
            logger.error("No data to write.")
            return
        }

        ensureFolderExists(relativePath)

        val kClass = data.first()::class
        val headers = kClass.declaredMemberProperties.map { it.name }

        csvWriter().open("$relativePath$fileName") {
            writeRow(headers)

            data.forEach { item ->
                val row = kClass.declaredMemberProperties.map { prop ->
                    prop.getter.call(item)?.toString()?.replace(",", "\\,") ?: ""
                }
                writeRow(row)
            }
        }

        logger.info("CSV written successfully to $relativePath$fileName")
    }

    fun writeCsvParamBased(data: List<ParameterSearchResult>, fileName: String, valueList: List<Double>) {
        if (data.isEmpty()) {
            logger.error("No data to write.")
            return
        }

        val headers = listOf("name", "best") + valueList.map { BigDecimal(it).setScale(2, RoundingMode.HALF_UP).toString()}

        csvWriter().open(fileName) {
            writeRow(headers)
            data.forEach { item ->
                val best = item.values.max().let {
                    val index = item.values.indexOf(it)
                    if (index == -1) {
                        "ERROR"
                    } else {
                        BigDecimal(valueList[index]).setScale(2, RoundingMode.HALF_UP).toString()
                    }
                }
                val row = listOf(item.name, best) + item.values.map { BigDecimal(it).setScale(2, RoundingMode.HALF_UP).toString() }
                writeRow(row)
            }
        }

        logger.info("CSV written successfully to $fileName")
    }

    fun writeLineToCsv(data: Any, instance: String) {

        val fileName = instance.split("-")[0] + ".csv"
        val relativePath = "results/meta"
        ensureFolderExists(relativePath)
        val relativeFilePath = "$relativePath/$fileName"
        val isNew = ensureFileExists(relativeFilePath)

        val kClass = data::class
        val headers = kClass.declaredMemberProperties.map { it.name }

        csvWriter().open(relativeFilePath, append = true) {

            if (isNew) {
                writeRow(headers)
            }

            data.let { item ->
                val row = kClass.declaredMemberProperties.map { prop ->
                    prop.getter.call(item)?.toString()?.replace(",", "\\,") ?: ""
                }
                writeRow(row)
            }
        }
    }

}