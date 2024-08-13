package moe.shizuku.manager.apphider

import android.content.Context

abstract class BaseAppHider(private val context: Context) {
    abstract fun hide(pkgNames: Set<String>)
    abstract fun show(pkgNames: Set<String>)
    abstract fun getName(): String
    abstract fun tryToActive(listener: ActivationCallbackListener)
}