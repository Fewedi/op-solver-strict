package masterthesis.solver.legacy

import com.fasterxml.jackson.annotation.JsonProperty

data class GkobeagaSolution(
    val prob: Prob,
    val sol: Sol,
    val param: Param,
    val stats: Stats,
    val timestamp: Long,
    val event: String,
    val env: String,
    val seed: Int,
    val pid: Int
) {
    data class Param(
        val timeLimit: Long,
        val init: Int? = null,
        val select: Int? = null,
        val pinit: Int? = null,
        val itLim: Int? = null,
        val popSize: Int? = null,
        val popStop: Int? = null,
        val d2d: Int? = null,
        val nparsel: Int? = null,
        val pmut: Double? = null,
        val lenImprove1: Int? = null,
        val lenImprove2: Int? = null
    )

    data class Stats(
        val time: Int,
        val it: Int? = null,
        val timeInfeasRecover: Int? = null
    )

    data class Prob(
        val name: String,
        val n: Int,
        val d0: Int
    )

    data class Sol(
        @JsonProperty("val") val value: Int,
        val cap: Int,
        val solNs: Int,
        val lb: Int,
        val ub: Double,
        val cycle: MutableList<Int>
    )
}



