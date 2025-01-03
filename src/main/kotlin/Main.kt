package masterthesis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import masterthesis.solver.GurobiClient
import masterthesis.solver.config.ConfigProvider
import org.slf4j.LoggerFactory
import solver.ProblemParser
import kotlin.system.measureTimeMillis


private val logger = LoggerFactory.getLogger(ProblemParser::class.java)
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    logger.info("Starting application")
    ConfigProvider.loadConfig()
    val timeTaken = measureTimeMillis {
        //val solver = GurobiClient()
        val solver = Solver()
        val objectMapper = jacksonObjectMapper().apply {
            setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.UPPER_CAMEL_CASE)
        }
        solver.solve()
    }
    logger.info("Application finished in $timeTaken ms")
}