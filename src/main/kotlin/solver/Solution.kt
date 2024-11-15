package solver

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
        val time_limit: Long,
        val init: Int? = null,        // Nullable for SolutionEvolution
        val select: Int? = null,      // Nullable for SolutionEvolution
        val pinit: Int? = null,       // Nullable for SolutionEvolution
        val it_lim: Int? = null,       // Nullable for SolutionInit
        val pop_size: Int? = null,     // Nullable for SolutionInit
        val pop_stop: Int? = null,     // Nullable for SolutionInit
        val d2d: Int? = null,         // Nullable for SolutionInit
        val nparsel: Int? = null,     // Nullable for SolutionInit
        val pmut: Double? = null,     // Nullable for SolutionInit
        val len_improve1: Int? = null, // Nullable for SolutionInit
        val len_improve2: Int? = null  // Nullable for SolutionInit
    )

    data class Stats(
        val time: Int,
        val it: Int? = null,                   // Nullable for SolutionInit
        val time_infeas_recover: Int? = null     // Nullable for SolutionInit
    )

    data class Prob(
        val name: String,
        val n: Int,
        val d0: Int
    )

    data class Sol(
        @JsonProperty("val") val value: Int,        // "val" is a reserved keyword in Kotlin, so using "val_"
        val cap: Int,
        val sol_ns: Int,
        val lb: Int,
        val ub: Double,
        val cycle: MutableList<Int>
    )
}



