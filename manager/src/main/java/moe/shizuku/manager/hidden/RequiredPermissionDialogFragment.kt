package moe.shizuku.manager.hidden

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.RequiredPermissionDialogFragmentBinding
import moe.shizuku.manager.utils.AutoStartPermissionHelper
import moe.shizuku.manager.utils.hasBatteryOptimizationExemption
import moe.shizuku.manager.utils.hasNotificationPermission


class RequiredPermissionDialogFragment : DialogFragment() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            ShizukuSettings.setIsOpenOtherActivity(false)
        }
        notificationPermit.isEnabled = !(context?.hasNotificationPermission() ?: false)
    }

    private val requestSpecialPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            ShizukuSettings.setIsOpenOtherActivity(false)
            lifecycleScope.launch(Dispatchers.Main) {
                batteryOptimizationPermit?.isEnabled =
                    !(context?.hasBatteryOptimizationExemption() ?: false)
            }
        }
    }

    private lateinit var binding: RequiredPermissionDialogFragmentBinding
    private lateinit var notificationPermit: MaterialButton
    private var batteryOptimizationPermit: MaterialButton? = null
    private var autoStartPermit: MaterialButton? = null
    private lateinit var groupNotification: LinearLayout
    private val autoStartPermissionHelper: AutoStartPermissionHelper =
        AutoStartPermissionHelper.instance


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        binding = RequiredPermissionDialogFragmentBinding.inflate(layoutInflater)

        val builder = MaterialAlertDialogBuilder(context).apply {
            setTitle(context.getString(R.string.permissions_required))
            setView(binding.root)
            isCancelable = false
        }
        val dialog = builder.create()
        dialog.setOnShowListener { onDialogShown(it) }
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        ShizukuSettings.setIsOpenOtherActivity(false)
        requestPermissionLauncher.unregister()
        requestSpecialPermissionLauncher.unregister()
        super.onDismiss(dialog)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            ShizukuSettings.setIsOpenOtherActivity(false)
            context?.let {
                val allPermissionsGranted =
                    it.hasNotificationPermission() &&
                            it.hasBatteryOptimizationExemption() &&
                            (!autoStartPermissionHelper.isAutoStartPermissionAvailable(
                                it,
                                false
                            ) || autoStartPermissionHelper.getAutoStartPermission(it, false, false))
                lifecycleScope.launch(Dispatchers.Main) {
                    if (allPermissionsGranted) {
                        dismiss()
                    } else {

                        autoStartPermit?.isEnabled =
                            !autoStartPermissionHelper.getAutoStartPermission(
                                context = it,
                                open = false,
                                newTask = false
                            ) && autoStartPermissionHelper.isAutoStartPermissionAvailable(it, false)
                    }
                }
            }
        }
    }

    private fun onDialogShown(dialog: DialogInterface) {
        context?.let { c ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                groupNotification = binding.groupNotification
                groupNotification.visibility = View.VISIBLE
                notificationPermit = binding.notificationPermit.apply {
                    isEnabled = !(c.hasNotificationPermission())
                    setOnClickListener {
                        ShizukuSettings.setIsOpenOtherActivity(true)
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            batteryOptimizationPermit = binding.batteryOptimizationPermit.apply {
                isEnabled = !c.hasBatteryOptimizationExemption()
                setOnClickListener {
                    ShizukuSettings.setIsOpenOtherActivity(true)
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:${c.packageName}")
                    }
                    requestSpecialPermissionLauncher.launch(intent)
                }
            }

            autoStartPermit = binding.autostartPermit.apply {
                isEnabled = !autoStartPermissionHelper.getAutoStartPermission(
                    c,
                    false,
                    false
                ) && autoStartPermissionHelper.isAutoStartPermissionAvailable(c, false)
                setOnClickListener {
                    ShizukuSettings.setIsOpenOtherActivity(true)
                    autoStartPermissionHelper.getAutoStartPermission(c, true, false)
                }
            }

        }
    }
}