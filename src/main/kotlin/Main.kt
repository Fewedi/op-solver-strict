package masterthesis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import masterthesis.solver.Clustering
import masterthesis.solver.TSPForCluster
import solver.GkobeagaOpSolverClient
import solver.ProblemParser
import solver.ProblemWriter
import solver.Visualizer

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val name = "Kotlin"
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    println("Hello, " + name + "!")

    val problemParser = ProblemParser()
    val problemWriter = ProblemWriter()
    val objectMapper = jacksonObjectMapper()
    val gkobeagaOpSolverClient = GkobeagaOpSolverClient()
    val clustering = Clustering()
    val tSPForCluster = TSPForCluster()
    val visualizer = Visualizer()


    val problem = problemParser.readProblem()
    val clusterMap = clustering.cluster(problem)
    val clusterPath = tSPForCluster.provideClusterPath(clusterMap, problem)
    val solutions = clusterPath.vertexList.map { cluster ->
        val path = "a280-gen1-50-cluster-${cluster.cluster}.oplib"
        try {
            problemWriter.writeCluster(
                problem,
                cluster,
                "src/main/resources/$path",
                problem.costLimit.toInt() / clusterPath.vertexList.size
            )
        }catch (e: Exception) {
            println("Error writing cluster: ${e.message}")
            println("Stacktrace: ${e.stackTrace}")
            throw e
        }
        gkobeagaOpSolverClient.solve(path, objectMapper).let {
            it?.let { solution ->
                problemParser.addSolutionToProblem(problem, solution, cluster.cluster)
                solution
            }
        }

    }

    visualizer.plotPaths(problem)

    println(solutions)

 //   problemParser.addSolutionToProblem(problem, solution)
 //   //val solution = gkobeagaOpSolverClient.solve(problem, objectMapper)
 //   val visualizer = Visualizer()
 //   visualizer.plotGraph(problem)


}