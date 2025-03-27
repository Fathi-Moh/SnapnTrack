package com.example.snapntrack.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var itemMap: Map<String, Int> = emptyMap()
    private var totalItems: Int = 0

    private val colors = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN,
        Color.MAGENTA, Color.GRAY, Color.DKGRAY, Color.LTGRAY
    )

    // Legend parameters
    private val legendRectSize = 46f
    private val legendTextSize = 38f
    private val legendSpacing = 15f
    private val legendMargin = 20f
    private val legendItemHeight = 50f

    fun setData(itemMap: Map<String, Int>, totalItems: Int) {
        this.itemMap = itemMap
        this.totalItems = totalItems
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (itemMap.isEmpty() || totalItems == 0) return

        val width = width.toFloat()
        val height = height.toFloat()

        // Calculate available space for pie chart (accounting for legend space)
        val legendHeight = calculateLegendHeight()
        val chartHeight = height - legendHeight - legendMargin * 2

        val radius = min(width, chartHeight) / 2 - 50f
        val centerX = width / 2
        val centerY = radius + legendMargin

        // Draw pie chart
        var startAngle = 0f
        var colorIndex = 0
        var totalCoveredPercentage = 0f
        var totalCoveredItems = 0

        val legendItems = mutableListOf<Pair<Int, String>>()

        for ((itemName, frequency) in itemMap) {
            if (frequency > 1) {
                val sweepAngle = (frequency.toFloat() / totalItems) * 360f
                val color = colors[colorIndex % colors.size]

                legendItems.add(Pair(color, "$itemName ($frequency, ${"%.2f".format(frequency.toFloat() / totalItems * 100)}%)"))
                colorIndex++

                paint.color = color
                canvas.drawArc(
                    centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                    startAngle, sweepAngle, true, paint
                )

                totalCoveredPercentage += (frequency.toFloat() / totalItems) * 100
                totalCoveredItems += frequency
                startAngle += sweepAngle
            }
        }

        // Draw remaining items as "Others"
        val remainingItems = totalItems - totalCoveredItems
        if (remainingItems > 0) {
            val remainingPercentage = 100f - totalCoveredPercentage
            val sweepAngle = (remainingPercentage / 100f) * 360f
            val othersColor = Color.rgb(70, 70, 70)

            legendItems.add(Pair(othersColor, "Others ($remainingItems, ${"%.2f".format(remainingPercentage)}%)"))

            paint.color = othersColor
            canvas.drawArc(
                centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                startAngle, sweepAngle, true, paint
            )
        }

        // Draw vertical legend at the bottom
        drawVerticalLegend(canvas, legendItems, width, height, legendHeight)
    }

    private fun calculateLegendHeight(): Float {
        val itemCount = itemMap.count { it.value > 1 } + if (totalItems > itemMap.values.sum()) 1 else 0
        return itemCount * legendItemHeight + legendMargin
    }

    private fun drawVerticalLegend(canvas: Canvas, legendItems: List<Pair<Int, String>>, width: Float, height: Float, legendHeight: Float) {
        val legendStartY = height - legendHeight + legendMargin
        val legendLeft = legendMargin

        paint.textSize = legendTextSize
        val textMetrics = paint.fontMetrics
        val textHeight = textMetrics.descent - textMetrics.ascent
        val textBaseline = legendRectSize / 2 + (textHeight / 2) - textMetrics.descent

        legendItems.forEachIndexed { index, (color, text) ->
            val itemTop = legendStartY + index * legendItemHeight

            paint.color = color
            canvas.drawRect(
                legendLeft,
                itemTop,
                legendLeft + legendRectSize,
                itemTop + legendRectSize,
                paint
            )

            // Draw text
            paint.color = Color.WHITE
            canvas.drawText(
                text,
                legendLeft + legendRectSize + legendSpacing,
                itemTop + textBaseline,
                paint
            )
        }
    }
}