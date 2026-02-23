package masterthesis.solver

import masterthesis.config.ConfigProvider
import masterthesis.config.Symmetric
import masterthesis.solver.model.Cluster
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.collections.flatten

class ConcordeClient {

    val logger = org.slf4j.LoggerFactory.getLogger(ConcordeClient::class.java)

    private fun makeSymmetric(M: Array<DoubleArray>): Array<DoubleArray> {
        val e = 0.01
        val INT_MAX = M.map{it.sum()}.max() * 10.0
        val n = M.size

        fun getDmin(matrix: Array<DoubleArray>): Double =
            matrix.flatMap { it.asList() }.filter { it != 0.0 }.minOrNull() ?: 0.0

        fun getDmax(matrix: Array<DoubleArray>): Double =
            matrix.flatMap { it.asList() }.maxOrNull() ?: 0.0

        val dmin = getDmin(M)
        val dmax = getDmax(M)

        val D = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                D[i][j] = if (i == j) {
                    0.0
                } else if (4 * dmin - 3 * dmax > 0) {
                    M[i][j]
                } else {
                    M[i][j] + 3 * dmax - 4 * dmin + e
                }
            }
        }

        val D_T = Array(n) { i -> DoubleArray(n) { j -> D[j][i] } }

        val D_symm = Array(2 * n) { DoubleArray(2 * n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                D_symm[i][j] = INT_MAX
            }
        }
        for (i in 0 until n) {
            for (j in n until 2 * n) {
                D_symm[i][j] = D_T[i][j - n]
            }
        }
        for (i in n until 2 * n) {
            for (j in 0 until n) {
                D_symm[i][j] = D[i - n][j]
            }
        }
        for (i in n until 2 * n) {
            for (j in n until 2 * n) {
                D_symm[i][j] = INT_MAX
            }
        }
        for (i in 0 until n) {
            D_symm[i][n] = 0.0
            D_symm[n][i] = 0.0
        }

        fun isSymmetric(matrix: Array<DoubleArray>): Boolean {
            for (i in matrix.indices) {
                for (j in matrix.indices) {
                    if (matrix[i][j] != matrix[j][i]) return false
                }
            }
            return true
        }

        require(isSymmetric(D_symm))
        return D_symm
    }

    fun solve(clusterList: List<Cluster>, startCluster: Cluster): List<Cluster> {
        val startClusterIndex = clusterList.indexOf(startCluster)
        val preparedList =
            listOf(clusterList[startClusterIndex]) + clusterList.filterIndexed { index, _ -> index != startClusterIndex }

        val asymetricCostMatrix = Array(clusterList.size) { DoubleArray(clusterList.size) { 0.0 } }

        for (i in preparedList.indices) {
            for (j in preparedList.indices) {
                asymetricCostMatrix[i][j] = preparedList[i].distanceTo(preparedList[j])
            }
        }

        val costM = if (ConfigProvider.config.algorithm.tspCostMatrix == Symmetric.MAKESYMMETRIC) {
            makeSymmetric(asymetricCostMatrix)
        } else {
            asymetricCostMatrix
        }

        File("TSP_problem").bufferedWriter().use { writer ->
            writer.write("NAME : TSP_problem\n")
            writer.write("TYPE : TSP\n")
            writer.write("COMMENT : A new TSP problem\n")
            writer.write("DIMENSION : ${costM.size}\n")
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

        return if (ConfigProvider.config.algorithm.tspCostMatrix == Symmetric.MAKESYMMETRIC) {
            val clusterPath = fileContent
                .lines()
                .subList(1, fileContent.lines().size - 1)
                .map { it.trim().split(" ") }
                .flatten()
                .filterIndexed { index, _ -> index % 2 == 0 }
                .map { it.toInt() }
                .filter { it < preparedList.size }
                .map { preparedList[it.toInt()] }
            val startIndex = clusterPath.first()
            val others = clusterPath.subList(1, clusterPath.size)
            val distToStart = startIndex.distanceTo(others.first())
            val distToEnd = startIndex.distanceTo(others.last())
            if (distToStart < distToEnd) {
                logger.info("Start node is closer to the first node in the path AS EXPECTED")
                clusterPath
            } else {
                logger.info("Start node is closer to the last node in the path, reversing the path")
                listOf(startIndex) + others.reversed()
            }
        } else {
            fileContent
                .lines()
                .subList(1, fileContent.lines().size - 1)
                .map { it.trim().split(" ") }
                .flatten()
                .map { preparedList[it.toInt()] }
        }
    }
}
