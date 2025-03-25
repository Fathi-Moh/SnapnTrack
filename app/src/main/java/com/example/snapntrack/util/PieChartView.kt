package com.example.snapntrack.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

class PieChartView  @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var itemMap: Map<String, Int> = emptyMap()
    private var totalItems: Int = 0

    private val colors = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN,
        Color.MAGENTA, Color.GRAY, Color.DKGRAY, Color.LTGRAY
    )

    fun setData(itemMap: Map<String, Int>, totalItems: Int) {
        this.itemMap = itemMap
        this.totalItems = totalItems
        invalidate() // Redraw the view
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (itemMap.isEmpty() || totalItems == 0) return

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = min(width, height) / 2 - 50f
        val centerX = width / 2
        val centerY = height / 2

        var startAngle = 0f
        var colorIndex = 0
        var totalCoveredPercentage = 0f
        var totalCoveredItems = 0

        for ((itemName, frequency) in itemMap) {
            if (frequency > 1) {
                val sweepAngle = (frequency.toFloat() / totalItems) * 360f

                paint.color = colors[colorIndex % colors.size]
                colorIndex++

                canvas.drawArc(
                    centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                    startAngle, sweepAngle, true, paint
                )

                val percentage = (frequency.toFloat() / totalItems) * 100
                totalCoveredPercentage += percentage
                totalCoveredItems += frequency

                val textAngle = startAngle + sweepAngle / 2
                val textRadius = radius * 0.75f

                val angleRad = Math.toRadians(textAngle.toDouble())

                val textX = centerX + cos(angleRad).toFloat() * textRadius
                val textY = centerY + sin(angleRad).toFloat() * textRadius

                paint.color = Color.WHITE
                paint.textSize = 26f
                canvas.drawText("$itemName ($frequency, ${"%.2f".format(percentage)}%)", textX, textY, paint)

                startAngle += sweepAngle
            }
        }

        val remainingItems = totalItems - totalCoveredItems
        val remainingPercentage = 100f - totalCoveredPercentage
        if (remainingPercentage > 0) {
            val sweepAngle = (remainingPercentage / 100f) * 360f

            paint.color = Color.rgb(70, 70, 70)

            canvas.drawArc(
                centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                startAngle, sweepAngle, true, paint
            )

            val textAngle = startAngle + sweepAngle / 2
            val textRadius = radius * 0.75f

            val angleRad = Math.toRadians(textAngle.toDouble())

            val textX = centerX + cos(angleRad).toFloat() * textRadius
            val textY = centerY + sin(angleRad).toFloat() * textRadius

            paint.color = Color.WHITE
            paint.textSize = 26f
            canvas.drawText("Others ($remainingItems,${"%.2f".format(remainingPercentage)}%)", textX, textY, paint)
        }
    }


}