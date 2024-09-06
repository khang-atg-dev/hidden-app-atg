package moe.shizuku.manager.statistics.raw

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import moe.shizuku.manager.AppConstants.FORMAT_DATE_TIME
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.model.StatisticFocus
import moe.shizuku.manager.statistics.SegmentTime
import moe.shizuku.manager.utils.getTimeAsString
import moe.shizuku.manager.utils.isSameDay
import moe.shizuku.manager.utils.isSameMonth
import moe.shizuku.manager.utils.isSameWeek
import moe.shizuku.manager.utils.isSameYear
import moe.shizuku.manager.utils.toDate
import java.util.Date

class RawStatisticsViewModel : ViewModel(), RawStatisticsCallback {
    private val _state = MutableStateFlow(RawStatisticsState())
    val state = _state.asStateFlow()

    override fun initData(id: String, segmentSelected: SegmentTime, dateIndicator: Date) {
        val allData = ShizukuSettings.getAllStatistics() ?: emptyList()
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
        val filteredId = if (id.isNotEmpty()) {
            filteredDate.filter { it.focusId == id }
        } else {
            filteredDate
        }
        val result = filteredId.groupBy {
            val startDate = it.startTime.toDate() ?: return
            startDate.getTimeAsString(FORMAT_DATE_TIME)
        }
        _state.update {
            it.copy(
                statisticsData = result
            )
        }
    }
}

data class RawStatisticsState(
    val statisticsData: Map<String, List<StatisticFocus>> = emptyMap(),
)

interface RawStatisticsCallback {
    fun initData(id: String, segmentSelected: SegmentTime, dateIndicator: Date)
}
