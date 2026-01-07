package si.um.feri.kaput.models

import kotlinx.serialization.Serializable
@Serializable
data class LocationResponse(
    val locations: List<Location>
)