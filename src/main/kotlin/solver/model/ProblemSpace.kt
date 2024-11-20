package masterthesis.solver.model


data class ProblemSpace(
    val metaData: ProblemMetaData,
    val nodeMap: Map<Int, Node>,
    val distanceMatrix: Array<DoubleArray>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProblemSpace

        if (metaData != other.metaData) return false
        if (nodeMap != other.nodeMap) return false
        if (!distanceMatrix.contentDeepEquals(other.distanceMatrix)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = metaData.hashCode()
        result = 31 * result + nodeMap.hashCode()
        result = 31 * result + distanceMatrix.contentDeepHashCode()
        return result
    }
}
