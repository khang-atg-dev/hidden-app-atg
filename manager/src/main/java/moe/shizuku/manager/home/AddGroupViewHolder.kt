package moe.shizuku.manager.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.databinding.AddGroupLayoutBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class AddGroupViewHolder(
    private val binding: AddGroupLayoutBinding,
    root: View
) : BaseViewHolder<Any?>(root), View.OnClickListener {
    companion object {
        val CREATOR = Creator<Any?> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = AddGroupLayoutBinding.inflate(inflater, outer.root, true)
            AddGroupViewHolder(inner, outer.root)
        }
    }

    init {
        root.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        (adapter?.listener as HomeCallback).onClickAddGroup()
    }
}