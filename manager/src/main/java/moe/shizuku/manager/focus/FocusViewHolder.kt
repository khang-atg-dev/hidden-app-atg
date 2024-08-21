package moe.shizuku.manager.focus

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import moe.shizuku.manager.databinding.FocusItemLayoutBinding
import moe.shizuku.manager.model.Focus
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator
import java.util.concurrent.TimeUnit

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
                min.text = convertTime(it.time)
                min.setOnClickListener {
                    (adapter?.listener as FocusCallback).onOpenTimePicker(data.time)
                }
            }
            binding.delete.setOnClickListener {
                (adapter?.listener as FocusCallback).onDelete(data.id)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun convertTime(time: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
        return String.format("%d minute%s", minutes, "".takeIf { minutes == 1L } ?: "s")
    }
}
