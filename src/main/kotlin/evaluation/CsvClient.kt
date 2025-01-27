package masterthesis.evaluation

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.full.declaredMemberProperties

class CsvClient {



    fun <T : Any> writeCsv(data: List<T>, fileName: String) {
        if (data.isEmpty()) {
            println("No data to write.")
            return
        }

        val kClass = data.first()::class
        val headers = kClass.declaredMemberProperties.map { it.name }

        csvWriter().open(fileName) {
            writeRow(headers)

            data.forEach { item ->
                val row = kClass.declaredMemberProperties.map { prop ->
                    prop.getter.call(item)?.toString()?.replace(",", "\\,") ?: ""
                }
                writeRow(row)
            }
        }

        println("CSV written successfully to $fileName")
    }

    fun writeCsvParamBased(data: List<ParameterSearchResult>, fileName: String, valueList: List<Double>) {
        if (data.isEmpty()) {
            println("No data to write.")
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

        println("CSV written successfully to $fileName")
    }

}