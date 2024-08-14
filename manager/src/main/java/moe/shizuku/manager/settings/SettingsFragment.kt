package moe.shizuku.manager.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.lock.LockDialogFragment
import moe.shizuku.manager.shizuku.ShizukuActivity
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val lockDialogFragment = LockDialogFragment()

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

        findPreference<SwitchPreference>("enable_lock")?.isChecked =
            ShizukuSettings.getEnablePassword()

        findPreference<Preference>("change_password")?.setOnPreferenceClickListener {
            ShizukuSettings.setIsChanningPassword(true)
            lockDialogFragment.show(parentFragmentManager, "my_dialog")
            true
        }

        findPreference<ListPreference>("auto_lock_preference")?.let {
            val value = ShizukuSettings.getTimeoutPassword()
            val index = context?.resources?.getStringArray(R.array.auto_lock_timeout_values)
                ?.indexOf(value.toString()) ?: 0
            it.setValueIndex(index)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == "shizuku_setting") {
            val intent = Intent(requireContext(), ShizukuActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onPreferenceTreeClick(preference)
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
                return
            }
            if (key == "auto_lock_preference") {
                val listPreference = findPreference<ListPreference>(key)
                if (listPreference != null) {
                    val index = listPreference.findIndexOfValue(listPreference.value)
                    if (index >= 0) {
                        context?.let { c ->
                            val value =
                                c.resources.getStringArray(R.array.auto_lock_timeout_values)[index]
                            ShizukuSettings.saveTimeoutPassword(value.toLong())
                        }
                    }
                }
            }
        }
    }
}
