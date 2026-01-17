package si.um.feri.kaput.classes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import si.um.feri.kaput.receivers.DataUploadReceiver
import si.um.feri.kaput.utils.SettingsUtil

object AlarmDataUploader {
    fun scheduleNext(context: Context) {
        val delayMillis = SettingsUtil.getDelayMillis(context)
        val triggerAt = System.currentTimeMillis() + delayMillis

        val intent = Intent(context, DataUploadReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }
}
