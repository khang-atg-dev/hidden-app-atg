package moe.shizuku.manager.statistics

import androidx.lifecycle.ViewModel
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
import moe.shizuku.manager.utils.getTimeAsString
import moe.shizuku.manager.utils.getWeekRange
import java.util.Calendar
import java.util.Date

class StatisticsViewModel : ViewModel(), StatisticCallback {
    private val _state = MutableStateFlow(StatisticState())
    val state = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                data = ShizukuSettings.getAllStatistics() ?: emptyList()
            )
        }
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
    }
}

data class StatisticState(
    val data: List<StatisticFocus> = emptyList(),
    val segmentSelected: SegmentTime = DAY,
    val dateIndicator: Date = Calendar.getInstance().time,
)

interface StatisticCallback {
    fun onChangeSegment(segmentId: Int)
    fun onChangeDateIndicator(isIncrease: Boolean)
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