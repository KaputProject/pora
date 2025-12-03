package si.um.feri.kaput

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import si.um.feri.kaput.databinding.ActivityMainBinding
import si.um.feri.kaput.utils.SettingsUtil

class MainActivity : AppCompatActivity() {
    lateinit var app: MyApplication

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        app = application as MyApplication

        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()

        if (!SettingsUtil.isNotificationAccessEnabled(this)) {
            showPermissionRequestDialog()
        }
    }

    private fun showPermissionRequestDialog() {
        AlertDialog.Builder(this)
            .setTitle("Additional Permissions Required")
            .setMessage("This app requires notification access to function properly. Please enable it in the settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}