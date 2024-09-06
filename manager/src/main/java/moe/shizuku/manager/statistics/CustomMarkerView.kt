package moe.shizuku.manager.statistics

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.card.MaterialCardView
import moe.shizuku.manager.R
import java.text.DateFormatSymbols
import java.util.concurrent.TimeUnit

class CustomMarkerView(
    context: Context,
    private val segmentTime: SegmentTime,
) : MarkerView(context, R.layout.custom_marker_view) {
    private val label: TextView = findViewById(R.id.label)
    private val txtInfo: TextView = findViewById(R.id.txt_info)
    private val markerView: MaterialCardView = findViewById(R.id.marker_container)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            if (it.y > 0) {
                markerView.visibility = VISIBLE
                label.text =
                    if (segmentTime == SegmentTime.WEEK) {
                        DateFormatSymbols().weekdays[it.x.toInt()]
                    } else {
                        it.x.toInt().toString()
                    }
                val hours = TimeUnit.MILLISECONDS.toHours(it.y.toLong())
                val minutes = TimeUnit.MILLISECONDS.toMinutes(it.y.toLong()) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(it.y.toLong()) % 60
                txtInfo.text = when {
                    hours > 0 -> String.format("%.1f h", it.y / (60 * 60 * 1000))
                    minutes > 0 -> "${minutes}m"
                    else -> "${seconds}s"
                }
            } else {
                markerView.visibility = GONE
            }
        }
        super.refreshContent(e, highlight);
    }

    private var mOffset: MPPointF? = null
    override fun getOffset(): MPPointF? {
        if (mOffset == null) {
            mOffset = MPPointF(-(width / 2).toFloat(), -height.toFloat())
        }
        return mOffset
    }
}