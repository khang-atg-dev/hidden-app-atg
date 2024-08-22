package moe.shizuku.manager.focus.details
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import moe.shizuku.manager.AppConstants.DEFAULT_TIME_FOCUS
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
        color = ContextCompat.getColor(context, android.R.color.holo_red_light)
        style = Paint.Style.STROKE
        strokeWidth = 30f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.black)
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
        val minutes = (remainingTimeMillis / 1000) / 60
        val seconds = (remainingTimeMillis / 1000) % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2

        canvas.drawText(timeText, centerX, textY, textPaint)
    }

    fun setTotalTimeMillis(totalTimeMillis: Long) {
        this.totalTimeMillis = totalTimeMillis
        this.remainingTimeMillis = totalTimeMillis
        invalidate() // Redraw the view
    }

    fun updateProgress(remainingTimeMillis: Long) {
        this.remainingTimeMillis = remainingTimeMillis
        invalidate() // Redraw the view
    }
}
