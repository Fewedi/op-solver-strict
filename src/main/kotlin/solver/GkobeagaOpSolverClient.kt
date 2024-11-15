package solver

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import masterthesis.solver.ClusterNode
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter


class GkobeagaOpSolverClient(
    private val problemWriter: ProblemWriter = ProblemWriter()
) {

    private fun runCommand(command: List<String>) {
        val processBuilder: ProcessBuilder = ProcessBuilder(command)
        val process = processBuilder.start()
        process.inputStream.reader(Charsets.UTF_8).use {
            println(it.readText())
        }
        process.waitFor()
    }

    fun solve(pathProblem: String, objectMapper: ObjectMapper): Solution? {
        val pathSolver = ("./src/main/resources/op-solver/build/src/op-solver")

        runCommand(listOf(pathSolver, "opt", "./src/main/resources/$pathProblem"))

        val solutionFile = File("./stats.json")

        val solutions = solutionFile.readLines().map {
            println(it)
            objectMapper.readValue(it, Solution::class.java)
        }
        PrintWriter(solutionFile).apply {
            print("")
            close()
        }
        return solutions.firstOrNull() { it.env == "cp_heur_ea" }
    }

    fun solvse(problem: Problem, objectMapper: ObjectMapper ): Solution {
        val pathSolver = ("./src/main/resources/op-solver/build/src/op-solver")
        val pathProblem = ("./src/main/resources/op-solver/build/OPLib/instances/gen3/kroA150-gen3-50.oplib")

        runCommand(listOf(pathSolver, "opt", pathProblem))

        val solutionFile = File("./stats.json")

        val solutions = solutionFile.readLines().map {
            println(it)
            objectMapper.readValue(it, Solution::class.java)
        }

        PrintWriter(solutionFile).apply {
            print("")
            close()
        }

        return solutions.first { it.env == "cp_heur_ea" }

    }

}
