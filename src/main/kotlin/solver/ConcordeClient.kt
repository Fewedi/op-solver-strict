package masterthesis.solver

import masterthesis.solver.model.Cluster
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class ConcordeClient {

    fun solve(clusterList: List<Cluster>, startCluster: Cluster): List<Cluster> {
        val startClusterIndex = clusterList.indexOf(startCluster)
        val preparedList =
            listOf(clusterList[startClusterIndex]) + clusterList.filterIndexed { index, _ -> index != startClusterIndex }

        val costM = Array(clusterList.size) { DoubleArray(clusterList.size) { 0.0 } }

        for (i in preparedList.indices) {
            for (j in preparedList.indices) {
                if (j == 0) {
                    continue
                }
                costM[i][j] = preparedList[i].distanceTo(preparedList[j])
            }
        }

        File("TSP_problem").bufferedWriter().use { writer ->
            writer.write("NAME : TSP_problem\n")
            writer.write("TYPE : TSP\n")
            writer.write("COMMENT : A new TSP problem\n")
            writer.write("DIMENSION : ${preparedList.size}\n")
            writer.write("EDGE_WEIGHT_TYPE : EXPLICIT\n")
            writer.write("EDGE_WEIGHT_FORMAT : FULL_MATRIX\n")
            writer.write("EDGE_WEIGHT_SECTION:\n")
            for (row in costM) {
                writer.write(row.joinToString(" ") { it.toInt().toString() } + "\n")
            }
        }

        val currentPath = Paths.get("").toAbsolutePath().toString()
        val process = ProcessBuilder("./src/main/resources/concorde/TSP/concorde", "TSP_problem")
            .redirectErrorStream(true)
            .start()
        process.waitFor()

        val filePath = Paths.get("$currentPath/TSP_problem.sol")
        val fileContent = Files.readString(filePath)

        //5
        //0 4 3 2 1 6 11 10 9 8
        //12 7 5
        return fileContent
            .lines()
            .subList(1, fileContent.lines().size - 1)
            .map { it.trim().split(" ") }
            .flatten()
            .map { preparedList[it.toInt()] }
    }
}
