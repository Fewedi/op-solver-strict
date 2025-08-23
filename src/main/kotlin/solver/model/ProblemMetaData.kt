package masterthesis.solver.model

data class ProblemMetaData(val path: String) {
    lateinit var name: String
    lateinit var comment: String
    lateinit var type: String
    lateinit var dimension: String
    var costLimit: Double = -1.0
    lateinit var edgeWeightType: String
    lateinit var startNode: Node
}