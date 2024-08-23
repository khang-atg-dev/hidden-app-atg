package moe.shizuku.manager.focus

import android.view.LayoutInflater
import android.view.ViewGroup
import moe.shizuku.manager.databinding.FocusItemLayoutBinding
import moe.shizuku.manager.model.Focus
import moe.shizuku.manager.utils.formatMilliseconds
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class FocusViewHolder(
    private val binding: FocusItemLayoutBinding,
) : BaseViewHolder<Focus>(binding.root) {

    companion object {
        val CREATOR = Creator<Focus> { inflater: LayoutInflater, parent: ViewGroup? ->
            val binding = FocusItemLayoutBinding.inflate(inflater, parent, false)
            FocusViewHolder(binding)
        }
    }

    init {
        binding.root.isEnabled = false
    }


    override fun onBind() {
        data?.let {
            binding.root.scrollTo(0, 0)
            binding.name.let { name ->
                name.text = it.name
                name.setOnClickListener {
                    (adapter?.listener as FocusCallback).onEditName(data.id)
                }
            }
            binding.txtMinute.let { min ->
                min.text = it.time.formatMilliseconds(context)
                min.setOnClickListener {
                    (adapter?.listener as FocusCallback).onOpenTimePicker(data.id, data.time)
                }
            }
            binding.delete.setOnClickListener {
                (adapter?.listener as FocusCallback).onDelete(data.id)
            }
            binding.startFocus.setOnClickListener {
                (adapter?.listener as FocusCallback).onStart(data.id, data.time, data.name)
            }
        }
    }
}
