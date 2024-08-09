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
    private val btnClose get() = binding.closeBtn
    private val listener get() = (adapter.listener as HomeCallback)
    private val mapTimeout = mapOf(
        0L to "0s",
        60000L to "60s",
        300000L to "5m",
        1800000L to "30m",
        3600000L to "1h"
    )

    override fun onBind() {
        data?.let {
            txtGroupName.text = it.groupName
            txtNumberApps.text = "${it.pkgs.size} Apps"
            btnClose.setOnClickListener {
                listener.onDeleteGroup(data.groupName)
            }
            btnTimeout.text = mapTimeout[data.timeOut]
            btnTimeout.setOnClickListener {
                listener.onEditTimeout(data.groupName)
            }
            btnHide.text = "Unhide".takeIf { data.isHidden } ?: "Hide"
            btnHide.setOnClickListener {
                listener.onActionHide(data.groupName)
            }
            btnLock.text = "Unlock".takeIf { data.isLocked } ?: "Lock"
            btnLock.setOnClickListener {
                listener.onActionLock(data.groupName)
            }
            binding.lockedIcon.visibility = if (it.isLocked) View.VISIBLE else View.GONE
            binding.hiddenIcon.visibility = if (it.isHidden) View.VISIBLE else View.GONE
        }
    }

    override fun onClick(v: View) {
        listener.onClickGroup(data.groupName)
    }
}