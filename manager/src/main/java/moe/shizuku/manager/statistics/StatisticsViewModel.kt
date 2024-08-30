package moe.shizuku.manager.statistics

import android.content.Context
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
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
import moe.shizuku.manager.statistics.SegmentTime.DAY
import moe.shizuku.manager.utils.calculateHourlyDurations
import moe.shizuku.manager.utils.formatMillisecondsToSimple
import moe.shizuku.manager.utils.getMixColor
import moe.shizuku.manager.utils.getTimeAsString
import moe.shizuku.manager.utils.getWeekRange
import moe.shizuku.manager.utils.isSameDay
import moe.shizuku.manager.utils.isSameMonth
import moe.shizuku.manager.utils.isSameWeek
import moe.shizuku.manager.utils.isSameYear
import moe.shizuku.manager.utils.toDate
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class StatisticsViewModel(context: Context) : ViewModel(), StatisticCallback {
    private val _state = MutableStateFlow(StatisticState())
    val state = _state.asStateFlow()

    private var allData = ShizukuSettings.getAllStatistics() ?: emptyList()
    private val mixColor = context.getMixColor()
    private val timeMark = (0 until 24).toList()

    init {
        refreshData()
    }

    override fun onChangeSegment(segmentId: Int) {
        _state.update {
            val newSeg = SegmentTime.fromId(segmentId)
            if (newSeg == it.segmentSelected) return@update it
            val date = Calendar.getInstance().time
            it.copy(
                segmentSelected = newSeg,
                dateIndicator = date,
            )
        }
        refreshData()
    }

    override fun onChangeDateIndicator(isIncrease: Boolean) {
        _state.update {
            val newDate = Calendar.getInstance().apply {
                time = it.dateIndicator
            }
            newDate.add(it.segmentSelected.typeOfTime, if (isIncrease) 1 else -1)
            it.copy(
                dateIndicator = newDate.time
            )
        }
        refreshData()
    }

    override fun refreshData() {
        _state.value.let { state ->
            allData = ShizukuSettings.getAllStatistics() ?: emptyList()
            val filteredDate = allData.filter {
                val startDate = it.startTime.toDate() ?: return@filter false
                when (state.segmentSelected) {
                    DAY -> isSameDay(startDate, state.dateIndicator)
                    SegmentTime.WEEK -> isSameWeek(startDate, state.dateIndicator)
                    SegmentTime.MONTH -> isSameMonth(startDate, state.dateIndicator)
                    SegmentTime.YEAR -> isSameYear(startDate, state.dateIndicator)
                }
            }
            val totalTime = filteredDate.sumOf { it.runningTime }
            val groupedData = filteredDate.groupBy { it.focusId }
            _state.update {
                it.copy(
                    totalTime = totalTime,
                    numberOfFocuses = filteredDate.size,
                    pieData = getPieData(groupedData, totalTime),
                    listStatistics = getListStatistics(groupedData, totalTime),
                    barData = getBarData(filteredDate)
                )
            }
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
        totalTime: Long
    ): List<StatisticItem> {
        var index = 0
        return data.map { d ->
            val sum = d.value.sumOf { i -> i.runningTime }
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
        data: List<StatisticFocus>
    ): BarData {
        if (data.isEmpty()) return BarData()
        val entries = timeMark.map {
            var value = 0f
            data.forEach { d ->
                val startDate = d.startTime.toDate() ?: return BarData()
                val endDate = d.endTime.toDate() ?: return BarData()
                value += calculateHourlyDurations(startDate, endDate, it)
            }
            BarEntry(it.toFloat(), value)
        }
        val barDataSet = BarDataSet(entries, "").apply {
            setDrawValues(false)
        }
        return BarData(barDataSet)
    }
}

data class StatisticState(
    val listStatistics: List<StatisticItem> = emptyList(),
    val barData: BarData = BarData(),
    val pieData: PieData = PieData(),
    val totalTime: Long = 0L,
    val numberOfFocuses: Int = 0,
    val segmentSelected: SegmentTime = DAY,
    val dateIndicator: Date = Calendar.getInstance().time,
)

interface StatisticCallback {
    fun onChangeSegment(segmentId: Int)
    fun onChangeDateIndicator(isIncrease: Boolean)
    fun refreshData()
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
            hours > 0 -> "${hours}h${minutes}m${seconds}s"
            minutes > 0 -> "${minutes}m${seconds}s"
            else -> "${seconds}s"
        }
    }
}