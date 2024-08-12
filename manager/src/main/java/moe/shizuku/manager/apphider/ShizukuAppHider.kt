package moe.shizuku.manager.apphider

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.system.Os
import android.util.Log
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

class ShizukuAppHider(val context: Context) : BaseAppHider(context) {
    private val shizukuRequestCode = 1001

    init {
        ShizukuProvider.enableMultiProcessSupport(false)
    }

    override fun hide(pkgNames: Set<String>) {
        if (!Shizuku.pingBinder()) {
            Log.d(getName(), "Binder not available")
            return
        }
        setAppDisabled(disabled = true, pkgNames = pkgNames)
        setAppHidden(hidden = true, pkgNames = pkgNames)
    }

    override fun show(pkgNames: Set<String>) {
        if (!Shizuku.pingBinder()) {
            Log.d(getName(), "Binder not available")
            return
        }
        setAppDisabled(disabled = false, pkgNames = pkgNames)
        setAppHidden(hidden = false, pkgNames = pkgNames)
    }

    override fun getName(): String {
        return this.javaClass.name
    }

    override fun tryToActive(listener: ActivationCallbackListener) {
        try {
            //Check version
            if (Shizuku.isPreV11()) {
                Log.d(getName(), "checking version: The current version is pre v11!")
                listener.onActivationSuccess(
                    appHider = this.javaClass,
                    success = false,
                    msg = context.getString(R.string.unsupported_shizuku_version)
                )
                return
            }

            //Check the Shizuku permission
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                if (Shizuku.pingBinder()) {
                    Log.d(getName(), "tryToActive: Shizuku is available.")
                    listener.onActivationSuccess(
                        appHider = this.javaClass,
                        success = true,
                        msg = ""
                    )
                    return
                } else {
                    Log.d(getName(), "tryToActive: Binder not available.")
                    listener.onActivationSuccess(
                        appHider = this.javaClass,
                        success = false,
                        msg = context.getString(R.string.shizuku_not_running)
                    )
                    return
                }
            }

            //The Shizuku permission is denied forever
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Log.d(getName(), "tryToActive: permission is denied.")
                listener.onActivationSuccess(
                    appHider = this.javaClass,
                    success = false,
                    msg = context.getString(R.string.shizuku_perrmission_denied)
                )
                return
            }

            //Request Shizuku permission
            val permissionListener = object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    when (grantResult) {
                        PackageManager.PERMISSION_GRANTED -> {
                            Log.d(getName(), "tryToActive: Permission granted.")
                            listener.onActivationSuccess(
                                appHider = this@ShizukuAppHider.javaClass,
                                success = true,
                                msg = ""
                            )
                        }

                        else -> {
                            Log.d(getName(), "tryToActive: Permission denied.")
                            listener.onActivationSuccess(
                                appHider = this@ShizukuAppHider.javaClass,
                                success = false,
                                msg = context.getString(R.string.shizuku_perrmission_denied)
                            )
                        }
                    }
                    Shizuku.removeRequestPermissionResultListener(this)
                }
            }
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.requestPermission(shizukuRequestCode)
            ShizukuSettings.setIsOpenOtherActivity(true)
        } catch (e: Exception) {
            Log.e(getName(), "tryToActive error: ${e.message}")
            listener.onActivationSuccess(
                appHider = this.javaClass,
                success = false,
                msg = context.getString(R.string.shizuku_note)
            )
        }
    }

    @SuppressLint("PrivateApi")
    private fun setAppDisabled(disabled: Boolean, pkgNames: Set<String>) {
        val mSetApplicationEnabledSetting: Method?
        val iPmInstance: Any?

        try {
            val iPmClass = Class.forName("android.content.pm.IPackageManager")
            val iPmStub = Class.forName("android.content.pm.IPackageManager\$Stub")
            val asInterface = iPmStub.getMethod("asInterface", IBinder::class.java)
            iPmInstance = asInterface.invoke(
                null,
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            )
            mSetApplicationEnabledSetting = iPmClass.getMethod(
                "setApplicationEnabledSetting",
                String::class.java,
                Int::class.java,
                Int::class.java,
                Int::class.java,
                String::class.java
            )
        } catch (e: Exception) {
            Log.e(getName(), "get disabled method: $disabled ${e.message}")
            return
        }
        if (mSetApplicationEnabledSetting != null && iPmInstance != null) {
            try {
                pkgNames.forEach {
                    mSetApplicationEnabledSetting.invoke(
                        iPmInstance,
                        it,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER.takeIf { disabled }
                            ?: PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        0,
                        Os.getegid() / 100000,
                        context.packageName
                    )
                    Log.d(getName(), "disabled: $it")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("PrivateApi")
    private fun setAppHidden(hidden: Boolean, pkgNames: Set<String>) {
        val mSetApplicationHiddenAppSettingAsUser: Method?
        val iPmInstance: Any?
        try {
            val iPmClass = Class.forName("android.content.pm.IPackageManager")
            val iPmStub = Class.forName("android.content.pm.IPackageManager\$Stub")
            val asInterface = iPmStub.getMethod("asInterface", IBinder::class.java)
            iPmInstance = asInterface.invoke(
                null,
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            )
            mSetApplicationHiddenAppSettingAsUser = iPmClass.getMethod(
                "setApplicationHiddenSettingAsUser",
                String::class.java,
                Boolean::class.java,
                Int::class.java
            )
        } catch (e: Exception) {
            Log.e(getName(), "get hidden method: $hidden ${e.message}")
            return
        }
        if (mSetApplicationHiddenAppSettingAsUser != null && iPmInstance != null) {
            try {
                pkgNames.forEach {
                    mSetApplicationHiddenAppSettingAsUser.invoke(
                        iPmInstance,
                        it,
                        hidden,
                        Os.getegid() / 100000
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}