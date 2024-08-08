package moe.shizuku.manager.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.model.GroupApps

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

fun Context.checkOverlayPermission() {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val groupApps = ShizukuSettings.getGroupLockedAppsAsSet()
            if (groupApps.isNotEmpty()) {
                groupApps.forEach {
                    ShizukuSettings.getPksByGroupName(
                        it.substringAfterLast(".")
                    )?.let { groupApps ->
                        if (groupApps.isLocked) {
                            val isAllowed =
                                this@checkOverlayPermission.isAccessibilityServiceEnabled() && this@checkOverlayPermission.isCanDrawOverlays()
                            if (!isAllowed) {
                                ShizukuSettings.saveDataByGroupName(
                                    groupApps.groupName, GroupApps(
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

