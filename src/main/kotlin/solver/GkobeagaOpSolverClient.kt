package solver

import com.fasterxml.jackson.databind.ObjectMapper
import masterthesis.solver.model.Solution
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter


class GkobeagaOpSolverClient {

    private val logger = LoggerFactory.getLogger(GkobeagaOpSolverClient::class.java)

    private fun runCommand(command: List<String>) {
        val processBuilder = ProcessBuilder(command)
        val process = processBuilder.start()
        process.inputStream.reader(Charsets.UTF_8)
        process.waitFor()
    }

    fun solve(pathProblem: String, objectMapper: ObjectMapper): Solution? {
        val pathSolver = ("./src/main/resources/op-solver/build/src/op-solver")

        runCommand(listOf(pathSolver, "opt", pathProblem))

        val solutionFile = File("./stats.json")

        val solutions = solutionFile.readLines().map {
            objectMapper.readValue(it, Solution::class.java)
        }
        PrintWriter(solutionFile).apply {
            this.print("")
            close()
        }
        val solution = solutions.firstOrNull { it.env == "cp_heur_ea" }
        if (solution == null) {
            logger.error("No solution found for problem $pathProblem")
        } else {
            logger.info("Solution found for problem $pathProblem with path ${solution.sol.cycle}")
        }
        return solution
    }

}
