package moe.shizuku.manager.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupApps(
    val id: String,
    val groupName: String,
    val pkgs: Set<String>,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val timeOut: Long = 0,
    val isDefaultGroup: Boolean = false,
)
