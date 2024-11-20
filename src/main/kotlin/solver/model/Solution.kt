package masterthesis.solver.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Solution(
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
        val init: Int? = null,        // Nullable for SolutionEvolution
        val select: Int? = null,      // Nullable for SolutionEvolution
        val pinit: Int? = null,       // Nullable for SolutionEvolution
        val itLim: Int? = null,       // Nullable for SolutionInit
        val popSize: Int? = null,     // Nullable for SolutionInit
        val popStop: Int? = null,     // Nullable for SolutionInit
        val d2d: Int? = null,         // Nullable for SolutionInit
        val nparsel: Int? = null,     // Nullable for SolutionInit
        val pmut: Double? = null,     // Nullable for SolutionInit
        val lenImprove1: Int? = null, // Nullable for SolutionInit
        val lenImprove2: Int? = null  // Nullable for SolutionInit
    )

    data class Stats(
        val time: Int,
        val it: Int? = null,                   // Nullable for SolutionInit
        val timeInfeasRecover: Int? = null     // Nullable for SolutionInit
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



