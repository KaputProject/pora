package si.um.feri.kaput.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import im.delight.android.location.SimpleLocation

object SensorUtil {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var location: SimpleLocation

    private var sensorManager: SensorManager? = null
    private var pressure: Float? = null

    fun init(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        location = SimpleLocation(context)
        location.beginUpdates()

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val barometer = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)
        barometer?.let {
            sensorManager?.registerListener(object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    pressure = event.values[0]
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun getLocation(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        data["latitude"] = location.getLatitude()
        data["longitude"] = location.getLongitude()

        return data
    }

    fun getPressure(): Float? {
        return pressure
    }

    fun stopLocationUpdates() {
        location.endUpdates()
    }

    fun resumeLocationUpdates() {
        location.beginUpdates()
    }
}