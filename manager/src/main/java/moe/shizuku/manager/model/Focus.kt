package moe.shizuku.manager.model

import kotlinx.serialization.Serializable

@Serializable
data class Focus(
    val id: String,
    val name: String,
    val time: Long,
)
