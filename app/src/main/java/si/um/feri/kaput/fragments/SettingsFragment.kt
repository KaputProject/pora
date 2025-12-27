package si.um.feri.kaput.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import si.um.feri.kaput.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}