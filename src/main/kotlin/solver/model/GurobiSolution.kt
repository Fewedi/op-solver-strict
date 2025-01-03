package masterthesis.solver.model

import com.fasterxml.jackson.annotation.JsonProperty

data class GurobiSolution(
    val solutionInfo: SolutionInfo,
    val vars: List<Variable>
)

data class SolutionInfo(
    val status: Long,
    val runtime: Double,
    val work: Double,
    val objVal: Long,
    val objBound: Long,
    val objBoundC: Long,
    @JsonProperty("MIPGap") val value: Double,
    val mipGap: Double,
    val intVio: Long,
    val boundVio: Long,
    val constrVio: Long,
    val iterCount: Long,
    val barIterCount: Long,
    val nodeCount: Long,
    val solCount: Long,
    val poolObjBound: Long,
    val poolObjVal: List<Long>
)

data class Variable(
    val varName: String,
    val x: Long
)

