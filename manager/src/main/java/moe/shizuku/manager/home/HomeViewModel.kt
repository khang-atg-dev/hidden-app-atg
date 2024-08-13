package moe.shizuku.manager.home

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.Manifest
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.apphider.ActivationCallbackListener
import moe.shizuku.manager.apphider.ShizukuAppHider
import moe.shizuku.manager.model.GroupApps
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.utils.Logger.LOGGER
import moe.shizuku.manager.utils.ShizukuSystemApis
import rikka.lifecycle.Resource
import rikka.shizuku.Shizuku
import java.util.concurrent.CancellationException

class HomeViewModel(context: Context) : ViewModel(), GroupBottomSheetCallback {

    private val _serviceStatus = MutableLiveData<Resource<ServiceStatus>>()
    val serviceStatus = _serviceStatus as LiveData<Resource<ServiceStatus>>

    private val _groupApps = MutableLiveData<Resource<List<GroupApps>>>()
    val groupApps = _groupApps as LiveData<Resource<List<GroupApps>>>

    private val _events = Channel<HomeEvents>()
    val events = _events.receiveAsFlow()

    private val appHider = ShizukuAppHider(context)

    private fun load(): ServiceStatus {
        if (!Shizuku.pingBinder()) {
            return ServiceStatus()
        }

        val uid = Shizuku.getUid()
        val apiVersion = Shizuku.getVersion()
        val patchVersion = Shizuku.getServerPatchVersion().let { if (it < 0) 0 else it }
        val seContext = if (apiVersion >= 6) {
            try {
                Shizuku.getSELinuxContext()
            } catch (tr: Throwable) {
                LOGGER.w(tr, "getSELinuxContext")
                null
            }
        } else null
        val permissionTest =
            Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED

        // Before a526d6bb, server will not exit on uninstall, manager installed later will get not permission
        // Run a random remote transaction here, report no permission as not running
        ShizukuSystemApis.checkPermission(
            Manifest.permission.API_V23,
            BuildConfig.APPLICATION_ID,
            0
        )
        return ServiceStatus(uid, apiVersion, patchVersion, seContext, permissionTest)
    }

