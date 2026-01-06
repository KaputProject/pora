package si.um.feri.kaput.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import info.mqtt.android.service.MqttAndroidClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

object HttpUtil {
    val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
    const val TAG = "HttpUtil"

    fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    /**
     * Sends a POST request with JSON data to the specified URL and publishes the response to the given MQTT topic if specified.
     */
    fun sendPostRequest(client: OkHttpClient, context: Context, url: String, data: MultipartBody, mqttClient: MqttAndroidClient? = null, mqttTopic: String? = null) {
        try {
            val request = Request.Builder()
                .url(url)
                .post(data)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "HTTP POST request failed: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()

                    if (mqttClient != null && mqttTopic != null) {
                        mqttClient.publish(mqttTopic, responseBody?.toByteArray() ?: "{}".toByteArray(), 0, false)
                    }

                    Log.d(TAG, "HTTP POST request successful. Response published to MQTT topic $mqttTopic")
                }
            })
        } catch (e: Exception) {
            Log.d(TAG, "Exception in sendPostRequest: ${e.message}")
            Toast.makeText(context, "Error sending POST request: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    fun httpPostRequest(
        client: OkHttpClient,
        context: Context,
        url: String,
        data: RequestBody,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val request = Request.Builder()
                .url(url)
                .post(data)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "HTTP POST request failed: ${e.message}")
                    onFailure("HTTP POST request failed: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        Log.d(TAG, "HTTP POST request successful.")
                        onSuccess(responseBody)
                    } else {
                        Log.d(TAG, "HTTP POST request failed with status code: ${response.code}")
                        onFailure("HTTP POST request failed with status code: ${response.code}")
                    }
                }
            })
        } catch (e: Exception) {
            Log.d(TAG, "Exception in logInRequest: ${e.message}")
            Toast.makeText(context, "Error sending POST request: ${e.message}", Toast.LENGTH_LONG).show()
            onFailure("Exception in logInRequest: ${e.message}")
        }
    }
}