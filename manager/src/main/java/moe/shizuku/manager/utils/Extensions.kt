package moe.shizuku.manager.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.AppConstants.FORMAT_MONTH_DAY_TIME
import moe.shizuku.manager.AppConstants.FORMAT_TIME
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.model.GroupApps
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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

fun Long.formatMillisecondsToSimple(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60

    return when {
        hours > 0 -> "${hours}h${minutes}m${seconds}s"
        minutes > 0 -> "${minutes}m${seconds}s"
        else -> "${seconds}s"
    }
}

fun Date.getTimeAsString(): String {
    val formatter = SimpleDateFormat(FORMAT_TIME, Locale.getDefault())
    return formatter.format(this)
}

fun Date.getTimeAsString(format: String): String {
    val formatter = SimpleDateFormat(format, Locale.getDefault())
    return formatter.format(this)
}

fun Date.getWeekRange(): String {
    val calendar = Calendar.getInstance().apply {
        time = this@getWeekRange
        // Set to the start of the week
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek + 1)
    }
    val startOfWeek = calendar.time

    // Move to the end of the week
    calendar.add(Calendar.DAY_OF_WEEK, 6)
    val endOfWeek = calendar.time

    // Format the dates
    val dateFormat = SimpleDateFormat(FORMAT_MONTH_DAY_TIME, Locale.getDefault())
    return "${dateFormat.format(startOfWeek)} - ${dateFormat.format(endOfWeek)}"
}

fun String.toDate(): Date? {
    val formatter = SimpleDateFormat(FORMAT_TIME, Locale.getDefault())
    return try {
        formatter.parse(this)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun calculateHourlyDurations(startTime: Date, endTime: Date, targetHour: Int): Long {
    val calendar = Calendar.getInstance()

    // Set the calendar to the start time and align it to the target hour
    calendar.time = startTime
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    // Set the calendar to the target hour
    calendar.set(Calendar.HOUR_OF_DAY, targetHour)
    val targetHourStart = calendar.time

    // Move to the next hour
    calendar.add(Calendar.HOUR_OF_DAY, 1)
    val targetHourEnd = calendar.time

    // Calculate the overlapping time in milliseconds for the target hour
    val overlapStart = maxOf(startTime.time, targetHourStart.time)
    val overlapEnd = minOf(endTime.time, targetHourEnd.time)
    val durationMillis = if (overlapStart < overlapEnd) overlapEnd - overlapStart else 0L

    return durationMillis
}

fun isInRangeHour(startDate: Date, endDate: Date, target: Int): Boolean {
    val calendar1 = Calendar.getInstance().apply { time = startDate }
    val calendar2 = Calendar.getInstance().apply { time = endDate }
    val calendar3 = Calendar.getInstance().apply {
        time = calendar1.time
        set(Calendar.HOUR_OF_DAY, target)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val startHour = calendar1.get(Calendar.HOUR_OF_DAY)
    val startDay = calendar1.get(Calendar.DAY_OF_YEAR)
    val endHour = calendar1.get(Calendar.HOUR_OF_DAY)
    val endDay = calendar2.get(Calendar.DAY_OF_YEAR)
    val targetHour = calendar3.get(Calendar.HOUR_OF_DAY)
    return if (startDay == endDay) {
        targetHour in startHour..endHour
    } else {
        targetHour in startHour..23 || targetHour in 0..endHour
    }
}

fun isSameDay(date1: Date, date2: Date): Boolean {
    val calendar1 = Calendar.getInstance().apply { time = date1 }
    val calendar2 = Calendar.getInstance().apply { time = date2 }

    return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
            calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
}

fun isSameWeek(date1: Date, date2: Date): Boolean {
    val calendar1 = Calendar.getInstance().apply { time = date1 }
    val calendar2 = Calendar.getInstance().apply { time = date2 }

    return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
            calendar1.get(Calendar.WEEK_OF_YEAR) == calendar2.get(Calendar.WEEK_OF_YEAR)
}

fun isSameMonth(date1: Date, date2: Date): Boolean {
    val calendar1 = Calendar.getInstance().apply { time = date1 }
    val calendar2 = Calendar.getInstance().apply { time = date2 }

    return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
            calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)
}

fun isSameYear(date1: Date, date2: Date): Boolean {
    val calendar1 = Calendar.getInstance().apply { time = date1 }
    val calendar2 = Calendar.getInstance().apply { time = date2 }

    return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
}

fun Context.getMixColor(): List<Int> {
    return this.resources?.getStringArray(R.array.mix_colors)?.map { c ->
        Color.parseColor(c)
    } ?: emptyList()
}