package moe.shizuku.manager.statistics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import moe.shizuku.manager.R
import moe.shizuku.manager.utils.BaseAdapter
import moe.shizuku.manager.utils.BaseHolder
import moe.shizuku.manager.utils.formatMillisecondsToSimple

class StatisticAdapter(
    inflater: LayoutInflater
) : BaseAdapter<StatisticItem, BaseHolder<StatisticItem>>(inflater) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticViewHolder {
        return StatisticViewHolder(inflater.inflate(R.layout.focus_progress_bar, parent, false))
    }

    inner class StatisticViewHolder(itemView: View) : BaseHolder<StatisticItem>(itemView) {
        override fun bind(data: StatisticItem?, position: Int) {
            data?.let {
                itemView.findViewById<TextView>(R.id.txtName).text = it.name
                itemView.findViewById<TextView>(R.id.txtTime).text =
                    it.time.formatMillisecondsToSimple()
                itemView.findViewById<TextView>(R.id.statistic_percent).text =
                    String.format(itemView.context.getString(R.string.percent_value), it.percentage)
                itemView.findViewById<TextView>(R.id.statistic_number).text =
                    String.format(
                        itemView.context.getString(R.string.number_focus),
                        it.numberOfFocuses
                    )
                itemView.findViewById<LinearProgressIndicator>(R.id.progressBar).apply {
                    progress = it.percentage.toInt()
                    setIndicatorColor(it.color)
                }
            }
        }
    }
}

data class StatisticItem(
    val name: String,
    val time: Long,
    val percentage: Float,
    val numberOfFocuses: Int,
    val color: Int,
)
