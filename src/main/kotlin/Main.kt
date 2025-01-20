package masterthesis

import masterthesis.solver.config.ConfigProvider
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis


private val logger = LoggerFactory.getLogger("Main")

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    logger.info("Starting application")
    ConfigProvider.loadConfig()
    val timeTaken = measureTimeMillis {
        val metaHandler = MetaHandler()
        metaHandler.runAll()
    }
    logger.info("Application finished in $timeTaken ms")
}