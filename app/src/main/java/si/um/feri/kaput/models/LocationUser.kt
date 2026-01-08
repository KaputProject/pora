package si.um.feri.kaput.models

import kotlinx.serialization.Serializable

@Serializable
data class LocationUser(
    val userId: String,
    val username: String,
    val numbOfTrans: Int,
    val inflow: Double,
    val outflow: Double
)