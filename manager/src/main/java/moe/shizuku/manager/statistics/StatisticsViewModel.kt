package moe.shizuku.manager.statistics

import android.content.Context
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BaseEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import moe.shizuku.manager.AppConstants.FORMAT_DATE_TIME
import moe.shizuku.manager.AppConstants.FORMAT_YEAR_MONTH_TIME
import moe.shizuku.manager.AppConstants.FORMAT_YEAR_TIME
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.model.StatisticFocus
import moe.shizuku.manager.utils.calculateDailyTotalRunningTime
import moe.shizuku.manager.utils.calculateRunningTimePerDay
import moe.shizuku.manager.utils.formatMillisecondsToSimple
import moe.shizuku.manager.utils.getDurationForTargetHour
import moe.shizuku.manager.utils.getFirstDayOfMonth
import moe.shizuku.manager.utils.getLastDayOfMonth
import moe.shizuku.manager.utils.getMixColor
import moe.shizuku.manager.utils.getTimeAsString
import moe.shizuku.manager.utils.getTotalTimeInDay
import moe.shizuku.manager.utils.getWeekRange
import moe.shizuku.manager.utils.isSameDay
import moe.shizuku.manager.utils.isSameMonth
import moe.shizuku.manager.utils.isSameWeek
import moe.shizuku.manager.utils.isSameYear
import moe.shizuku.manager.utils.toDate
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class StatisticsViewModel(context: Context) : ViewModel(), StatisticCallback {
    private val _state = MutableStateFlow(StatisticState())
    val state = _state.asStateFlow()

    private var allData = ShizukuSettings.getAllStatistics() ?: emptyList()
    private val mixColor = context.getMixColor()

    init {
        refreshData(_state.value.dateIndicator, _state.value.segmentSelected)
    }

    override fun onChangeSegment(segmentId: Int) {
        _state.value.let {
            val newSeg = SegmentTime.fromId(segmentId)
            if (newSeg == it.segmentSelected) return
            val date = Calendar.getInstance().time
            refreshData(date, newSeg)
        }

    }

    override fun onChangeDateIndicator(isIncrease: Boolean) {
        _state.value.let {
            val newDate = Calendar.getInstance().apply {
                time = it.dateIndicator
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            newDate.add(it.segmentSelected.typeOfTime, if (isIncrease) 1 else -1)
            refreshData(newDate.time, it.segmentSelected)
        }

    }

    override fun refreshData(dateIndicator: Date, segmentSelected: SegmentTime) {
        allData = ShizukuSettings.getAllStatistics() ?: emptyList()
        val filteredDate = allData.filterNotNull().filter {
            val startDate = it.startTime.toDate() ?: return@filter false
            val endDate = it.endTime.toDate() ?: return@filter false
            when (segmentSelected) {
                SegmentTime.DAY -> isSameDay(startDate, endDate, dateIndicator)
                SegmentTime.WEEK -> isSameWeek(startDate, endDate, dateIndicator)
                SegmentTime.MONTH -> isSameMonth(startDate, endDate, dateIndicator)
                SegmentTime.YEAR -> isSameYear(startDate, endDate, dateIndicator)
            }
        }
        val totalTime = filteredDate.sumOf {
            getTotalTimeInDay(
                it.startTime,
                it.endTime,
                dateIndicator,
                segmentSelected
            )
        }
        val groupedData = filteredDate.groupBy { it.focusId }
        _state.update {
            it.copy(
                dateIndicator = dateIndicator,
                segmentSelected = segmentSelected,
                totalTime = totalTime,
                numberOfFocuses = filteredDate.size,
                pieData = getPieData(groupedData, totalTime),
                listStatistics = getListStatistics(groupedData, totalTime, dateIndicator, segmentSelected),
                barData = getBarData(filteredDate, dateIndicator),
                periodicBarData = getPeriodicBarData(
                    filteredDate,
                    dateIndicator,
                    segmentSelected
                )
            )
        }
    }

    private fun getPieData(data: Map<String, List<StatisticFocus>>, totalTime: Long): PieData {
        if (data.isEmpty()) return PieData()
        val entries = data.map { d ->
            PieEntry(d.value.sumOf { i -> i.runningTime }.toFloat() / totalTime, d.value[0].name)
        }
        val pieSet = PieDataSet(entries, "").apply {
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueTextSize = 9f
            valueLineWidth = 1f
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
            colors = mixColor
            valueFormatter = CustomPieValueFormatter(totalTime)
        }
        return PieData(pieSet)
    }

    private fun getListStatistics(
        data: Map<String, List<StatisticFocus>>,
        totalTime: Long,
        dateIndicator: Date,
        segmentSelected: SegmentTime,
    ): List<StatisticItem> {
        var index = 0
        return data.map { d ->
            val sum = d.value.sumOf { i -> getTotalTimeInDay(
                i.startTime,
                i.endTime,
                dateIndicator,
                segmentSelected
            ) }
            StatisticItem(
                name = d.value[0].name,
                time = sum,
                color = mixColor[index++],
                percentage = (sum.toFloat() / totalTime) * 100,
                numberOfFocuses = d.value.size
            )
        }
    }

    private fun getBarData(
        data: List<StatisticFocus>,
        dateIndicator: Date
    ): BarData {
        if (data.isEmpty()) return BarData()
        val entries = (0 until 24).map {
            var value = 0f
            data.forEach { d ->
                val startDate = d.startTime.toDate() ?: return BarData()
                val endDate = d.endTime.toDate() ?: return BarData()
                value += getDurationForTargetHour(startDate, endDate, dateIndicator, it)
            }
            BarEntry(it.toFloat(), value)
        }
        val barDataSet = BarDataSet(entries, "").apply {
            setDrawValues(false)
        }
        return BarData(barDataSet)
    }

    private fun getPeriodicBarData(
        data: List<StatisticFocus>,
        dateIndicator: Date,
        segmentSelected: SegmentTime
    ): BarData {
        val entries: List<BaseEntry>? = when (segmentSelected) {
            SegmentTime.DAY -> null
            SegmentTime.WEEK -> {
                val times = calculateRunningTimePerDay(data)
                if (times.isEmpty())
                    null
                else
                    (Calendar.SUNDAY..Calendar.SATURDAY).map {
                        val time = times.find { t -> t.first == it }?.second?.toFloat()
                        BarEntry(it.toFloat(), time ?: 0f)
                    }
            }

            SegmentTime.MONTH -> {
                val times = calculateDailyTotalRunningTime(data)
                if (times.isEmpty()) null
                else {
                    (dateIndicator.getFirstDayOfMonth()..dateIndicator.getLastDayOfMonth()).map {
                        val time = times.find { t -> t.first == it }?.second?.toFloat()
                        BarEntry(it.toFloat(), time ?: 0f)
                    }
                }
            }

            SegmentTime.YEAR -> null
        }
        entries?.map { i -> i as BarEntry }?.let {
            val barDataSet = BarDataSet(it, "").apply {
                setDrawValues(false)
            }
            return BarData(barDataSet).apply {
                barWidth = 0.7f
            }
        } ?: return BarData()
    }
}

data class StatisticState(
    val listStatistics: List<StatisticItem> = emptyList(),
    val barData: BarData = BarData(),
    val periodicBarData: BarData = BarData(),
    val pieData: PieData = PieData(),
    val totalTime: Long = 0L,
    val numberOfFocuses: Int = 0,
    val segmentSelected: SegmentTime = SegmentTime.DAY,
    val dateIndicator: Date = Calendar.getInstance().time,
)

interface StatisticCallback {
    fun onChangeSegment(segmentId: Int)
    fun onChangeDateIndicator(isIncrease: Boolean)
    fun refreshData(dateIndicator: Date, segmentSelected: SegmentTime)
}

enum class SegmentTime(val id: Int, val typeOfTime: Int) {
    DAY(R.id.tab_day, Calendar.DAY_OF_YEAR),
    WEEK(R.id.tab_week, Calendar.WEEK_OF_YEAR),
    MONTH(R.id.tab_month, Calendar.MONTH),
    YEAR(R.id.tab_year, Calendar.YEAR);

    companion object {
        fun fromId(id: Int): SegmentTime = values().find { it.id == id } ?: DAY
    }

    fun getFormatTime(date: Date) = when (this) {
        DAY -> date.getTimeAsString(FORMAT_DATE_TIME)
        WEEK -> date.getWeekRange()
        MONTH -> date.getTimeAsString(FORMAT_YEAR_MONTH_TIME)
        YEAR -> date.getTimeAsString(FORMAT_YEAR_TIME)
    }
}


class CustomPieValueFormatter(
    private val totalTime: Long
) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        // Format your label with a newline
        return ((value * totalTime) / 100).toLong().formatMillisecondsToSimple()
    }
}

class CustomBarValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        // Format your label with a newline
        val hours = TimeUnit.MILLISECONDS.toHours(value.toLong())
        val minutes = TimeUnit.MILLISECONDS.toMinutes(value.toLong()) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(value.toLong()) % 60

        return when {
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}

class CustomDaysOfWeekValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val weekDays = DateFormatSymbols().weekdays
        return weekDays[value.toInt()]
    }
}