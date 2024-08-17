package moe.shizuku.manager.home

import moe.shizuku.manager.model.GroupApps
import rikka.recyclerview.IdBasedRecyclerViewAdapter
import rikka.recyclerview.IndexCreatorPool

class HomeAdapter : IdBasedRecyclerViewAdapter(ArrayList()) {

    init {
        updateData(null)
        setHasStableIds(true)
    }

    companion object {
        private const val ID_GROUP = 2L
        private const val ID_ADD_GROUP = 2L
    }

    override fun onCreateCreatorPool(): IndexCreatorPool {
        return IndexCreatorPool()
    }

    fun updateData(data: List<GroupApps>?) {
        clear()
        if (!data.isNullOrEmpty()) {
            data.forEach {
                addItem(
                    GroupAppsViewHolder.CREATOR,
                    it,
                    ID_GROUP + it.id.hashCode()
                )
            }
        }
        addItem(
            AddGroupViewHolder.CREATOR,
            null,
            ID_ADD_GROUP
        )
        notifyDataSetChanged()
    }
}

interface HomeCallback {
    fun onClickAddGroup()
    fun onClickGroup(id: String)
    fun onDeleteGroup(id: String)
    fun onEditTimeout(id: String)
    fun onActionHide(id: String)
    fun onActionLock(id: String)
}
