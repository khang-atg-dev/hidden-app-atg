package moe.shizuku.manager.model

data class GroupApps(
    val groupName: String,
    val pkgs: Set<String>,
    val isLocked: Boolean,
    val isHidden: Boolean,
    val timeOut: Int,
)
