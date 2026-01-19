package si.um.feri.kaput.models

import kotlinx.serialization.Serializable

@Serializable
data class LocationUser(
    val userId: String,
    val username: String,
    var numbOfTrans: Int,
    var inflow: Double,
    var outflow: Double
)