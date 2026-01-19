package si.um.feri.kaput.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.utils.MqttUtil
import si.um.feri.kaput.utils.SensorUtil
import si.um.feri.kaput.classes.AlarmDataUploader

class DataUploadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as MyApplication
        val mqttClient = app.mqttClient

        val timestamp = System.currentTimeMillis()
        val location = SensorUtil.getLocation()
        val pressure = SensorUtil.getPressure()
        val lng = location["longitude"] ?: 0.0
        val lat = location["latitude"] ?: 0.0

        val data = JSONObject().apply {
            put("timestamp", timestamp)
            put("lat", lat)
            put("lng", lng)
            put("pressure", pressure)
        }.toString()

        MqttUtil.publish(mqttClient, MqttUtil.BLOCKCHAIN_UPLOAD_TOPIC, data)

        AlarmDataUploader.scheduleNext(context)
    }
}
