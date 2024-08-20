package moe.shizuku.manager.focus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.databinding.AddFocusItemLayoutBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class AddFocusViewHolder(
    val binding: AddFocusItemLayoutBinding,
    root: View
): BaseViewHolder<Unit>(root), View.OnClickListener {
    companion object {
        val CREATOR = Creator<Any?> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = AddFocusItemLayoutBinding.inflate(inflater, outer.root, true)
            AddFocusViewHolder(inner, outer.root)
        }
    }

    init {
        root.setOnClickListener(this)
    }

    override fun onClick(v: View?) {

    }
}