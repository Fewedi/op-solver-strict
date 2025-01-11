package masterthesis

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import masterthesis.solver.model.Result
import org.slf4j.LoggerFactory
import java.io.File

class MetaHandler {

    private val logger = LoggerFactory.getLogger(MetaHandler::class.java)

    fun runAll() {


        val folderName = "OPLib/instances/gen1" // The folder in your resources

        val classLoader = Thread.currentThread().contextClassLoader

        val fileNames = classLoader.getResource(folderName)?.let { folder ->
            File(folder.toURI()).listFiles()?.map { it.name.split(".")[0] } ?: emptyList()
        } ?: throw IllegalArgumentException("Folder '$folderName' not found!")

        val solver = Solver()

        val baseLines = listOf(
            "eil101-gen1-50",
            "gil262-gen1-50",
            "pr299-gen1-50",
            "lin318-gen1-50",
            "rd400-gen1-50",
            "d493-gen1-50",
            "u574-gen1-50",
            "u724-gen1-50",
            "pcb1173-gen1-50",
            "fl1400-gen1-50",
            "pr2392-gen1-50"
        )

        val results = fileNames.filter { baseLines.contains(it) }.map { fileName ->
            logger.info("Solving $fileName")
            try {
                solver.solve(fileName)
            } catch (e: Exception) {
                logger.error("Failed to solve $fileName", e)
                Result(fileName, 0, emptyList(), 0.0, false, 0, 0.0, 0.0)
            }
        }

        csvWriter().open("results.csv") {
            writeRow(
                listOf(
                    "Name",
                    "Nodes",
                    "Budget",
                    "Successful",
                    "Revenue",
                    "NodesCollected",
                    "BudgetSpent",
                    "Time"
                )
            )

            results.forEach() { result ->
                writeRow(
                    listOf(
                        result.name,
                        result.size,
                        result.budget,
                        result.successful,
                        result.revenue,
                        result.finalPath.size,
                        result.budgetSpent,
                        result.time
                    )
                )
            }
        }

    }


}