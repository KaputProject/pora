package si.um.feri.kaput

import android.app.Application
import info.mqtt.android.service.MqttAndroidClient
import okhttp3.OkHttpClient
import si.um.feri.kaput.utils.HttpUtil
import si.um.feri.kaput.utils.MqttUtil
import si.um.feri.kaput.utils.SettingsUtil

class MyApplication: Application() {
    var data: MutableList<Int> = mutableListOf()
    lateinit var mqttClient: MqttAndroidClient
    lateinit var httpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()

        SettingsUtil.handleUserUUID(this)
        data = mutableListOf(1, 2, 3, 4, 5)

        mqttClient = MqttUtil.buildClient(this, SettingsUtil.getUserUUID(this) ?: System.currentTimeMillis().toString())
        httpClient = HttpUtil.buildClient()
    }
}