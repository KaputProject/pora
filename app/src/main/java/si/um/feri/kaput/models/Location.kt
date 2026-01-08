package si.um.feri.kaput.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val _id: String,
    val name: String? = null,
    val identifier: String? = null,
    val address: String? = null,
    var numbOfTrans: Int? = null,
    var total_inflow: Double? = null,
    var total_outflow: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    var users: List<LocationUser>? = null
)

