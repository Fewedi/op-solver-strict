package masterthesis.solver.legacy

import masterthesis.DataCapturing
import org.slf4j.LoggerFactory
import java.io.File

class CleanupService {
    private val logger = LoggerFactory.getLogger(CleanupService::class.java)
    fun cleanUp() {
        cleanupFilesWithExtension("./src/main/resources", ".oplib")
        cleanupFilesWithExtension("./", "TSP_problem")
        cleanupFilesWithExtension("./", "TSP_problem.sol")
    }

    fun finalCleanUp() {
        cleanUp()
        cleanupFoldersInFolder("./lets-plot-images")
        DataCapturing.finalCleanup()
    }

    private fun cleanupFoldersInFolder(directoryPath: String) {
        val directory = File(directoryPath)

        if (directory.exists() && directory.isDirectory) {
            val foldersToDelete = directory.listFiles { file ->
                file.isDirectory
            }

            foldersToDelete?.forEach { folder ->
                if (folder.deleteRecursively()) {
                    logger.info("Deleted: ${folder.name}")
                } else {
                    logger.error("Failed to delete: ${folder.name}")
                }
            }
        } else {
            logger.error("Invalid directory: $directoryPath")
        }
    }


    private fun cleanupFilesWithExtension(directoryPath: String, fileExtension: String) {

        val directory = File(directoryPath)

        if (directory.exists() && directory.isDirectory) {
            val filesToDelete = directory.listFiles { _, name ->
                name.endsWith(fileExtension, ignoreCase = true)
            }

            filesToDelete?.forEach { file ->
                if (file.delete()) {
                    logger.info("Deleted: ${file.name}")
                } else {
                    logger.error("Failed to delete: ${file.name}")
                }
            }
        } else {
            logger.error("Invalid directory: $directoryPath")
        }
    }

    fun clearFile(path: String) {
        val file = File(path)
        file.writeText("")
    }
}