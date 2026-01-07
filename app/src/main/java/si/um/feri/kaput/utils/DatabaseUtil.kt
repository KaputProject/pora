package si.um.feri.kaput.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import si.um.feri.kaput.BuildConfig
import si.um.feri.kaput.MyApplication

class DatabaseUtil(
    private val httpClient: OkHttpClient,
    private val context: MyApplication,
) {
    var JWTtoken: String = ""
    var userId: String = ""
    var familyId: String = ""
    var familyDataSet: JSONObject = JSONObject()
    var UserDataSet: JSONObject = JSONObject()
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
            context,
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
            context,
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
            context,
            BuildConfig.FAMILY_URL + "/" + this.familyId + "/statistics",
            headers = headers,
            onSuccess = { response ->
                try {
                    this.familyDataSet = JSONObject(response)
                    Log.d("MyApp", "Family data set retrieved." + familyDataSet.toString())
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
            context,
            BuildConfig.USER_URL + "/" + this.userId + "/statistics",
            headers = headers,
            onSuccess = { response ->
                try {
                    this.UserDataSet = JSONObject(response)
                } catch (e: Exception) {
                    Log.d("MyApp", "getUserDataSet error: ${e.message}")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            })
    }
}