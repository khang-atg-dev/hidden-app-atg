package moe.shizuku.manager.hidden

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.R
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
            outer.root.setCardBackgroundColor(outer.root.context.getColor(R.color.home_card_background_color))
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
    private val listener get() = (adapter.listener as HiddenCallback)
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
                listener.onDeleteGroup(data.id)
            }
            btnTimeout.text = mapTimeout[data.timeOut]
            btnTimeout.setOnClickListener {
                listener.onEditTimeout(data.id)
            }
            btnHide.text = context.getString(R.string.unhide).takeIf { data.isHidden }
                ?: context.getString(R.string.hide)
            btnHide.setOnClickListener {
                listener.onActionHide(data.id)
            }
            btnLock.text = context.getString(R.string.unlock).takeIf { data.isLocked }
                ?: context.getString(R.string.lock)
            btnLock.setOnClickListener {
                listener.onActionLock(data.id)
            }
            binding.lockedIcon.visibility = if (it.isLocked) View.VISIBLE else View.GONE
            binding.hiddenIcon.visibility = if (it.isHidden) View.VISIBLE else View.GONE
        }
    }

    override fun onClick(v: View) {
        listener.onClickGroup(data.id)
    }
}