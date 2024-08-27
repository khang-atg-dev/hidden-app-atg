package moe.shizuku.manager.focus

import moe.shizuku.manager.model.Focus
import rikka.recyclerview.IdBasedRecyclerViewAdapter
import rikka.recyclerview.IndexCreatorPool

class FocusAdapter : IdBasedRecyclerViewAdapter(ArrayList()) {
    init {
        setHasStableIds(true)
    }

    companion object {
        private const val ID_FOCUS = 2L
    }

    override fun onCreateCreatorPool(): IndexCreatorPool {
        return IndexCreatorPool()
    }

    fun updateData(data: List<Focus>?) {
        clear()
        if (!data.isNullOrEmpty()) {
            data.forEach {
                addItem(
                    FocusViewHolder.CREATOR,
                    it,
                    ID_FOCUS + it.id.hashCode()
                )
            }
        }
        notifyDataSetChanged()
    }
}