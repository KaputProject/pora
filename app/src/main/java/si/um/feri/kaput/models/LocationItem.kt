package si.um.feri.kaput.models
data class LocationItem(
    val _id: String,
    val name: String,
    val inflow: Double,
    val outflow: Double,
    val number_of_transactions: Int
)
