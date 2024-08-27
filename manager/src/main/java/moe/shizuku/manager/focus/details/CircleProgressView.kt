package moe.shizuku.manager.focus.details

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import moe.shizuku.manager.AppConstants.DEFAULT_TIME_FOCUS
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.utils.formatMilliseconds
import kotlin.math.min

class CircleProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var totalTimeMillis: Long = DEFAULT_TIME_FOCUS
    private var remainingTimeMillis: Long = totalTimeMillis

    private val circlePaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.transparent)
        style = Paint.Style.STROKE
        strokeWidth = 30f
        isAntiAlias = true
    }

    private val progressPaint: Paint = Paint().apply {
        color =
            ShizukuSettings.getColorCurrentTask()?.let {
                Color.parseColor(it)
            } ?: ContextCompat.getColor(context, android.R.color.holo_red_light)
        style = Paint.Style.STROKE
        strokeWidth = 30f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint: Paint = Paint().apply {
        color = ShizukuSettings.getColorCurrentTask()?.let {
            Color.parseColor(it)
        } ?: fetchDefaultTextColor(context)
        textSize = 64f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2
        val centerY = height / 2
        val radius = min(centerX, centerY) - circlePaint.strokeWidth

        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        // Calculate sweep angle
        val sweepAngle = 360f * (remainingTimeMillis.toFloat() / totalTimeMillis)

        // Draw progress arc
        canvas.drawArc(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            -90f,
            sweepAngle,
            false,
            progressPaint
        )

        // Draw remaining time text
        val timeText = remainingTimeMillis.formatMilliseconds(context)
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2

        canvas.drawText(timeText, centerX, textY, textPaint)
    }

    fun initTimeMillis(totalTimeMillis: Long, remainingTimeMillis: Long) {
        this.totalTimeMillis = totalTimeMillis
        this.remainingTimeMillis = remainingTimeMillis
        invalidate() // Redraw the view
    }

    fun updateProgress(remainingTimeMillis: Long) {
        this.remainingTimeMillis = remainingTimeMillis
        invalidate() // Redraw the view
    }

    fun updateColor(colorHex: String) {
        progressPaint.color = Color.parseColor(colorHex)
        textPaint.color = Color.parseColor(colorHex)
        invalidate()
    }

    // Function to fetch the default TextView text color from the theme
    private fun fetchDefaultTextColor(context: Context): Int {
        val textView = AppCompatTextView(context)
        return textView.currentTextColor
    }
}
