package moe.shizuku.manager.model

import kotlinx.serialization.Serializable

@Serializable
data class CurrentFocus(
    val id: String,
    val name: String,
    val time: Long,
    val remainingTime: Long,
    val isPaused: Boolean,
)
