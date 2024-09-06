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
import moe.shizuku.manager.model.StatisticFocus
import moe.shizuku.manager.statistics.SegmentTime
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
        setFirstDayOfWeek(Calendar.SUNDAY)
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
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

fun Date.getFirstDayOfMonth(): Int {
    val firstDay = Calendar.getInstance().apply {
        time = this@getFirstDayOfMonth
        setFirstDayOfWeek(Calendar.SUNDAY)
    }
    firstDay[Calendar.DAY_OF_MONTH] = 1
    return firstDay.get(Calendar.DAY_OF_MONTH)
}

fun Date.getLastDayOfMonth(): Int {
    val lastDay = Calendar.getInstance().apply {
        time = this@getLastDayOfMonth
        setFirstDayOfWeek(Calendar.SUNDAY)
    }
    lastDay.add(Calendar.MONTH, 1)
    lastDay[Calendar.DAY_OF_MONTH] = 1
    lastDay.add(Calendar.DAY_OF_MONTH, -1)
    return lastDay.get(Calendar.DAY_OF_MONTH)
}

fun calculateMonthlyTotalRunningTime(
    tasks: List<StatisticFocus>,
    targetDate: Date,
    targetMonth: Int
): Long {
    var result = 0L
    tasks.forEach {
        val start = it.startTime.toDate() ?: return@forEach
        val end = it.endTime.toDate() ?: return@forEach
        val startCal = Calendar.getInstance().apply {
            time = start
            setFirstDayOfWeek(Calendar.SUNDAY)
        }
        val endCal = Calendar.getInstance().apply {
            time = end
            setFirstDayOfWeek(Calendar.SUNDAY)
        }
        val targetCal = Calendar.getInstance().apply {
            time = targetDate
            setFirstDayOfWeek(Calendar.SUNDAY)
            set(Calendar.MONTH, targetMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetYear = targetCal.get(Calendar.YEAR)
        val startYear = startCal.get(Calendar.YEAR)
        val endYear = endCal.get(Calendar.YEAR)
        val startMonth = startCal.get(Calendar.MONTH)
        val endMonth = endCal.get(Calendar.MONTH)
        when {
            startYear == targetYear && endYear == targetYear -> {
                when {
                    startMonth == targetMonth && endMonth == targetMonth -> {
                        result += endCal.timeInMillis - startCal.timeInMillis
                    }

                    startMonth == targetMonth -> {
                        val endDayOfMonth = Calendar.getInstance().apply {
                            setFirstDayOfWeek(Calendar.SUNDAY)
                            time = targetDate
                            set(Calendar.MONTH, endMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        result += endDayOfMonth.timeInMillis - startCal.timeInMillis
                    }

                    endMonth == targetMonth -> {
                        val startDayOfMonth = Calendar.getInstance().apply {
                            setFirstDayOfWeek(Calendar.SUNDAY)
                            time = targetDate
                            set(Calendar.MONTH, endMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        result += endCal.timeInMillis - startDayOfMonth.timeInMillis
                    }
                }
            }

            startYear == targetYear -> {
                if (startMonth == targetMonth) {
                    val endDayOfMonth = Calendar.getInstance().apply {
                        setFirstDayOfWeek(Calendar.SUNDAY)
                        time = targetDate
                        set(Calendar.YEAR, endYear)
                        set(Calendar.MONTH, endMonth)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    result += endDayOfMonth.timeInMillis - startCal.timeInMillis
                }
            }

            endYear == targetYear -> {
                if (endMonth == targetMonth) {
                    val startDayOfMonth = Calendar.getInstance().apply {
                        setFirstDayOfWeek(Calendar.SUNDAY)
                        time = targetDate
                        set(Calendar.YEAR, endYear)
                        set(Calendar.MONTH, endMonth)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    result += endCal.timeInMillis - startDayOfMonth.timeInMillis
                }
            }
        }
    }
    return result
}

fun calculateDailyTotalRunningTime(
    tasks: List<StatisticFocus>,
    targetDate: Date
): List<Pair<Int, Long>> {
    val result = mutableMapOf<Int, Long>()
    tasks.forEach {
        val start = it.startTime.toDate() ?: return@forEach
        val end = it.endTime.toDate() ?: return@forEach
        val startCal = Calendar.getInstance().apply {
            time = start
            setFirstDayOfWeek(Calendar.SUNDAY)
        }
        val endCal = Calendar.getInstance().apply {
            time = end
            setFirstDayOfWeek(Calendar.SUNDAY)
        }
        val targetCal = Calendar.getInstance().apply {
            time = targetDate
            setFirstDayOfWeek(Calendar.SUNDAY)
        }
        val targetMonth = targetCal.get(Calendar.MONTH)
        val startMonth = startCal.get(Calendar.MONTH)
        val endMonth = endCal.get(Calendar.MONTH)
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)
        val endDay = endCal.get(Calendar.DAY_OF_MONTH)
        when {
            startMonth == targetMonth && endMonth == targetMonth -> {
                if (startDay == endDay) {
                    result[startDay] = result.getOrDefault(
                        startDay,
                        0
                    ) + endCal.timeInMillis - startCal.timeInMillis
                } else {
                    val endDayOfStartDay = Calendar.getInstance().apply {
                        time = start
                        setFirstDayOfWeek(Calendar.SUNDAY)
                        add(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    result[startDay] = result.getOrDefault(
                        startDay,
                        0
                    ) + endDayOfStartDay.timeInMillis - startCal.timeInMillis
                    val startDayOfEndDay = Calendar.getInstance().apply {
                        time = end
                        setFirstDayOfWeek(Calendar.SUNDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    result[endDay] = result.getOrDefault(
                        endDay,
                        0
                    ) + endCal.timeInMillis - startDayOfEndDay.timeInMillis
                }
            }

            startMonth == targetMonth -> {
                val endDayOfStartDay = Calendar.getInstance().apply {
                    time = start
                    setFirstDayOfWeek(Calendar.SUNDAY)
                    add(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                result[startDay] = result.getOrDefault(
                    startDay,
                    0
                ) + endDayOfStartDay.timeInMillis - startCal.timeInMillis
            }

            endMonth == targetMonth -> {
                val startDayOfEndDay = Calendar.getInstance().apply {
                    time = end
                    setFirstDayOfWeek(Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                result[endDay] = result.getOrDefault(
                    endDay,
                    0
                ) + endCal.timeInMillis - startDayOfEndDay.timeInMillis
            }
        }
    }
    return result.toList().sortedBy { it.first }
}

fun calculateRunningTimePerDay(
    tasks: List<StatisticFocus>,
    targetDate: Date,
    segmentTime: SegmentTime
): List<Pair<Int, Long>> {
    if (tasks.isEmpty()) return emptyList()
    val firstField =
        Calendar.DAY_OF_MONTH.takeIf { segmentTime == SegmentTime.MONTH } ?: Calendar.DAY_OF_WEEK
    val secondField =
        Calendar.MONTH.takeIf { segmentTime == SegmentTime.MONTH } ?: Calendar.WEEK_OF_YEAR
    val dayRunningTimeMap = mutableMapOf<Int, Long>()
    val targetCal = Calendar.getInstance().apply {
        time = targetDate
        setFirstDayOfWeek(Calendar.SUNDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    when (segmentTime) {
        SegmentTime.WEEK -> {
            targetCal.setFirstDayOfWeek(Calendar.SUNDAY)
            targetCal.set(Calendar.DAY_OF_WEEK, targetCal.firstDayOfWeek)
        }

        SegmentTime.MONTH -> {
            targetCal.set(Calendar.DAY_OF_MONTH, 1)
        }

        else -> {}
    }
    val target = targetCal.get(firstField)
    val targetMonth = targetCal.get(secondField)
    val nextTargetCal = targetCal.apply {
        when (segmentTime) {
            SegmentTime.WEEK -> add(Calendar.WEEK_OF_YEAR, 1)
            SegmentTime.MONTH -> add(Calendar.MONTH, 1)
            else -> {}
        }
        add(Calendar.DAY_OF_MONTH, -1)
    }
    val nextTarget = nextTargetCal.get(firstField)
    val nextTargetMonth = nextTargetCal.get(secondField)
    (target..nextTarget).forEach {
        var runningTime = 0L
        tasks.forEach inner@{ task ->
            val start = task.startTime.toDate() ?: return@inner
            val end = task.endTime.toDate() ?: return@inner
            val startCal = Calendar.getInstance().apply {
                time = start
                setFirstDayOfWeek(Calendar.SUNDAY)
            }
            val endCal = Calendar.getInstance().apply {
                time = end
                setFirstDayOfWeek(Calendar.SUNDAY)
            }
            val dayStart = startCal.get(firstField)
            val monthStart = startCal.get(secondField)
            val dayEnd = endCal.get(firstField)
            val monthEnd = endCal.get(secondField)
            runningTime += when {
                dayEnd == it && dayStart == it -> {
                    dayRunningTimeMap.getOrDefault(
                        it,
                        0L
                    ) + endCal.timeInMillis - startCal.timeInMillis
                }

                dayStart == it && (monthStart == targetMonth || monthStart == nextTargetMonth) -> {
                    val midnight = Calendar.getInstance().apply {
                        time = start
                        setFirstDayOfWeek(Calendar.SUNDAY)
                    }
                    midnight.add(Calendar.DAY_OF_YEAR, 1)
                    midnight[Calendar.HOUR_OF_DAY] = 0
                    midnight[Calendar.MINUTE] = 0
                    midnight[Calendar.SECOND] = 0
                    midnight[Calendar.MILLISECOND] = 0
                    midnight.timeInMillis - startCal.timeInMillis
                }

                dayEnd == it && (monthEnd == targetMonth || monthEnd == nextTargetMonth) -> {
                    val midnight = Calendar.getInstance().apply {
                        time = end
                        setFirstDayOfWeek(Calendar.SUNDAY)
                    }
                    midnight[Calendar.HOUR_OF_DAY] = 0
                    midnight[Calendar.MINUTE] = 0
                    midnight[Calendar.SECOND] = 0
                    midnight[Calendar.MILLISECOND] = 0
                    endCal.timeInMillis - midnight.timeInMillis
                }

                else -> 0
            }
        }
        dayRunningTimeMap[it] = runningTime
    }
    return dayRunningTimeMap.map { Pair(it.key, it.value) }
}

fun getTotalTimeInDay(
    startTime: String,
    endTime: String,
    targetDate: Date,
    segmentTime: SegmentTime
): Long {
    val startDate = startTime.toDate() ?: return 0
    val endDate = endTime.toDate() ?: return 0
    val start = Calendar.getInstance().apply {
        setFirstDayOfWeek(Calendar.SUNDAY)
        time = startDate
    }.timeInMillis
    val end = Calendar.getInstance().apply {
        setFirstDayOfWeek(Calendar.SUNDAY)
        time = endDate
    }.timeInMillis
    val targetCal = Calendar.getInstance().apply {
        setFirstDayOfWeek(Calendar.SUNDAY)
        time = targetDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    when (segmentTime) {
        SegmentTime.DAY -> {}
        SegmentTime.WEEK -> {
            targetCal.setFirstDayOfWeek(Calendar.SUNDAY)
            targetCal.set(Calendar.DAY_OF_WEEK, targetCal.firstDayOfWeek)
        }

        SegmentTime.MONTH -> {
            targetCal.set(Calendar.DAY_OF_MONTH, 1)
        }

        SegmentTime.YEAR -> {
            targetCal.set(Calendar.DAY_OF_YEAR, 1)
        }
    }
    val target = targetCal.timeInMillis
    val nextTargetCal = Calendar.getInstance().apply {
        time = targetCal.time
        setFirstDayOfWeek(Calendar.SUNDAY)
        when (segmentTime) {
            SegmentTime.DAY -> add(Calendar.DAY_OF_MONTH, 1)
            SegmentTime.WEEK -> add(Calendar.WEEK_OF_YEAR, 1)
            SegmentTime.MONTH -> add(Calendar.MONTH, 1)
            SegmentTime.YEAR -> add(Calendar.YEAR, 1)
        }
    }
    val nextTarget = nextTargetCal.timeInMillis
    return when {
        start in target..nextTarget && end in target..nextTarget -> {
            end - start
        }

        start in target..nextTarget -> {
            nextTarget - start
        }

        end in target..nextTarget -> {
            end - target
        }

        else -> 0
    }
}

fun getDurationForTargetHour(
    startTime: Date,
    endTime: Date,
    targetDate: Date,
    targetHour: Int,
    segmentTime: SegmentTime
): Long {
    val startCal = Calendar.getInstance().apply {
        setFirstDayOfWeek(Calendar.SUNDAY)
        time = startTime
    }
    val startTimeInMillis = startCal.timeInMillis
    val endCal = Calendar.getInstance().apply {
        setFirstDayOfWeek(Calendar.SUNDAY)
        time = endTime
    }
    val endTimeInMillis = endCal.timeInMillis
    val targetCal = Calendar.getInstance().apply {
        time = targetDate
        setFirstDayOfWeek(Calendar.SUNDAY)
    }
    targetCal.time = when (segmentTime) {
        SegmentTime.DAY -> targetDate
        SegmentTime.WEEK,
        SegmentTime.MONTH,
        SegmentTime.YEAR -> {
            val field = when (segmentTime) {
                SegmentTime.WEEK -> Calendar.WEEK_OF_YEAR
                SegmentTime.MONTH -> Calendar.MONTH
                else -> Calendar.YEAR
            }
            if (startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)) {
                startTime
            } else {
                if (startCal.get(field) == endCal.get(field)) {
                    if (targetHour < 12) {
                        endTime
                    } else {
                        startTime
                    }
                } else {
                    if (startCal.get(field) == targetCal.get(field)) {
                        startTime
                    } else {
                        endTime
                    }
                }

            }
        }
    }
    targetCal.apply {
        set(Calendar.HOUR_OF_DAY, targetHour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val targetTimeInMillis = targetCal.timeInMillis
    val nextTargetTimeInMillis = targetCal.timeInMillis + 60 * 60 * 1000
    return when {
        startTimeInMillis <= targetTimeInMillis && endTimeInMillis >= nextTargetTimeInMillis -> {
            60 * 60 * 1000
        }

        startTimeInMillis in targetTimeInMillis..nextTargetTimeInMillis && endTimeInMillis in targetTimeInMillis..nextTargetTimeInMillis -> {
            endTimeInMillis - startTimeInMillis
        }

        startTimeInMillis in targetTimeInMillis..nextTargetTimeInMillis -> {
            nextTargetTimeInMillis - startTimeInMillis
        }

        endTimeInMillis in targetTimeInMillis..nextTargetTimeInMillis -> {
            endTimeInMillis - targetTimeInMillis
        }

        else -> 0
    }
}

fun isSameDay(startDate: Date, endDate: Date, dateIndicator: Date): Boolean {
    val calendar = Calendar.getInstance().apply {
        setFirstDayOfWeek(Calendar.SUNDAY)
    }

    calendar.time = startDate
    val startDay = calendar.get(Calendar.DAY_OF_YEAR)
    val startYear = calendar.get(Calendar.YEAR)

    calendar.time = endDate
    val endDay = calendar.get(Calendar.DAY_OF_YEAR)
    val endYear = calendar.get(Calendar.YEAR)

    calendar.time = dateIndicator
    val indicatorDay = calendar.get(Calendar.DAY_OF_YEAR)
    val indicatorYear = calendar.get(Calendar.YEAR)

    return (startDay == indicatorDay && startYear == indicatorYear) ||
            (endDay == indicatorDay && endYear == indicatorYear)
}

fun isSameWeek(startDate: Date, endDate: Date, dateIndicator: Date): Boolean {
    val calendar = Calendar.getInstance()
    calendar.setFirstDayOfWeek(Calendar.SUNDAY)

    calendar.time = startDate
    val startWeek = calendar.get(Calendar.WEEK_OF_YEAR)
    val startYear = calendar.get(Calendar.YEAR)

    calendar.time = endDate
    val endWeek = calendar.get(Calendar.WEEK_OF_YEAR)
    val endYear = calendar.get(Calendar.YEAR)

    calendar.time = dateIndicator
    val indicatorWeek = calendar.get(Calendar.WEEK_OF_YEAR)
    val indicatorYear = calendar.get(Calendar.YEAR)

    return (startWeek == indicatorWeek && startYear == indicatorYear) ||
            (endWeek == indicatorWeek && endYear == indicatorYear)
}

fun isSameMonth(startDate: Date, endDate: Date, dateIndicator: Date): Boolean {
    val calendar = Calendar.getInstance().apply {
        setFirstDayOfWeek(Calendar.SUNDAY)
    }

    calendar.time = startDate
    val startMonth = calendar.get(Calendar.MONTH)
    val startYear = calendar.get(Calendar.YEAR)

    calendar.time = endDate
    val endMonth = calendar.get(Calendar.MONTH)
    val endYear = calendar.get(Calendar.YEAR)

    calendar.time = dateIndicator
    val indicatorMonth = calendar.get(Calendar.MONTH)
    val indicatorYear = calendar.get(Calendar.YEAR)

    return (startMonth == indicatorMonth && startYear == indicatorYear) ||
            (endMonth == indicatorMonth && endYear == indicatorYear)
}

fun isSameYear(startDate: Date, endDate: Date, dateIndicator: Date): Boolean {
    val calendar = Calendar.getInstance().apply {
        setFirstDayOfWeek(Calendar.SUNDAY)
    }

    calendar.time = startDate
    val startYear = calendar.get(Calendar.YEAR)

    calendar.time = endDate
    val endYear = calendar.get(Calendar.YEAR)

    calendar.time = dateIndicator
    val indicatorYear = calendar.get(Calendar.YEAR)

    return startYear == indicatorYear || endYear == indicatorYear
}

fun Context.getMixColor(): List<Int> {
    return this.resources?.getStringArray(R.array.mix_colors)?.map { c ->
        Color.parseColor(c)
    } ?: emptyList()
}