package moe.shizuku.manager.home

import moe.shizuku.manager.management.AppsViewModel
import rikka.recyclerview.IdBasedRecyclerViewAdapter
import rikka.recyclerview.IndexCreatorPool

class HomeAdapter(private val homeModel: HomeViewModel, private val appsModel: AppsViewModel) :
    IdBasedRecyclerViewAdapter(ArrayList()) {

    init {
        updateData()
        setHasStableIds(true)
    }

    companion object {

        private const val ID_LOCK = 0L
        private const val ID_HIDE = 1L
    }

    override fun onCreateCreatorPool(): IndexCreatorPool {
        return IndexCreatorPool()
    }

    fun updateData() {
        clear()
        addItem(LockAppsViewHolder.CREATOR, null , ID_LOCK)
        addItem(HideAppsViewHolder.CREATOR, null , ID_HIDE)
        notifyDataSetChanged()
    }
}
