package si.um.feri.kaput.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.util.UUID

object SettingsUtil {
    const val FILE_NAME = "settings"
//    const val VIBRATION = "vibration_enabled"
//    const val INEDIBLE = "inedible_enabled"
    const val UUID_KEY = "uuid"
    const val BROKER_KEY = "mqtt_broker"
    const val MQTT_USERNAME_KEY = "mqtt_username"
    const val MQTT_PASSWORD_KEY = "mqtt_password"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    fun isNotificationAccessEnabled(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

//    fun isVibrationEnabled(context: Context): Boolean {
//        return getPreferences(context).getBoolean(VIBRATION, true)
//    }
//
//    fun setVibrationEnabled(context: Context, enabled: Boolean) {
//        getPreferences(context).edit {
//            putBoolean(VIBRATION, enabled)
//        }
//    }
//
//    fun isInedibleEnabled(context: Context): Boolean {
//        return getPreferences(context).getBoolean(INEDIBLE, true)
//    }
//
//    fun setInedibleEnabled(context: Context, enabled: Boolean) {
//        getPreferences(context).edit {
//            putBoolean(INEDIBLE, enabled)
//        }
//    }

    fun getMqttBroker(context: Context): String {
        return getPreferences(context).getString(BROKER_KEY, "")!!
    }

    fun getMqttUsername(context: Context): String {
        return getPreferences(context).getString(MQTT_USERNAME_KEY, "")!!
    }

    fun getMqttPassword(context: Context): String {
        return getPreferences(context).getString(MQTT_PASSWORD_KEY, "")!!
    }

    fun handleUserUUID(context: Context) {
        var uuid = getPreferences(context).getString(UUID_KEY, "")

        if (uuid!!.isEmpty()) {
            uuid = UUID.randomUUID().toString()
            getPreferences(context).edit {
                putString(UUID_KEY, uuid)
            }
        }
    }

    fun getUserUUID(context: Context): String? {
        return getPreferences(context).getString(UUID_KEY, "unknown")
    }

    fun getDelayMillis(context: Context): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val value = prefs.getString("delay_value", "60")!!.toLong()
        val unit = prefs.getString("delay_unit", "seconds")

        return when (unit) {
            "seconds" -> value * 1000L
            "minutes" -> value * 60_000L
            "hours" -> value * 3_600_000L
            else -> 60000
        }
    }
}