    fun reloadServiceStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = load()
                _serviceStatus.postValue(Resource.success(status))
            } catch (_: CancellationException) {

            } catch (e: Throwable) {
                _serviceStatus.postValue(Resource.error(e, ServiceStatus()))
            }
        }
    }

    fun reloadGroupApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val groupApps = ShizukuSettings.getGroupLockedAppsAsSet()
                if (groupApps.isEmpty()) {
                    _groupApps.postValue(Resource.success(emptyList()))
                } else {
                    val list = mutableListOf<GroupApps>()
                    groupApps.forEach {
                        ShizukuSettings.getPksByGroupName(
                            it.substringAfterLast(".")
                        )?.let { groupApps ->
                            list.add(groupApps)
                        }
                    }
                    _groupApps.postValue(Resource.success(list))
                }
            } catch (e: Throwable) {
                _groupApps.postValue(Resource.error(e, emptyList()))
            }
        }
    }

    fun changeTimeout(groupName: String, timeout: Long) {
        ShizukuSettings.getPksByGroupName(groupName)?.let {
            ShizukuSettings.saveDataByGroupName(
                groupName,
                GroupApps(
                    groupName = it.groupName,
                    pkgs = it.pkgs,
                    isLocked = it.isLocked,
                    isHidden = it.isHidden,
                    timeOut = timeout,
                )
            )
            it.pkgs.forEach { pk -> ShizukuSettings.saveUnlockStatus(pk, false) }
        }
        reloadGroupApps()
    }

    fun actionHideGroup(groupName: String, context: Context) {
        if (serviceStatus.value?.data?.isRunning != true) {
            viewModelScope.launch {
                _events.send(HomeEvents.ShowShirukuAlert(context.getString(R.string.shizuku_note)))
            }
            return
        }
        appHider.tryToActive(object : ActivationCallbackListener {
            override fun <T : moe.shizuku.manager.apphider.BaseAppHider> onActivationSuccess(
                appHider: Class<T>,
                success: Boolean,
                msg: String
            ) {
                viewModelScope.launch {
                    delay(1000)
                    ShizukuSettings.setIsOpenOtherActivity(false)
                }
                if (success) {
                    ShizukuSettings.getPksByGroupName(groupName)?.let {
                        if (it.pkgs.isNotEmpty()) {
                            if (!it.isHidden) {
                                ShizukuSettings.saveAppsIsHidden(it.pkgs)
                                this@HomeViewModel.appHider.hide(it.pkgs)
                            } else {
                                this@HomeViewModel.appHider.show(it.pkgs)
                                ShizukuSettings.removeAppsIsHidden(it.pkgs)
                            }
                        }
                        ShizukuSettings.saveDataByGroupName(
                            groupName,
                            GroupApps(
                                groupName = it.groupName,
                                pkgs = it.pkgs,
                                isLocked = it.isLocked,
                                isHidden = !it.isHidden,
                                timeOut = it.timeOut,
                            )
                        )
                        reloadGroupApps()
                    }
                } else {
                    viewModelScope.launch {
                        _events.send(HomeEvents.ShowShirukuAlert(msg))
                    }
                }
            }
        })
    }

    fun actionLockGroup(groupName: String) {
        ShizukuSettings.getPksByGroupName(groupName)?.let {
            ShizukuSettings.saveDataByGroupName(
                groupName,
                GroupApps(
                    groupName = it.groupName,
                    pkgs = it.pkgs,
                    isLocked = !it.isLocked,
                    isHidden = it.isHidden,
                    timeOut = it.timeOut,
                )
            )
        }
        reloadGroupApps()
        viewModelScope.launch {
            _events.send(HomeEvents.RefreshLock)
        }
    }

    fun onDeleteGroup(groupName: String) {
        ShizukuSettings.getPksByGroupName(groupName)?.let {
            if (it.isHidden) {
                ShizukuSettings.removeAppsIsHidden(it.pkgs)
                this@HomeViewModel.appHider.show(it.pkgs)
            }
        }
    }

    fun reloadPkgLock() {
        viewModelScope.launch {
            _events.send(HomeEvents.RefreshLock)
        }
    }

    override fun onDone(groupName: String, pks: Set<String>) {
        ShizukuSettings.saveGroupLockedApps(groupName)
        ShizukuSettings.saveDataByGroupName(
            groupName,
            GroupApps(
                groupName = groupName,
                pkgs = pks,
            )
        )
        reloadGroupApps()
    }

    override fun onEditDone(editGroupName: String, newGroupName: String, pkgs: Set<String>) {
        ShizukuSettings.getPksByGroupName(editGroupName)?.let {
            ShizukuSettings.saveGroupLockedApps(newGroupName)
            ShizukuSettings.saveDataByGroupName(
                newGroupName,
                GroupApps(
                    groupName = newGroupName,
                    pkgs = pkgs,
                    isLocked = it.isLocked,
                    isHidden = it.isHidden,
                    timeOut = it.timeOut,
                )
            )
            if (it.isLocked) {
                viewModelScope.launch {
                    _events.send(HomeEvents.RefreshLock)
                }
            }
            if (it.isHidden) reloadHideApps(it.pkgs, pkgs)
        }
        if (editGroupName != newGroupName) ShizukuSettings.removeDataByGroupName(editGroupName)
        reloadGroupApps()
    }

    private fun reloadHideApps(oldPks: Set<String>, newPks: Set<String>) {
        appHider.tryToActive(object : ActivationCallbackListener {
            override fun <T : moe.shizuku.manager.apphider.BaseAppHider> onActivationSuccess(
                appHider: Class<T>,
                success: Boolean,
                msg: String
            ) {
                if (success) {
                    this@HomeViewModel.appHider.show(oldPks)
                    this@HomeViewModel.appHider.hide(newPks)
                } else {
                    viewModelScope.launch {
                        _events.send(HomeEvents.ShowShirukuAlert(msg))
                    }
                }
            }
        })
    }

}

sealed class HomeEvents {
    data class ShowShirukuAlert(val message: String) : HomeEvents()
    object RefreshLock : HomeEvents()
}
