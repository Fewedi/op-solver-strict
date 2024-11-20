package masterthesis.solver

import org.slf4j.LoggerFactory
import java.io.File

class CleanupService {
    private val logger = LoggerFactory.getLogger(CleanupService::class.java)
    fun cleanUp() {
        cleanupFilesWithExtension("./src/main/resources", ".oplib")
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