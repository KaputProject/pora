package si.um.feri.kaput.utils

import android.content.Context
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import im.delight.android.location.SimpleLocation

object SensorUtil {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var location: SimpleLocation

    fun init(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        location = SimpleLocation(context)
        location.beginUpdates()
    }

    fun getLocation(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        data["latitude"] = location.getLatitude()
        data["longitude"] = location.getLongitude()

        return data
    }

    fun stopLocationUpdates() {
        location.endUpdates()
    }

    fun resumeLocationUpdates() {
        location.beginUpdates()
    }
}