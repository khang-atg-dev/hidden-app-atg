package moe.shizuku.manager.home

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
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.RequiredPermissionDialogFragmentBinding
import moe.shizuku.manager.utils.isAccessibilityServiceEnabled
import moe.shizuku.manager.utils.isCanDrawOverlays

class RequiredPermissionDialogFragment : DialogFragment() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            ShizukuSettings.setIsOpenOtherActivity(false)
        }
    }

    private val request1 = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            ShizukuSettings.setIsOpenOtherActivity(false)
        }
    }

    private lateinit var binding: RequiredPermissionDialogFragmentBinding
    private lateinit var notificationPermit: MaterialButton
    private lateinit var batteryOptimizationPermit: MaterialButton
    private lateinit var autoStartPermit: MaterialButton
    private lateinit var groupNotification: LinearLayout

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        binding = RequiredPermissionDialogFragmentBinding.inflate(layoutInflater)

        val builder = MaterialAlertDialogBuilder(context).apply {
            setTitle("Permissions Required")
            setView(binding.root)
            isCancelable = false
        }
        val dialog = builder.create()
        dialog.setOnShowListener { onDialogShown(it) }
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        ShizukuSettings.setIsOpenOtherActivity(false)
        super.onDismiss(dialog)
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            if (it.isAccessibilityServiceEnabled() && it.isCanDrawOverlays()) {
                dismiss()
            }
        }
    }

    private fun onDialogShown(dialog: DialogInterface) {
        context?.let { c ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                groupNotification = binding.groupNotification
                groupNotification.visibility = View.VISIBLE
                notificationPermit = binding.notificationPermit.apply {
                    setOnClickListener {
                        ShizukuSettings.setIsOpenOtherActivity(true)
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            batteryOptimizationPermit = binding.batteryOptimizationPermit.apply {
                setOnClickListener {
                    ShizukuSettings.setIsOpenOtherActivity(true)
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:${c.packageName}")
                    }
                    request1.launch(intent)
                }
            }

            autoStartPermit = binding.autostartPermit.apply {
                setOnClickListener {
                    ShizukuSettings.setIsOpenOtherActivity(true)
                    val intent = Intent("miui.intent.action.OP_AUTO_START").apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        putExtra("package_name", c.packageName)
                    }
                    request1.launch(intent)
                }
            }

        }
    }
}