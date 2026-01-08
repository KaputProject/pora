package si.um.feri.kaput

import android.app.Application
import im.delight.android.location.SimpleLocation
import info.mqtt.android.service.MqttAndroidClient
import okhttp3.OkHttpClient
import si.um.feri.kaput.managers.DataManager
import si.um.feri.kaput.utils.DatabaseUtil
import si.um.feri.kaput.utils.HttpUtil
import si.um.feri.kaput.utils.MqttUtil
import si.um.feri.kaput.utils.SensorUtil
import si.um.feri.kaput.utils.SettingsUtil

class MyApplication : Application() {
    lateinit var mqttClient: MqttAndroidClient
    lateinit var httpClient: OkHttpClient
    lateinit var databaseUtil: DatabaseUtil
    lateinit var dataManager: DataManager

    override fun onCreate() {
        super.onCreate()

        SettingsUtil.handleUserUUID(this)

        mqttClient = MqttUtil.buildClient(
            this, SettingsUtil.getUserUUID(this) ?: System.currentTimeMillis().toString()
        )
        httpClient = HttpUtil.buildClient()
        // Initialize DataManager
        dataManager = DataManager() // access to data app.dataManager.someMethod()/property

        databaseUtil = DatabaseUtil(httpClient, this,dataManager)

        databaseUtil.loginToServer()
    }
}