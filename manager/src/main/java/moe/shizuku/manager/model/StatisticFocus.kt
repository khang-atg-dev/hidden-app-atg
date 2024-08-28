package moe.shizuku.manager.model

import kotlinx.serialization.Serializable

@Serializable
data class StatisticFocus(
    val id: String,
    val focusId: String,
    val name: String,
    val time: Long,
    val runningTime: Long,
    val pauseTime: Int,
    val startTime: String,
    val endTime: String,
)