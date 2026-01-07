package si.um.feri.kaput.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

object SensorUtil {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun init(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun getLocation(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                data["latitude"] = it.latitude
                data["longitude"] = it.longitude
            }
        }

        Log.d("SensorUtil", "Location data retrieved: $data")

        return data
    }
}