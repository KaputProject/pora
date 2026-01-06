package si.um.feri.kaput

import android.app.Application
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.eclipse.paho.client.mqttv3.internal.Token
import org.json.JSONObject

import si.um.feri.kaput.utils.HttpUtil
import si.um.feri.kaput.utils.MqttUtil
import si.um.feri.kaput.utils.SettingsUtil
val USER_NAME = BuildConfig.USER_NAME
val PASSWORD = BuildConfig.PASSWORD
val LOG_IN_URL = BuildConfig.LOG_IN_URL

val FAMILY_URL = "http://10.0.2.2:5000/family"

val USER_URL = "http://10.0.2.2:5000/users"

class MyApplication: Application() {
    var data: MutableList<Int> = mutableListOf()
    lateinit var mqttClient: MqttAndroidClient
    lateinit var httpClient: OkHttpClient
    var JWTtoken: String = ""
    var userId: String = ""
    var familyId: String = ""

    var dataSetOne: JSONObject = JSONObject()
    var dataSetTwo: JSONObject = JSONObject()

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
                    val id = if (json.has("user") && json.get("user") is JSONObject) {
                        json.getJSONObject("user").optString("_id", "")
                    } else {
                        json.optString("_id", "")
                    }
                    this.JWTtoken = token
                    this.userId = id
                    Log.d("MyApp", "Token: $token")
                    Log.d("MyApp", "user id: $id")
                    if(this.JWTtoken.isNotEmpty()){
                         getFamilyId()
                    }
                } catch (e: Exception) {
                    Log.d("MyApp", "JSON parse error: ${e.message}")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            }
        )
    }
    fun getFamilyId() {
        val headers = if (JWTtoken.isNotEmpty()) {
            mapOf("Authorization" to "Bearer $JWTtoken")
        } else emptyMap()

        HttpUtil.httpGetRequest(
            this.httpClient,
            this,
            FAMILY_URL,
            headers = headers,
            onSuccess = { response ->
                try {
                    val json = JSONObject(response)
                    val id = if (json.has("family") && json.get("family") is JSONObject) {
                        json.getJSONObject("family").optString("_id", "")
                    } else {
                        json.optString("_id", "")
                    }
                    this.familyId = id
                    getDataSetOne()
                    getDataSetTwo()
                    Log.d("MyApp", "Family ID: $id")
                } catch (e: Exception) {
                    Log.d("MyApp", "JSON parse error: ${e.message}")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            }
        )
    }

    fun getDataSetOne() {
        val headers = if (JWTtoken.isNotEmpty()) {
            mapOf("Authorization" to "Bearer $JWTtoken")
        } else emptyMap()

        HttpUtil.httpGetRequest(
            this.httpClient,
            this,
            FAMILY_URL + "/" + this.familyId + "/statistics",
            headers = headers,
            onSuccess = { response ->
                try {
                    Log.d("MyApp", "DataSetOne response: $response")
                    this.dataSetOne = JSONObject(response)
                } catch (e: Exception) {
                    Log.d("MyApp", "DataSetOne response: $response")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            }
        )
    }

    fun getDataSetTwo() {
        val headers = if (JWTtoken.isNotEmpty()) {
            mapOf("Authorization" to "Bearer $JWTtoken")
        } else emptyMap()

        HttpUtil.httpGetRequest(
            this.httpClient,
            this,
            USER_URL + "/" + this.userId + "/statistics",
            headers = headers,
            onSuccess = { response ->
                try {
                    this.dataSetTwo = JSONObject(response)
                    Log.d("MyApp", "DataSetTwo response: $response")
                } catch (e: Exception) {
                    Log.d("MyApp", "DataSetTwo response: $response")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            }
        )
    }
}