package moe.shizuku.manager.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupApps(
    val groupName: String,
    val pkgs: Set<String>,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val timeOut: Int = 0,
)
