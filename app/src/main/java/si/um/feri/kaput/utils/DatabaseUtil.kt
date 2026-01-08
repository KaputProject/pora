package si.um.feri.kaput.utils

import android.app.AlertDialog
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import si.um.feri.kaput.BuildConfig
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.managers.DataManager
import kotlin.text.get

class DatabaseUtil(
    private val httpClient: OkHttpClient,
    private val context: MyApplication,
    private val dataManager: DataManager,
) {
    var JWTtoken: String = ""
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
                    val username = if (json.has("user") && json.get("user") is JSONObject) {
                        json.getJSONObject("user").optString("username", "")
                    } else {
                        json.optString("username", "")
                    }
                    this.JWTtoken = token
                    dataManager.setLoggedInUser(id, username)
                    Log.d("MyApp", "Token: $token")
                    Log.d("MyApp", "user id: $id")
                    Log.d("MyApp", "username: $username")
                    if (this.JWTtoken.isNotEmpty()) {
                        getFamilyId()
                        getUserDataSet()
                    }
                } catch (e: Exception) {
                    Log.d("MyApp", "JSON parse error: ${e.message}")
                }
            },
            onFailure = { errorMsg ->
                Log.d("MyApp", "log in failed: $errorMsg")

                // `httpPostRequest` kliče callback iz background threada,
                // zato uporabi `mainLooper` za Toast
                android.os.Handler(context.mainLooper).post {
                    android.widget.Toast.makeText(
                        context,
                        "Povezava s strežnikom ni uspela. Preverite internetno povezavo.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
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

    private fun writeJsonToFile(fileName: String, jsonObject: JSONObject) {
        try {
            val file = java.io.File(context.filesDir, fileName)
            file.writeText(jsonObject.toString())
            Log.d("MyApp", "Saved JSON to file: $fileName")
        } catch (e: Exception) {
            Log.d("MyApp", "Error writing JSON to file $fileName: ${e.message}")
        }
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
                    dataManager.loadFamilyLocations(this.familyDataSet)
                    writeJsonToFile("family_dataset.json", this.familyDataSet)
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
            BuildConfig.USER_URL + "/" + dataManager.getUserId() + "/statistics",
            headers = headers,
            onSuccess = { response ->
                try {
                    this.UserDataSet = JSONObject(response)
                    dataManager.loadUserLocations(this.UserDataSet)
                    writeJsonToFile("user_dataset.json", this.UserDataSet)
                } catch (e: Exception) {
                    Log.d("MyApp", "getUserDataSet error: ${e.message}")
                }
            },
            onFailure = {
                Log.d("MyApp", it)
            })
    }
}