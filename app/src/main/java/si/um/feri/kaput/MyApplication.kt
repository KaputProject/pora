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


class MyApplication : Application() {
    var data: MutableList<Int> = mutableListOf()
    lateinit var mqttClient: MqttAndroidClient
    lateinit var httpClient: OkHttpClient

    // storign data after login in the app instance Accessible from other activities and fragments // will be refreshed on each app start
    var JWTtoken: String = ""
    var userId: String = ""
    var familyId: String = ""
    var familyDataSet: JSONObject = JSONObject()
    var UserDataSet: JSONObject = JSONObject()

    override fun onCreate() {
        super.onCreate()

        SettingsUtil.handleUserUUID(this)
        data = mutableListOf(1, 2, 3, 4, 5)

        mqttClient = MqttUtil.buildClient(
            this, SettingsUtil.getUserUUID(this) ?: System.currentTimeMillis().toString()
        )
        httpClient = HttpUtil.buildClient()
        // log in to server to get JWT token. Further requests done after successful login in log in function.
        loginToServer()
    }

    /**
     * Used to log in to the server and retrieve JWT token.
     */
    fun loginToServer() {
        val jsonBody: RequestBody = """
                    {
                        "username": "${BuildConfig.USER_NAME}",
                        "password": "${BuildConfig.PASSWORD}"
                    }
                """.trimIndent().toRequestBody(HttpUtil.JSON)

        HttpUtil.httpPostRequest(
            this.httpClient,
            this,
            BuildConfig.USER_URL + "/login",
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
                    if (this.JWTtoken.isNotEmpty()) {
                        // after successful login, get family ID and user dataset.
                        getFamilyId()  // family dataset will be fetched after family ID is known. in GetFamilyId function
                        getUserDataSet()
                    }
                } catch (e: Exception) {
                    Log.d("MyApp", "JSON parse error: ${e.message}")
                }
            },
            onFailure = {
                Log.d("MyApp", "log in failed: " + it)
            })
    }

    /**
     * gets the family ID of the logged in user if available.
     */
    fun getFamilyId() {
        val headers = if (JWTtoken.isNotEmpty()) {
            mapOf("Authorization" to "Bearer $JWTtoken")
        } else emptyMap()

        HttpUtil.httpGetRequest(
            this.httpClient,
            this,
            BuildConfig.FAMILY_URL,
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
                    if (this.familyId.isNotEmpty()) {
                        // after family ID is known, get the family dataset.
                        getFamilyDataSet()
                    }
                    Log.d("MyApp", "Family ID: $id")
                } catch (e: Exception) {
                    Log.d("MyApp", "JSON parse error: ${e.message}")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            })
    }

    /**
     * if familzy ID is known, gets the family dataset from the server.
     */
    fun getFamilyDataSet() {
        val headers = if (JWTtoken.isNotEmpty()) {
            mapOf("Authorization" to "Bearer $JWTtoken")
        } else emptyMap()

        HttpUtil.httpGetRequest(
            this.httpClient,
            this,
            BuildConfig.FAMILY_URL + "/" + this.familyId + "/statistics",
            headers = headers,
            onSuccess = { response ->
                try {
                    this.familyDataSet = JSONObject(response)

                    val dir = java.io.File(filesDir, "testJSONfiles")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = java.io.File(dir, "family_statistics.json")
                    file.writeText(response)
                    Log.d("MyApp", "getFamilyDataSet length: ${response.length}")
                    Log.d("MyApp", "family JSON saved to: ${file.absolutePath}")
                } catch (e: Exception) {
                    Log.d("MyApp", "getFamilyDataSet error: ${e.message}")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            })
    }

    /**
     * used to get the user dataset from the server if logged in.
     */
    fun getUserDataSet() {
        val headers = if (JWTtoken.isNotEmpty()) {
            mapOf("Authorization" to "Bearer $JWTtoken")
        } else emptyMap()

        HttpUtil.httpGetRequest(
            this.httpClient,
            this,
            BuildConfig.USER_URL + "/" + this.userId + "/statistics",
            headers = headers,
            onSuccess = { response ->
                try {
                    this.UserDataSet = JSONObject(response)

                    val dir = java.io.File(filesDir, "testJSONfiles")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = java.io.File(dir, "user_statistics.json")
                    file.writeText(response)

                    Log.d("MyApp", "getUserDataSet length: ${response.length}")
                    Log.d("MyApp", "user JSON saved to: ${file.absolutePath}")
                } catch (e: Exception) {
                    Log.d("MyApp", "getUserDataSet error: ${e.message}")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            })
    }
}