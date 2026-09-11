package com.example.data.gemini

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.data.model.CandleStick
import com.example.data.model.MarketAsset
import com.example.data.model.TechnicalIndicators
import com.example.data.model.Timeframe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ChartImageRenderer {

    fun renderChartBitmap(
        asset: MarketAsset,
        timeframe: Timeframe,
        candles: List<CandleStick>,
        indicators: TechnicalIndicators,
        width: Int = 960,
        height: Int = 600
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#0B0E14") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (candles.isEmpty()) {
            return bitmap
        }

        // Layout bounds
        val paddingLeft = 20f
        val paddingRight = 95f
        val headerHeight = 70f
        val rsiPaneHeight = 110f
        val mainChartBottom = height - rsiPaneHeight - 20f
        val mainChartTop = headerHeight + 10f
        val chartWidth = width - paddingLeft - paddingRight

        // Paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E6ED")
            textSize = 24f
            isFakeBoldText = true
        }
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#90A4AE")
            textSize = 15f
        }
        val gridPaint = Paint().apply {
            color = Color.parseColor("#1B2230")
            strokeWidth = 1.2f
        }
        val bullPaint = Paint().apply {
            color = Color.parseColor("#00E676")
            style = Paint.Style.FILL
        }
        val bearPaint = Paint().apply {
            color = Color.parseColor("#FF3D57")
            style = Paint.Style.FILL
        }
        val bullWickPaint = Paint().apply {
            color = Color.parseColor("#00E676")
            strokeWidth = 2f
        }
        val bearWickPaint = Paint().apply {
            color = Color.parseColor("#FF3D57")
            strokeWidth = 2f
        }
        val ema9Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        val ema21Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9100")
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        val bbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A6572")
            strokeWidth = 1.8f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        val rsiLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E040FB")
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }

        // Header Render
        canvas.drawText("${asset.displayName} • ${timeframe.label}", paddingLeft, 38f, textPaint)
        val priceColor = if (asset.change24h >= 0) "#00E676" else "#FF3D57"
        val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(priceColor)
            textSize = 22f
            isFakeBoldText = true
        }
        val formattedPrice = String.format(Locale.US, "%.${asset.decimals}f", asset.currentPrice)
        val changeStr = String.format(Locale.US, "%+.2f%%", asset.change24h)
        canvas.drawText("$formattedPrice ($changeStr)", paddingLeft + 260f, 38f, pricePaint)

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.drawText("Generated for Gemini AI Vision Analysis • $dateStr", paddingLeft, 62f, subTextPaint)

        // Indicators Legend
        val legendY = 62f
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f }

        legendPaint.color = Color.parseColor("#00E5FF")
        canvas.drawText("EMA 9", width - 360f, legendY, legendPaint)
        legendPaint.color = Color.parseColor("#FF9100")
        canvas.drawText("EMA 21", width - 290f, legendY, legendPaint)
        legendPaint.color = Color.parseColor("#90A4AE")
        canvas.drawText("BB (20,2)", width - 215f, legendY, legendPaint)
        legendPaint.color = Color.parseColor("#E040FB")
        canvas.drawText("RSI (14)", width - 130f, legendY, legendPaint)

        // Select visible slice of candles
        val visibleCandles = candles.takeLast(min(candles.size, 65))
        val candleCount = visibleCandles.size
        val candleSpacing = chartWidth / candleCount
        val candleWidth = candleSpacing * 0.72f

        // High / Low for main chart scaling
        var maxPrice = visibleCandles.maxOf { it.high }
        var minPrice = visibleCandles.minOf { it.low }

        // Also incorporate BB if present
        val startIndex = candles.size - candleCount
        for (i in 0 until candleCount) {
            val bb = indicators.bollingerBands.getOrNull(startIndex + i)
            if (bb != null) {
                maxPrice = max(maxPrice, bb.upper)
                minPrice = min(minPrice, bb.lower)
            }
        }

        // Add 5% headroom
        val priceRange = max(0.0001, maxPrice - minPrice)
        val scaledMax = maxPrice + (priceRange * 0.05)
        val scaledMin = minPrice - (priceRange * 0.05)
        val chartHeight = mainChartBottom - mainChartTop

        fun priceToY(price: Double): Float {
            val ratio = (price - scaledMin) / (scaledMax - scaledMin)
            return (mainChartBottom - (ratio * chartHeight)).toFloat()
        }

        // Main Chart Background & Grid
        val mainChartBg = Paint().apply { color = Color.parseColor("#121620") }
        canvas.drawRect(paddingLeft, mainChartTop, paddingLeft + chartWidth, mainChartBottom, mainChartBg)

        // Draw 5 horizontal price gridlines
        val gridSteps = 4
        for (s in 0..gridSteps) {
            val p = scaledMin + ((scaledMax - scaledMin) * (s.toDouble() / gridSteps))
            val y = priceToY(p)
            canvas.drawLine(paddingLeft, y, paddingLeft + chartWidth, y, gridPaint)
            val pStr = String.format(Locale.US, "%.${asset.decimals}f", p)
            canvas.drawText(pStr, paddingLeft + chartWidth + 6f, y + 5f, subTextPaint)
        }

        // Support & Resistance reference lines if within price range
        val srPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }
        if (indicators.supportLevel1 in scaledMin..scaledMax) {
            srPaint.color = Color.parseColor("#4CAF50")
            val y = priceToY(indicators.supportLevel1)
            canvas.drawLine(paddingLeft, y, paddingLeft + chartWidth, y, srPaint)
            canvas.drawText("S1", paddingLeft + 5f, y - 4f, srPaint)
        }
        if (indicators.resistanceLevel1 in scaledMin..scaledMax) {
            srPaint.color = Color.parseColor("#EF5350")
            val y = priceToY(indicators.resistanceLevel1)
            canvas.drawLine(paddingLeft, y, paddingLeft + chartWidth, y, srPaint)
            canvas.drawText("R1", paddingLeft + 5f, y - 4f, srPaint)
        }

        // Paths for EMA and BB
        val ema9Path = Path()
        val ema21Path = Path()
        val bbUpperPath = Path()
        val bbLowerPath = Path()
        var ema9Started = false
        var ema21Started = false
        var bbStarted = false

        // Draw Candlesticks & Accumulate Lines
        for (i in 0 until candleCount) {
            val candle = visibleCandles[i]
            val globalIdx = startIndex + i
            val centerX = paddingLeft + (i * candleSpacing) + (candleSpacing / 2f)

            val openY = priceToY(candle.open)
            val closeY = priceToY(candle.close)
            val highY = priceToY(candle.high)
            val lowY = priceToY(candle.low)

            // Draw Wick
            val isBull = candle.isBullish
            val wickPaint = if (isBull) bullWickPaint else bearWickPaint
            canvas.drawLine(centerX, highY, centerX, lowY, wickPaint)

            // Draw Body
            val bodyPaint = if (isBull) bullPaint else bearPaint
            val topBody = min(openY, closeY)
            val bottomBody = max(openY, closeY).coerceAtLeast(topBody + 2f)
            val left = centerX - (candleWidth / 2f)
            val right = centerX + (candleWidth / 2f)
            canvas.drawRect(left, topBody, right, bottomBody, bodyPaint)

            // EMA 9 point
            val e9 = indicators.ema9.getOrNull(globalIdx)
            if (e9 != null) {
                val y = priceToY(e9)
                if (!ema9Started) {
                    ema9Path.moveTo(centerX, y)
                    ema9Started = true
                } else {
                    ema9Path.lineTo(centerX, y)
                }
            }

            // EMA 21 point
            val e21 = indicators.ema21.getOrNull(globalIdx)
            if (e21 != null) {
                val y = priceToY(e21)
                if (!ema21Started) {
                    ema21Path.moveTo(centerX, y)
                    ema21Started = true
                } else {
                    ema21Path.lineTo(centerX, y)
                }
            }

            // Bollinger bands
            val bb = indicators.bollingerBands.getOrNull(globalIdx)
            if (bb != null) {
                val uy = priceToY(bb.upper)
                val ly = priceToY(bb.lower)
                if (!bbStarted) {
                    bbUpperPath.moveTo(centerX, uy)
                    bbLowerPath.moveTo(centerX, ly)
                    bbStarted = true
                } else {
                    bbUpperPath.lineTo(centerX, uy)
                    bbLowerPath.lineTo(centerX, ly)
                }
            }
        }

        // Stroke overlays
        if (bbStarted) {
            canvas.drawPath(bbUpperPath, bbPaint)
            canvas.drawPath(bbLowerPath, bbPaint)
        }
        if (ema9Started) canvas.drawPath(ema9Path, ema9Paint)
        if (ema21Started) canvas.drawPath(ema21Path, ema21Paint)

        // Current Price Line
        val currentPriceY = priceToY(visibleCandles.last().close)
        val currentLinePaint = Paint().apply {
            color = Color.parseColor(priceColor)
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        canvas.drawLine(paddingLeft, currentPriceY, paddingLeft + chartWidth, currentPriceY, currentLinePaint)

        // RSI Sub-Pane
        val rsiTop = mainChartBottom + 12f
        val rsiBottom = height - 12f
        val rsiHeight = rsiBottom - rsiTop
        val rsiBgPaint = Paint().apply { color = Color.parseColor("#10131B") }
        canvas.drawRect(paddingLeft, rsiTop, paddingLeft + chartWidth, rsiBottom, rsiBgPaint)

        fun rsiToY(rsi: Double): Float {
            val clamped = rsi.coerceIn(0.0, 100.0)
            return (rsiBottom - ((clamped / 100.0) * rsiHeight)).toFloat()
        }

        // 70 and 30 reference lines
        val rsiRefPaint = Paint().apply {
            color = Color.parseColor("#37474F")
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        val y70 = rsiToY(70.0)
        val y30 = rsiToY(30.0)
        canvas.drawLine(paddingLeft, y70, paddingLeft + chartWidth, y70, rsiRefPaint)
        canvas.drawLine(paddingLeft, y30, paddingLeft + chartWidth, y30, rsiRefPaint)
        canvas.drawText("70", paddingLeft + chartWidth + 6f, y70 + 4f, subTextPaint)
        canvas.drawText("30", paddingLeft + chartWidth + 6f, y30 + 4f, subTextPaint)

        // Shaded zone between 30 and 70
        val rsiZonePaint = Paint().apply {
            color = Color.parseColor("#181B26")
            style = Paint.Style.FILL
        }
        canvas.drawRect(paddingLeft, y70, paddingLeft + chartWidth, y30, rsiZonePaint)

        // Draw RSI Curve
        val rsiPath = Path()
        var rsiStarted = false
        for (i in 0 until candleCount) {
            val globalIdx = startIndex + i
            val centerX = paddingLeft + (i * candleSpacing) + (candleSpacing / 2f)
            val rVal = indicators.rsi14.getOrNull(globalIdx)
            if (rVal != null) {
                val y = rsiToY(rVal)
                if (!rsiStarted) {
                    rsiPath.moveTo(centerX, y)
                    rsiStarted = true
                } else {
                    rsiPath.lineTo(centerX, y)
                }
            }
        }
        if (rsiStarted) {
            canvas.drawPath(rsiPath, rsiLinePaint)
        }

        // Current RSI label
        val curRsi = indicators.currentRsi ?: 50.0
        val curRsiText = String.format(Locale.US, "RSI: %.1f", curRsi)
        val rsiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E040FB")
            textSize = 14f
            isFakeBoldText = true
        }
        canvas.drawText(curRsiText, paddingLeft + 10f, rsiTop + 20f, rsiLabelPaint)

        return bitmap
    }
}
