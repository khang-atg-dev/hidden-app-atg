package moe.shizuku.manager.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, null)

        findPreference<ListPreference>("auto_lock_timeout")?.value = ShizukuSettings.getAutoLockTimeout().toString()

        findPreference<SwitchPreference>("enable_lock")?.isChecked = ShizukuSettings.getEnablePassword()

    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView =
            super.onCreateRecyclerView(inflater, parent, savedInstanceState) as BorderRecyclerView
        recyclerView.fixEdgeEffect()
        recyclerView.addEdgeSpacing(bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)

        val lp = recyclerView.layoutParams
        if (lp is FrameLayout.LayoutParams) {
            lp.rightMargin =
                recyclerView.context.resources.getDimension(R.dimen.rd_activity_horizontal_margin)
                    .toInt()
            lp.leftMargin = lp.rightMargin
        }

        return recyclerView
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.let {
            if (key == "enable_lock") {
                val selectedValue = it.getBoolean(key, false)
                ShizukuSettings.setEnablePassword(selectedValue)
                Log.d("sss", "enable_lock: $selectedValue")
            }
            if (key == "auto_lock_timeout") {
                val selectedValue = it.getString(key, "0") ?: "0"
                ShizukuSettings.setAutoLockTimeout(selectedValue.toInt())
                Log.d("sss", "auto_lock_timeout: $selectedValue")
            }
        }
    }
}
