package si.um.feri.kaput.models
data class Location(
    val _id: String,
    val name: String,
    val inflow: Double? = null,
    val outflow: Double? = null,
    val number_of_transactions: Int? = null,
    val lat: Double?,
    val lng: Double?,
)
