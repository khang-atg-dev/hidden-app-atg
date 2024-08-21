package moe.shizuku.manager.focus

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.databinding.FocusItemLayoutBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.model.Focus
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator
import java.util.concurrent.TimeUnit

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

        }
    }

    override fun onClick(v: View) {
    }

    @SuppressLint("DefaultLocale")
    private fun convertTime(time: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
        return String.format("%d minute%s", minutes, "".takeIf { minutes == 1L } ?: "s")
    }
}
