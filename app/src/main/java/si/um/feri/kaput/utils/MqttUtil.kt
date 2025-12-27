package si.um.feri.kaput.utils

import android.content.Context
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

object MqttUtil {
    private lateinit var BROKER_URL: String
    private lateinit var USERNAME: String
    private lateinit var PASSWORD: String

    const val DEFAULT_TOPIC = "kaput"
    const val UPLOAD_TOPIC = "kaput/upload"
    const val SIMULATION_TOPIC = "kaput/simulation"
    const val EVENT_TOPIC = "kaput/event"

    const val TAG = "MqttUtil"

    fun buildClient(context: Context, clientId: String): MqttAndroidClient {
        val client = MqttAndroidClient(context, BROKER_URL, clientId)
        BROKER_URL = SettingsUtil.getMqttBroker(context)
        USERNAME = SettingsUtil.getMqttUsername(context)
        PASSWORD = SettingsUtil.getMqttPassword(context)

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            userName = USERNAME
            password = PASSWORD.toCharArray()
            isAutomaticReconnect = true
        }

        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost: ${cause?.message}")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Message arrived. Topic: $topic Message: ${message.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "Delivery complete")
            }
        })

        client.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(TAG, "Connected to MQTT broker")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(TAG, "Failed to connect to MQTT broker: ${exception?.message}")
            }
        })

        return client
    }

    fun publish(client: MqttAndroidClient, topic: String, payload: String) {
        if (!client.isConnected) {
            Log.d(TAG, "Client not connected, cannot publish message")
            return
        }

        try {
            val message = MqttMessage()
            message.payload = payload.toByteArray()

            client.publish(topic, message)

            Log.d(TAG, "Message published to topic $topic")
        } catch (e: Exception) {
            Log.d(TAG, "Error Publishing: ${e.message}")
        }
    }
}