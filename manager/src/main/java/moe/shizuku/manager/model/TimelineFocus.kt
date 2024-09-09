package moe.shizuku.manager.model

import kotlinx.serialization.Serializable

@Serializable
data class TimelineFocus(
    val startTime: String,
    val endTime: String,
)