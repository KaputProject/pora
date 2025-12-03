package si.um.feri.kaput.services

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class TransactionNotificationListener : NotificationListenerService()  {
    private val acceptedPackages = listOf(
        "com.hrc.eb.mobile.android.hibismobiledh",
    )

    private val tag = "TransactionNotifService"

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val notification: Notification = sbn.notification
        val extras: Bundle = notification.extras

        if (sbn.packageName !in acceptedPackages) {
            return
        }

        val title = extras.getString(Notification.EXTRA_TITLE)
        val bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        Log.d(tag, "Notification Posted from: ${sbn.packageName}")
        Log.d(tag, "Title: $title")
        Log.d(tag, "BigTitle: $bigTitle")
        Log.d(tag, "Text: $text")
        Log.d(tag, "BigText: $bigText")
        Log.d(tag, "SubText: $subText")

        // TODO: Handle the notification data here
    }
}