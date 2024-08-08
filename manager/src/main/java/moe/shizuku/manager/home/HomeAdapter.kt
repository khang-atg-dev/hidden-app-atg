package moe.shizuku.manager.home

import moe.shizuku.manager.model.GroupApps
import rikka.recyclerview.IdBasedRecyclerViewAdapter
import rikka.recyclerview.IndexCreatorPool

class HomeAdapter : IdBasedRecyclerViewAdapter(ArrayList()) {

    init {
        updateData()
        setHasStableIds(true)
    }

    companion object {
        private const val ID_GROUP = 2L
        private const val ID_ADD_GROUP = 2L
    }

    override fun onCreateCreatorPool(): IndexCreatorPool {
        return IndexCreatorPool()
    }

    fun updateData() {
        clear()
        addItem(
            GroupAppsViewHolder.CREATOR,
            GroupApps(
                groupName = "Nell Burris",
                pkgs = setOf(),
                isLocked = false,
                isHidden = false,
                timeOut = 5797
            ),
            ID_GROUP
        )
        addItem(
            AddGroupViewHolder.CREATOR,
            null,
            ID_ADD_GROUP
        )
        notifyDataSetChanged()
    }
}

interface HomeCallback{
    fun onClickAddGroup()
}
