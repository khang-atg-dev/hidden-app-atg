package moe.shizuku.manager.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.apphider.ActivationCallbackListener
import moe.shizuku.manager.apphider.ShizukuAppHider
import moe.shizuku.manager.model.GroupApps
import rikka.lifecycle.Resource

class HomeViewModel(context: Context) : ViewModel(), GroupBottomSheetCallback {

    private val _groupApps = MutableLiveData<Resource<List<GroupApps>>>()
    val groupApps = _groupApps as LiveData<Resource<List<GroupApps>>>

    private val _events = Channel<HomeEvents>()
    val events = _events.receiveAsFlow()

    private val appHider = ShizukuAppHider(context)

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
        }
        reloadGroupApps()
    }

    fun actionHideGroup(groupName: String) {
        appHider.tryToActive(object : ActivationCallbackListener {
            override fun <T : moe.shizuku.manager.apphider.BaseAppHider> onActivationSuccess(
                appHider: Class<T>,
                success: Boolean,
                msg: String
            ) {
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
