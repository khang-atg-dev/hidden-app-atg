package moe.shizuku.manager.statistics.raw

import android.view.LayoutInflater
import android.view.ViewGroup
import moe.shizuku.manager.AppConstants.FORMAT_HMS
import moe.shizuku.manager.databinding.RawStatisticViewHolderBinding
import moe.shizuku.manager.databinding.SectionViewHolderBinding
import moe.shizuku.manager.model.StatisticFocus
import moe.shizuku.manager.utils.formatMilliseconds
import moe.shizuku.manager.utils.getTimeAsString
import moe.shizuku.manager.utils.toDate
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator
import rikka.recyclerview.IdBasedRecyclerViewAdapter
import rikka.recyclerview.IndexCreatorPool
import java.util.Calendar

class RawStatisticAdapter : IdBasedRecyclerViewAdapter(ArrayList()) {
    init {
        setHasStableIds(true)
    }

    override fun onCreateCreatorPool(): IndexCreatorPool {
        return IndexCreatorPool()
    }

    fun updateData(data: Map<String, List<StatisticFocus>>?) {
        clear()
        if (!data.isNullOrEmpty()) {
            data.forEach {
                addItem(
                    SectionViewHolder.CREATOR,
                    it.key,
                    it.key.hashCode().toLong()
                )
                it.value.forEach { i ->
                    addItem(
                        RawStatisticViewHolder.CREATOR,
                        i,
                        i.id.hashCode().toLong()
                    )
                }
            }
        }
        notifyDataSetChanged()
    }
}

class RawStatisticViewHolder(
    private val binding: RawStatisticViewHolderBinding,
) : BaseViewHolder<StatisticFocus>(binding.root) {
    companion object {
        val CREATOR = Creator<StatisticFocus> { inflater: LayoutInflater, parent: ViewGroup? ->
            val binding = RawStatisticViewHolderBinding.inflate(inflater, parent, false)
            RawStatisticViewHolder(binding)
        }
    }

    init {
        binding.root.isEnabled = false
    }

    override fun onBind() {
        super.onBind()
        binding.name.text = data.name
        var sum = 0L
        data.timeline.forEach {
            val calender = Calendar.getInstance().apply {
                setFirstDayOfWeek(Calendar.SUNDAY)
            }
            val start = it.startTime.toDate() ?: return@forEach
            val end = it.endTime.toDate() ?: return@forEach
            val startTimeInMillis = calender.apply {
                time = start
            }.timeInMillis
            val endInTimeInMills = calender.apply {
                time = end
            }.timeInMillis
            sum += endInTimeInMills - startTimeInMillis
        }
        binding.duration.text = sum.formatMilliseconds(itemView.context)
        binding.time.text =
            data.startTime.toDate()?.getTimeAsString(FORMAT_HMS) + "~" + data.endTime.toDate()
                ?.getTimeAsString(FORMAT_HMS)
    }
}

class SectionViewHolder(
    private val binding: SectionViewHolderBinding,
) : BaseViewHolder<String>(binding.root) {
    companion object {
        val CREATOR = Creator<String> { inflater: LayoutInflater, parent: ViewGroup? ->
            val binding = SectionViewHolderBinding.inflate(inflater, parent, false)
            SectionViewHolder(binding)
        }
    }

    init {
        binding.root.isEnabled = false
    }

    override fun onBind() {
        super.onBind()
        binding.text.text = data
    }
}