package moe.shizuku.manager.focus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.databinding.FocusItemLayoutBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.model.Focus
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class FocusViewHolder(
    private val binding: FocusItemLayoutBinding,
    root: View
) : BaseViewHolder<Focus>(root), View.OnClickListener {

    companion object {
        val CREATOR = Creator<Focus> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = FocusItemLayoutBinding.inflate(inflater, outer.root, true)
            FocusViewHolder(inner, outer.root)
        }
    }

    init {
        root.setOnClickListener(this)
    }


    override fun onBind() {
        data?.let {
            binding.name.text = it.name
            binding.txtMinute.text = it.time.toString()
        }
    }

    override fun onClick(v: View) {
    }
}
