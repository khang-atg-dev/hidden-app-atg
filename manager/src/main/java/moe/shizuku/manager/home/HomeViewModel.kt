package moe.shizuku.manager.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.model.GroupApps
import rikka.lifecycle.Resource

class HomeViewModel : ViewModel(), GroupBottomSheetCallback {

    private val _groupApps = MutableLiveData<Resource<List<GroupApps>>>()
    val groupApps = _groupApps as LiveData<Resource<List<GroupApps>>>

    fun reloadGroupApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val groupApps = ShizukuSettings.getGroupLockedAppsAsSet()
                if (groupApps.isEmpty()) {
                    _groupApps.postValue(Resource.success(emptyList()))
                } else {
                    val list = mutableListOf<GroupApps>()
                    groupApps.forEach {
                        ShizukuSettings.getPksByGroupName(it)?.let { groupApps ->
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
        ShizukuSettings.removeDataByGroupName(editGroupName)
        onDone(newGroupName, pkgs)
    }
}
