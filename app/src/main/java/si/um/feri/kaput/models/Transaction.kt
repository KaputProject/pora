package si.um.feri.kaput.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Transaction(
    val _id: String = UUID.randomUUID().toString(),
    val location: Location,
    val datetime: Long,
)