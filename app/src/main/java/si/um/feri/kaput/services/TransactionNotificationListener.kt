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
import kotlin.collections.get

class TransactionNotificationListener : NotificationListenerService()  {
    private val acceptedPackages = listOf(
        "com.hrc.eb.mobile.android.hibismobiledh",
    )

    private val topic = "transaction_notifications"

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

        Log.d(topic, "Notification Posted from: ${sbn.packageName}")
        Log.d(topic, "Title: $title")
        Log.d(topic, "BigTitle: $bigTitle")
        Log.d(topic, "Text: $text")
        Log.d(topic, "BigText: $bigText")
        Log.d(topic, "SubText: $subText")

        val outgoing = (title != "Obvestilo o prilivu")

        val match = regex.find(text!!)

        if (match != null) {
            val amount = match.groupValues[1].replace(',', '.')
            val name = match.groupValues[2]

            // TODO: Tukaj dobi lokacijo iz baze glede na name oz. identifier
            val location = Location(
                "1234",
                name
            )

            val transaction = Transaction(
                user = app.databaseUtil.userId,
                location = location,
                datetime = System.currentTimeMillis(),
                change = amount.toDouble(),
                outgoing = outgoing
            )

            Log.d(topic, "Parsed transaction: $transaction")

            val transactionJson = gson.toJson(transaction)
            app.mqttClient.publish(topic, transactionJson.toByteArray(), 0, false)
        } else {
            Log.d(topic, "No match found in notification text.")
            return
        }
    }
}