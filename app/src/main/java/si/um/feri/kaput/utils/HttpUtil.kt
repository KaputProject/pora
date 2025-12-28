package si.um.feri.kaput.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import info.mqtt.android.service.MqttAndroidClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object HttpUtil {
    val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
    const val TAG = "HttpUtil"

    fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    /**
     * Sends a POST request with JSON data
     * If you want to send MQTT after response, add mqttClient, mqttTopic to params
     */
    fun sendPostRequest(client: OkHttpClient, context: Context, url: String, data: MultipartBody, mqttClient: MqttAndroidClient, mqttTopic: String) {
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

                    mqttClient.publish(mqttTopic, responseBody?.toByteArray() ?: "{}".toByteArray(), 0, false)
                    Log.d(TAG, "HTTP POST request successful. Response published to MQTT topic $mqttTopic")
                }
            })
        } catch (e: Exception) {
            Log.d(TAG, "Exception in sendPostRequest: ${e.message}")
            Toast.makeText(context, "Error sending POST request: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}