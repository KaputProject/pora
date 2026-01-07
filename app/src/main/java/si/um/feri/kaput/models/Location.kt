package si.um.feri.kaput.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val _id: String,
    val identifier: String? = null,
    val name: String? = null,
    val inflow: Double? = null,
    val outflow: Double? = null,
    val total_inflow: Double? = null,
    val total_outflow: Double? = null,
    val number_of_transactions: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val userId: List<String>? = null,
    val address: String? = null,
    val users: List<LocationUser>? = null
)

@Serializable
data class LocationUser(
    val userId: String,
    val username: String,
    val numbOfTrans: Int,
    val inflow: Double,
    val outflow: Double
)