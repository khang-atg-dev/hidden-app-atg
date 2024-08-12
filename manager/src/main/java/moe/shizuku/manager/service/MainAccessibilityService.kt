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
import moe.shizuku.manager.home.LockScreenManage
import moe.shizuku.manager.utils.getPackageLauncher

class MainAccessibilityService : AccessibilityService() {

    private var packageLauncher: String = ""
    private val lockManager = LockScreenManage()
    private var prevEventTime = 0L

    private val _broadcastReceiver: BroadcastReceiver = (object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RELOAD_PACKAGES_FOR_LOCK -> {
                    packageLauncher = context?.getPackageLauncher() ?: ""
                    val groups = ShizukuSettings.getGroupLockedAppsAsSet()
                    val pkgsSet = mutableSetOf(packageLauncher)
                    groups.forEach {
                        val groupApps =
                            ShizukuSettings.getPksByGroupName(it.substringAfterLast("."))
                        groupApps?.let { d ->
                            if (d.isLocked) pkgsSet.addAll(d.pkgs)
                        }
                    }
                    if (pkgsSet.size == 1) {
                        pkgsSet.clear()
                        pkgsSet.add(baseContext.packageName)
                    }
                    this@MainAccessibilityService.serviceInfo =
                        AccessibilityServiceInfo().apply {
                            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                            packageNames = pkgsSet.toTypedArray()
                            notificationTimeout = 100
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
            if (it.packageName == baseContext.packageName) return
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    prevEventTime = if (prevEventTime == 0L) {
                        event.eventTime
                    } else {
                        if (event.eventTime - prevEventTime < 500) {
                            return
                        } else {
                            event.eventTime
                        }
                    }
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
            packageNames = arrayOf(baseContext.packageName)
        }
    }

    private fun checkAppIsLocked(pkName: String) {
        if (packageLauncher == pkName) {
            lockManager.hideLockScreen()
            lockManager.resetSkipEvent()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lockManager.showLockScreen(context = baseContext, packageName = pkName)
        } else {
            lockManager.hideLockScreen()
            lockManager.resetSkipEvent()
        }
    }
}