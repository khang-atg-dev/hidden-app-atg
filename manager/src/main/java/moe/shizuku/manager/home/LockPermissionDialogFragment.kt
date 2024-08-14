package moe.shizuku.manager.home

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.LockPermissionDialogFragementBinding
import moe.shizuku.manager.utils.isAccessibilityServiceEnabled
import moe.shizuku.manager.utils.isCanDrawOverlays

class LockPermissionDialogFragment : DialogFragment() {

    private val launcherDrawOverlay = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            ShizukuSettings.setIsOpenOtherActivity(false)
        }
        checkOverlayPermission()
    }

    private val launcherAccessibility = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            ShizukuSettings.setIsOpenOtherActivity(false)
        }
        checkAccessibilityPermission()
    }

    private lateinit var binding: LockPermissionDialogFragementBinding
    private lateinit var overPermit: MaterialButton
    private lateinit var accessibilityPermit: MaterialButton

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        binding = LockPermissionDialogFragementBinding.inflate(layoutInflater)

        val builder = MaterialAlertDialogBuilder(context).apply {
            setTitle(context.getString(R.string.permissions_required))
            setView(binding.root)
            setNegativeButton(android.R.string.cancel, null)
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
            overPermit = binding.overlayPermit
            checkOverlayPermission()
            overPermit.setOnClickListener {
                ShizukuSettings.setIsOpenOtherActivity(true)
                launcherDrawOverlay.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${c.packageName}")
                    )
                )
            }
            accessibilityPermit = binding.accessibilityPermit
            checkAccessibilityPermission()
            accessibilityPermit.setOnClickListener {
                ShizukuSettings.setIsOpenOtherActivity(true)
                launcherAccessibility.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    private fun checkOverlayPermission() {
        CoroutineScope(Dispatchers.Main).launch {
            overPermit.isEnabled = !(context?.isCanDrawOverlays() ?: true)
        }
    }

    private fun checkAccessibilityPermission() {
        CoroutineScope(Dispatchers.Main).launch {
            accessibilityPermit.isEnabled = !(context?.isAccessibilityServiceEnabled() ?: true)
        }
    }
}