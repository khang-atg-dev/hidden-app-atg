package moe.shizuku.manager.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.model.GroupApps
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit

fun Context.isAccessibilityServiceEnabled(): Boolean {
    val am = this.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices =
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (service in enabledServices) {
        if (service.id.contains("MainAccessibilityService")) {
            return true
        }
    }
    return false
}

fun Context.isCanDrawOverlays(): Boolean {
    return Settings.canDrawOverlays(this)
}

fun Context.getPackageLauncher(): String? {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        this.packageManager.resolveActivity(intent, 0)
    }
    return resolveInfo?.activityInfo?.packageName
}

fun Context.checkLockAppsPermission() {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val isAllowed =
                this@checkLockAppsPermission.isAccessibilityServiceEnabled() && this@checkLockAppsPermission.isCanDrawOverlays()
            if (!isAllowed) {
                val groupApps = ShizukuSettings.getGroupLockedAppsAsSet()
                if (groupApps.isNotEmpty()) {
                    groupApps.forEach {
                        ShizukuSettings.getPksById(
                            it.substringAfterLast(".")
                        )?.let { groupApps ->
                            if (groupApps.isLocked) {
                                ShizukuSettings.saveDataById(
                                    groupApps.id, GroupApps(
                                        id = groupApps.id,
                                        groupName = groupApps.groupName,
                                        pkgs = groupApps.pkgs,
                                        isLocked = false,
                                        isHidden = groupApps.isHidden,
                                        timeOut = groupApps.timeOut,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

fun checkHideAppsPermission() {
    try {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            val groupApps = ShizukuSettings.getGroupLockedAppsAsSet()
            if (groupApps.isNotEmpty()) {
                groupApps.forEach {
                    ShizukuSettings.getPksById(
                        it.substringAfterLast(".")
                    )?.let { groupApps ->
                        if (groupApps.isHidden) {
                            ShizukuSettings.saveDataById(
                                groupApps.id, GroupApps(
                                    id = groupApps.id,
                                    groupName = groupApps.groupName,
                                    pkgs = groupApps.pkgs,
                                    isLocked = groupApps.isLocked,
                                    isHidden = false,
                                    timeOut = groupApps.timeOut,
                                )
                            )
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.getApplicationIcon(pkName: String): Drawable? {
    val packageManager = this.packageManager
    val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getApplicationInfo(
            pkName,
            PackageManager.ApplicationInfoFlags.of(0)
        )
    } else {
        packageManager.getApplicationInfo(pkName, 0)
    }
    return applicationInfo.loadIcon(packageManager)
}

fun Context.getAppLabel(packageName: String): String {
    return try {
        val packageManager = this.packageManager
        val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            packageManager.getApplicationInfo(packageName, 0)
        }
        packageManager.getApplicationLabel(applicationInfo).toString()
    } catch (e: Exception) {
        // Handle the case where the package is not found
        packageName
    }
}

fun Context.hasNotificationPermission(): Boolean {
    val notificationManager =
        this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return notificationManager.areNotificationsEnabled()
}

fun Context.hasBatteryOptimizationExemption(): Boolean {
    val powerManager = this.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(this.packageName)
}

fun DialogFragment.isDialogFragmentShowing(): Boolean {
    return this.dialog?.isShowing == true && !this.isRemoving
}

fun Long.formatMilliseconds(context: Context): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60

    return when {
        hours > 0 -> context.getString(R.string.time_format_hours, hours, minutes, seconds)
        minutes > 0 -> context.getString(R.string.time_format_minutes, minutes, seconds)
        else -> context.getString(R.string.time_format_seconds, seconds)
    }
}