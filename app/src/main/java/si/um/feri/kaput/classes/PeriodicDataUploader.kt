package si.um.feri.kaput.classes

import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import si.um.feri.kaput.utils.MqttUtil
import si.um.feri.kaput.utils.SensorUtil

class PeriodicDataUploader(
    private val mqttClient: MqttAndroidClient
) {
    private val tag = "blockchain/upload"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        scope.launch {
            while (true) {
                uploadDataToBlockchain()
                delay(6000)
            }
        }
    }

    private fun uploadDataToBlockchain() {
        val timestamp = System.currentTimeMillis()
        val location = SensorUtil.getLocation()
        val lng = location["longitude"] ?: 0.0
        val lat = location["latitude"] ?: 0.0

        val data = JSONObject().apply {
            put("timestamp", timestamp)
            put("lat", lat)
            put("lng", lng)
        }.toString()

        Log.d(tag, "Uploading data to blockchain: $data")
        MqttUtil.publish(mqttClient, MqttUtil.BLOCKCHAIN_UPLOAD_TOPIC, data)
    }

    fun onDestroy() {
        scope.cancel()
    }
}
