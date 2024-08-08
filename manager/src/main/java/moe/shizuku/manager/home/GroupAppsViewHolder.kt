package moe.shizuku.manager.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.databinding.GroupAppsLayoutBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.model.GroupApps
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class GroupAppsViewHolder(
    private val binding: GroupAppsLayoutBinding,
    root: View
) : BaseViewHolder<GroupApps>(root), View.OnClickListener {

    companion object {
        val CREATOR = Creator<GroupApps> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = GroupAppsLayoutBinding.inflate(inflater, outer.root, true)
            GroupAppsViewHolder(inner, outer.root)
        }
    }

    init {
        root.setOnClickListener(this)
    }

    private val txtGroupName get() = binding.groupName
    private val txtNumberApps get() = binding.numberApps
    private val btnLock get() = binding.btnLock
    private val btnHide get() = binding.btnHide
    private val btnTimeout get() = binding.btnTimeout

    override fun onBind() {
        data?.let {
            txtGroupName.text = it.groupName
            txtNumberApps.text = "${it.pkgs.size} Apps"
        }
    }

    override fun onClick(v: View) {

    }
}