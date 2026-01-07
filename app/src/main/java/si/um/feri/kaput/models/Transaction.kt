package si.um.feri.kaput.models

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,
    val user: String,
    val location: Location,
    val datetime: Long,   // millis since epoch
    val change: Double = 0.0,
    val outgoing: Boolean = true,
)