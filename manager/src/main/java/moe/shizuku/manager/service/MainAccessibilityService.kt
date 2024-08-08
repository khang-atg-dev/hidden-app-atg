package moe.shizuku.manager.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import moe.shizuku.manager.AppConstants.RELOAD_PACKAGES_FOR_LOCK
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.utils.getPackageLauncher

class MainAccessibilityService : AccessibilityService() {

    private var packageLauncher: String = ""

    private val _broadcastReceiver: BroadcastReceiver = (object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RELOAD_PACKAGES_FOR_LOCK -> {
                    packageLauncher = context?.getPackageLauncher() ?: ""
                    val groups = ShizukuSettings.getGroupLockedAppsAsSet()
                    val pkgsSet = mutableSetOf<String>()
                    groups.forEach {
                        val groupApps =
                            ShizukuSettings.getPksByGroupName(it.substringAfterLast("."))
                        groupApps?.let { d ->
                            if (d.isLocked) pkgsSet.addAll(d.pkgs)
                        }
                    }
                    this@MainAccessibilityService.serviceInfo =
                        AccessibilityServiceInfo().apply {
                            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                            packageNames = pkgsSet.toTypedArray() + packageLauncher
                            notificationTimeout = 300
                        }
                }

                else -> {}
            }
        }
    })

    private fun unregisterReceivers() {
        this.unregisterReceiver(_broadcastReceiver)
    }

    private fun registerReceivers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiversAndroid33AndHigher()
        } else {
            registerReceiversLowerAndroid33()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerReceiversAndroid33AndHigher() {
        this.registerReceiver(
            _broadcastReceiver,
            IntentFilter(RELOAD_PACKAGES_FOR_LOCK),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiversLowerAndroid33() {
        this.registerReceiver(
            _broadcastReceiver, IntentFilter(RELOAD_PACKAGES_FOR_LOCK)
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    checkAppIsLocked(it.packageName.toString())
                }

                else -> {}
            }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        unregisterReceivers()
        return super.onUnbind(intent)
    }

    override fun onServiceConnected() {
        registerReceivers()

        this@MainAccessibilityService.serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        }
    }

    private fun checkAppIsLocked(pkName: String) {

    }
}