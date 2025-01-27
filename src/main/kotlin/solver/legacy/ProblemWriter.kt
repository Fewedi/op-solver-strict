package masterthesis.solver.legacy

import masterthesis.solver.model.Cluster
import masterthesis.solver.model.ProblemMetaData

fun interface ProblemWriter {

    fun writeCluster(
        metadata: ProblemMetaData,
        distanceMatrix: Array<DoubleArray>,
        cluster: Cluster,
        path: String,
        costLimit: Double
    )
}