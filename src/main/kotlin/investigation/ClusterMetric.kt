package masterthesis.investigation

import java.math.BigDecimal

data class ClusterMetric(
    val _name: String,
    val _id: Int,
    val _x: BigDecimal,
    val _y: BigDecimal,
    val _isStart: Boolean,
    val _isEnd: Boolean,
    val _size: Int,
    val _startToMean: BigDecimal,
    val _endToMean: BigDecimal,
    val _startToEnd: BigDecimal,
    val _meanDistToMean: BigDecimal,
    val _meanDetour: BigDecimal,
    val _potentialRevenue: Int,

    val revenueSum: Int,
    val revenueMean: BigDecimal,
    val budgetSmall: Int,
    val budgetBig: Int,
    val revenueSmall: Int,
    val revenueBig: Int,
    val revenueDif: Int,
    val additionalRevenueToBudget: BigDecimal,
    val meanDetourRelativeToRevenue: BigDecimal
)
