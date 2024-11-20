package masterthesis.solver.model

data class ProblemMetaData(val path: String) {
    lateinit var name: String
    lateinit var comment: String
    lateinit var type: String
    lateinit var dimension: String
    lateinit var costLimit: String
    lateinit var edgeWeightType: String
}