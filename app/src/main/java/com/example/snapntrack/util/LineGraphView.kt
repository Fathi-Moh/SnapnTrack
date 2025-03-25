package com.example.snapntrack.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class LineGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var entries: List<Pair<Float, Float>> = emptyList()
    private var labels: List<String> = emptyList()
    private var yMin: Float = 0f
    private var yMax: Float = 0f

    fun setData(entries: List<Pair<Float, Float>>, labels: List<String>, yMin: Float, yMax: Float) {
        this.entries = entries
        this.labels = labels
        this.yMin = yMin
        this.yMax = yMax
        invalidate() // Redraw the view
    }

@SuppressLint("DefaultLocale")
override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    if (entries.isEmpty()) return

    val width = width.toFloat()
    val height = height.toFloat()
    val padding = 50f
    val graphWidth = width - 2 * padding
    val graphHeight = height - 2 * padding
//
    paint.color = Color.BLACK
    paint.strokeWidth = 2f
    canvas.drawLine(padding, height - padding, width - padding, height - padding, paint) // X-axis
    canvas.drawLine(padding, height - padding, padding, padding, paint) // Y-axis

    paint.textSize = 24f
    val xStep = if (labels.size > 1) graphWidth / (labels.size - 1) else graphWidth

    for (i in labels.indices) {
        val label = labels[i]
        if (label.isEmpty()) continue

        val x = padding + i * xStep
        val textWidth = paint.measureText(label)
        val adjustedX = x - (textWidth / 2)
        canvas.drawText(label, adjustedX, height - padding + 30, paint)
    }

    // Draw Y-axis labels
    val yStep = graphHeight / 6
    for (i in 0..5) {
        val y = height - padding - i * yStep
        val value = (yMax - yMin) * (i / 5f)
        canvas.drawText(String.format("%.0f", value), padding - 40, y, paint)
    }

    paint.color = Color.BLUE
    paint.strokeWidth = 4f
    path.reset()

    val yScale = graphHeight / (yMax - yMin)

    for (i in entries.indices) {
        val x = padding + i * xStep
        val y = height - padding - (entries[i].second - yMin) * yScale

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }

        paint.color = Color.RED
        canvas.drawCircle(x, y, 8f, paint)
    }

    paint.color = Color.BLUE
}

}