package si.um.feri.kaput.services

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.models.Location
import si.um.feri.kaput.models.Transaction
import com.google.gson.Gson
import org.json.JSONArray
import si.um.feri.kaput.utils.MqttUtil

class TransactionNotificationListener : NotificationListenerService()  {
    private val acceptedPackages = listOf(
        "com.hrc.eb.mobile.android.hibismobiledh",
    )

    private lateinit var app: MyApplication

    private val gson = Gson()

    private val regex = Regex("""([\d,.]+)\s+EUR s strani\s+([A-ZČŠŽ ]+\w\.?)""")

    override fun onCreate() {
        super.onCreate()
        app = application as MyApplication
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val notification: Notification = sbn.notification
        val extras: Bundle = notification.extras

        if (sbn.packageName !in acceptedPackages) {
            return
        }

        val title = extras.getString(Notification.EXTRA_TITLE)
        val bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        val text: CharSequence? = extras.getCharSequence(Notification.EXTRA_TEXT)
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

//        Log.d(topic, "Notification Posted from: ${sbn.packageName}")
//        Log.d(topic, "Title: $title")
//        Log.d(topic, "BigTitle: $bigTitle")
//        Log.d(topic, "Text: $text")
//        Log.d(topic, "BigText: $bigText")
//        Log.d(topic, "SubText: $subText")

        val outgoing = (title != "Obvestilo o prilivu")

        val match = regex.find(text!!)

        if (match != null) {
            val amount = match.groupValues[1].replace(',', '.')
            val name = match.groupValues[2]


            val location = app.dataManager.userLocations.find {
                it.identifier == name
            }

            if (location == null) {
                Log.d(MqttUtil.BLOCKCHAIN_UPLOAD_TOPIC, "Location with identifier '$name' not found, extreme event being sent to blockchain")

                val message = {
                    "type" to "extreme_event"
                    "message" to "Location with identifier '$name' not found"
                }

                app.mqttClient.publish(MqttUtil.BLOCKCHAIN_UPLOAD_TOPIC, message.toString().toByteArray(), 0, false)
                return
            }

            if (outgoing) {
                location.total_outflow = (location.total_outflow ?: 0.0) + amount.toDouble()
                location.users?.find {
                    it.userId == app.dataManager.userId
                }?.let {
                    it.numbOfTrans += 1
                    it.outflow += amount.toDouble()
                }
            } else {
                location.total_inflow = (location.total_inflow ?: 0.0) + amount.toDouble()
                location.users?.find {
                    it.userId == app.dataManager.userId
                }?.let {
                    it.numbOfTrans += 1
                    it.inflow += amount.toDouble()
                }
            }

            val transaction = Transaction(
                location = location,
                datetime = System.currentTimeMillis(),
            )

            Log.d(MqttUtil.EVENT_TOPIC, "Parsed transaction: $transaction")

            val transactionJson = gson.toJson(transaction)
            app.mqttClient.publish(MqttUtil.EVENT_TOPIC, transactionJson.toByteArray(), 0, false)
        } else {
            Log.d(MqttUtil.EVENT_TOPIC, "No match found in notification text.")
            return
        }
    }
}