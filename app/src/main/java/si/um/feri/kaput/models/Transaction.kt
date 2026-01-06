package si.um.feri.kaput.models

import java.util.Date
import java.util.UUID

data class Transaction(
    val id: UUID? = null,
    val user: String? = null,
    val location: Location,
    val datetime: Date = Date(),
    val change: Double = 0.0,
    val outgoing: Boolean = true,
)