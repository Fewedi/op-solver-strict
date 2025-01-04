package masterthesis.solver.model

import com.fasterxml.jackson.annotation.JsonProperty

data class GurobiSolution(
    val solutionInfo: SolutionInfo,
    val vars: List<Variable>
)

data class SolutionInfo(
    val status: Int,
    val runtime: Double,
    val work: Double,
    val objVal: Int,
    val objBound: Int,
    val objBoundC: Int,
    @JsonProperty("MIPGap") val value: Double,
    val mipGap: Double,
    val intVio: Int,
    val boundVio: Int,
    val constrVio: Int,
    val iterCount: Int,
    val barIterCount: Int,
    val nodeCount: Int,
    val solCount: Int,
    val poolObjBound: Int,
    val poolObjVal: List<Int>
)

data class Variable(
    val varName: String,
    val x: Int
)

