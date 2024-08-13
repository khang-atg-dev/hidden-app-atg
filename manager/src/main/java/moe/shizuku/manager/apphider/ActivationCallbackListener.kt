package moe.shizuku.manager.apphider

interface ActivationCallbackListener {
    fun <T : BaseAppHider> onActivationSuccess(appHider: Class<T>, success: Boolean, msg: String)
}
