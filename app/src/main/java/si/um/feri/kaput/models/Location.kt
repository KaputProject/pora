package si.um.feri.kaput.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val _id: String,
    val name: String,
    val inflow: Double? = null,
    val outflow: Double? = null,
    val number_of_transactions: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)
