package si.um.feri.kaput

import android.app.Application
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

import si.um.feri.kaput.utils.HttpUtil
import si.um.feri.kaput.utils.MqttUtil
import si.um.feri.kaput.utils.SettingsUtil
val USER_NAME = BuildConfig.USER_NAME
val PASSWORD = BuildConfig.PASSWORD
val LOG_IN_URL = BuildConfig.LOG_IN_URL

class MyApplication: Application() {
    var data: MutableList<Int> = mutableListOf()
    lateinit var mqttClient: MqttAndroidClient
    lateinit var httpClient: OkHttpClient
    var JWTtoken: String = ""
    override fun onCreate() {
        super.onCreate()

        SettingsUtil.handleUserUUID(this)
        data = mutableListOf(1, 2, 3, 4, 5)

        mqttClient = MqttUtil.buildClient(this, SettingsUtil.getUserUUID(this) ?: System.currentTimeMillis().toString())
        httpClient = HttpUtil.buildClient()
        loginToServer()
    }
    fun loginToServer() {
        val jsonBody: RequestBody = """
            {
                "username": "$USER_NAME",
                "password": "$PASSWORD"
            }
        """.trimIndent().toRequestBody(HttpUtil.JSON)

        HttpUtil.httpPostRequest(
            this.httpClient,
            this,
            LOG_IN_URL,
            jsonBody,
            onSuccess = { response ->
                try {
                    val json = JSONObject(response)
                    val token = json.getString("token")
                    this.JWTtoken = token
                    Log.d("MyApp", "Token: $token")
                } catch (e: Exception) {
                    Log.d("MyApp", "JSON parse error: ${e.message}")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            }
        )
    }
}