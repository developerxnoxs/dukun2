package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandleStick
import com.example.data.model.MarketAsset
import com.example.data.model.TechnicalIndicators
import com.example.ui.theme.BearRed
import com.example.ui.theme.BollingerLine
import com.example.ui.theme.BullGreen
import com.example.ui.theme.ChartGridLine
import com.example.ui.theme.CrosshairLine
import com.example.ui.theme.Ema21Orange
import com.example.ui.theme.Ema9Cyan
import com.example.ui.theme.RsiPurple
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class SubChartType {
    RSI, MACD, VOLUME, NONE
}

@Composable
fun InteractiveCandlestickChart(
    asset: MarketAsset,
    candles: List<CandleStick>,
    indicators: TechnicalIndicators,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(SurfaceDark, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Memuat data grafik candlestick...",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        return
    }

    // Interactive State
    var showEma by remember { mutableStateOf(true) }
    var showBollinger by remember { mutableStateOf(true) }
    var subChart by remember { mutableStateOf(SubChartType.RSI) }

    var candleSpacing by remember { mutableFloatStateOf(16f) }
    var scrollOffsetIndex by remember { mutableIntStateOf(0) }
    var crosshairCandleIndex by remember { mutableStateOf<Int?>(null) }
    var crosshairTouchY by remember { mutableFloatStateOf(-1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("candlestick_chart_container")
    ) {
        // Platform & TradingView Consensus Live Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(BullGreen, androidx.compose.foundation.shape.CircleShape)
                )
                Text(
                    text = "PLATFORM: ${asset.platform.badgeLabel}",
                    color = BullGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${asset.tvSymbol})",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            asset.tvRating?.let { rating ->
                val ratingColor = when (rating.action) {
                    com.example.data.model.SignalAction.STRONG_BUY, com.example.data.model.SignalAction.BUY -> BullGreen
                    com.example.data.model.SignalAction.STRONG_SELL, com.example.data.model.SignalAction.SELL -> BearRed
                    com.example.data.model.SignalAction.NEUTRAL -> TextMuted
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ratingColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ratingColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "TV: ${rating.action.label} (${String.format(Locale.US, "%+.2f", rating.score)})",
                        color = ratingColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Indicator Controls Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = showEma,
                    onClick = { showEma = !showEma },
                    label = { Text("EMA 9/21", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Ema9Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = Ema9Cyan
                    ),
                    modifier = Modifier.testTag("toggle_ema")
                )
                FilterChip(
                    selected = showBollinger,
                    onClick = { showBollinger = !showBollinger },
                    label = { Text("Bollinger", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BollingerLine.copy(alpha = 0.2f),
                        selectedLabelColor = BollingerLine
                    ),
                    modifier = Modifier.testTag("toggle_bollinger")
                )
            }

            // Sub-chart Toggle (RSI / MACD / Volume)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(SubChartType.RSI, SubChartType.MACD, SubChartType.VOLUME).forEach { type ->
                    val isSelected = subChart == type
                    Surface(
                        onClick = { subChart = if (isSelected) SubChartType.NONE else type },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) SurfaceCard else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder) else null,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = type.name,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Ema9Cyan else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Crosshair HUD Info Bar
        val selectedCandle = crosshairCandleIndex?.let { idx ->
            candles.getOrNull(idx)
        } ?: candles.lastOrNull()

        selectedCandle?.let { c ->
            val idx = crosshairCandleIndex ?: (candles.size - 1)
            val dateStr = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(c.timestamp))
            val change = ((c.close - c.open) / c.open) * 100.0
            val isBull = c.isBullish

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "O: ${String.format(Locale.US, "%.${asset.decimals}f", c.open)}",
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "H: ${String.format(Locale.US, "%.${asset.decimals}f", c.high)}",
                        color = BullGreen,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "L: ${String.format(Locale.US, "%.${asset.decimals}f", c.low)}",
                        color = BearRed,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "C: ${String.format(Locale.US, "%.${asset.decimals}f", c.close)} (${String.format(Locale.US, "%+.2f%%", change)})",
                        color = if (isBull) BullGreen else BearRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Chart Canvas with Gesture Detection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        candleSpacing = (candleSpacing * zoom).coerceIn(8f, 36f)
                        val deltaCandles = (pan.x / candleSpacing).toInt()
                        scrollOffsetIndex = (scrollOffsetIndex - deltaCandles).coerceAtLeast(0)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            crosshairTouchY = offset.y
                            // Will calculate candle index in draw scope
                        },
                        onTap = {
                            crosshairCandleIndex = null
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            crosshairTouchY = offset.y
                        },
                        onDrag = { change, _ ->
                            crosshairTouchY = change.position.y
                        },
                        onDragEnd = {
                            // keep crosshair for a moment or tap to dismiss
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("candlestick_canvas")
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val priceScaleWidth = 72.dp.toPx()
                val mainPlotWidth = canvasWidth - priceScaleWidth

                // Sub-chart height allocation
                val subChartHeight = if (subChart != SubChartType.NONE) 70.dp.toPx() else 0f
                val subChartSpacing = if (subChart != SubChartType.NONE) 12.dp.toPx() else 0f
                val mainChartHeight = canvasHeight - subChartHeight - subChartSpacing

                // Calculate visible candle slice
                val maxVisible = (mainPlotWidth / candleSpacing).toInt().coerceAtLeast(10)
                val totalCandles = candles.size

                val startIdx = (totalCandles - maxVisible - scrollOffsetIndex).coerceIn(0, totalCandles - 1)
                val endIdx = (startIdx + maxVisible).coerceAtMost(totalCandles)
                val visibleCandles = candles.subList(startIdx, endIdx)

                if (visibleCandles.isEmpty()) return@Canvas

                // Detect crosshair position if touched
                // Using last touch X
                // Main Chart Price Extents
                var maxP = visibleCandles.maxOf { it.high }
                var minP = visibleCandles.minOf { it.low }

                if (showBollinger) {
                    for (i in startIdx until endIdx) {
                        indicators.bollingerBands.getOrNull(i)?.let { bb ->
                            maxP = max(maxP, bb.upper)
                            minP = min(minP, bb.lower)
                        }
                    }
                }

                val pRange = max(0.0001, maxP - minP)
                val paddedMax = maxP + (pRange * 0.05)
                val paddedMin = minP - (pRange * 0.05)

                fun priceToY(price: Double): Float {
                    val ratio = (price - paddedMin) / (paddedMax - paddedMin)
                    return (mainChartHeight - (ratio * mainChartHeight)).toFloat()
                }

                // 1. Draw Grid Lines & Price Axis Labels
                val gridSteps = 4
                for (step in 0..gridSteps) {
                    val p = paddedMin + ((paddedMax - paddedMin) * (step.toDouble() / gridSteps))
                    val y = priceToY(p)
                    drawLine(
                        color = ChartGridLine,
                        start = Offset(0f, y),
                        end = Offset(mainPlotWidth, y),
                        strokeWidth = 1f
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        String.format(Locale.US, "%.${asset.decimals}f", p),
                        mainPlotWidth + 8f,
                        y + 4f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#64748B")
                            textSize = 24f
                        }
                    )
                }

                // 2. Draw Candlesticks and Overlays
                val ema9Path = Path()
                val ema21Path = Path()
                val bbUpperPath = Path()
                val bbLowerPath = Path()
                var ema9Init = false
                var ema21Init = false
                var bbInit = false

                val candleBodyWidth = (candleSpacing * 0.68f).coerceAtLeast(2f)

                for (i in visibleCandles.indices) {
                    val candle = visibleCandles[i]
                    val globalIndex = startIdx + i
                    val x = (i * candleSpacing) + (candleSpacing / 2f)

                    val openY = priceToY(candle.open)
                    val closeY = priceToY(candle.close)
                    val highY = priceToY(candle.high)
                    val lowY = priceToY(candle.low)

                    val isBull = candle.isBullish
                    val candleColor = if (isBull) BullGreen else BearRed

                    // Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(x, highY),
                        end = Offset(x, lowY),
                        strokeWidth = 1.5f
                    )

                    // Body
                    val topY = min(openY, closeY)
                    val bottomY = max(openY, closeY).coerceAtLeast(topY + 1.5f)
                    drawRect(
                        color = candleColor,
                        topLeft = Offset(x - (candleBodyWidth / 2f), topY),
                        size = Size(candleBodyWidth, bottomY - topY)
                    )

                    // EMA 9 & 21
                    if (showEma) {
                        indicators.ema9.getOrNull(globalIndex)?.let { e9 ->
                            val y = priceToY(e9)
                            if (!ema9Init) {
                                ema9Path.moveTo(x, y)
                                ema9Init = true
                            } else {
                                ema9Path.lineTo(x, y)
                            }
                        }
                        indicators.ema21.getOrNull(globalIndex)?.let { e21 ->
                            val y = priceToY(e21)
                            if (!ema21Init) {
                                ema21Path.moveTo(x, y)
                                ema21Init = true
                            } else {
                                ema21Path.lineTo(x, y)
                            }
                        }
                    }

                    // Bollinger
                    if (showBollinger) {
                        indicators.bollingerBands.getOrNull(globalIndex)?.let { bb ->
                            val uy = priceToY(bb.upper)
                            val ly = priceToY(bb.lower)
                            if (!bbInit) {
                                bbUpperPath.moveTo(x, uy)
                                bbLowerPath.moveTo(x, ly)
                                bbInit = true
                            } else {
                                bbUpperPath.lineTo(x, uy)
                                bbLowerPath.lineTo(x, ly)
                            }
                        }
                    }
                }

                // Render Overlays
                if (showBollinger && bbInit) {
                    drawPath(
                        path = bbUpperPath,
                        color = BollingerLine.copy(alpha = 0.7f),
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    )
                    drawPath(
                        path = bbLowerPath,
                        color = BollingerLine.copy(alpha = 0.7f),
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    )
                }

                if (showEma) {
                    if (ema9Init) drawPath(path = ema9Path, color = Ema9Cyan, style = Stroke(width = 2f))
                    if (ema21Init) drawPath(path = ema21Path, color = Ema21Orange, style = Stroke(width = 2f))
                }

                // Current Price Live Line
                val currentY = priceToY(visibleCandles.last().close)
                drawLine(
                    color = if (asset.change24h >= 0) BullGreen else BearRed,
                    start = Offset(0f, currentY),
                    end = Offset(mainPlotWidth, currentY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )

                // Sub-chart rendering (RSI / MACD / Volume)
                if (subChart != SubChartType.NONE) {
                    val subTop = mainChartHeight + subChartSpacing
                    val subBottom = canvasHeight
                    val subHeight = subBottom - subTop

                    // Sub-chart background
                    drawRect(
                        color = Color(0xFF0F131C),
                        topLeft = Offset(0f, subTop),
                        size = Size(mainPlotWidth, subHeight)
                    )

                    when (subChart) {
                        SubChartType.RSI -> {
                            // RSI 70/30 lines
                            fun rsiY(rsiVal: Double): Float {
                                val clamped = rsiVal.coerceIn(0.0, 100.0)
                                return (subBottom - ((clamped / 100.0) * subHeight)).toFloat()
                            }
                            val y70 = rsiY(70.0)
                            val y30 = rsiY(30.0)

                            drawRect(
                                color = RsiPurple.copy(alpha = 0.08f),
                                topLeft = Offset(0f, y70),
                                size = Size(mainPlotWidth, y30 - y70)
                            )
                            drawLine(color = ChartGridLine, start = Offset(0f, y70), end = Offset(mainPlotWidth, y70), strokeWidth = 1f)
                            drawLine(color = ChartGridLine, start = Offset(0f, y30), end = Offset(mainPlotWidth, y30), strokeWidth = 1f)

                            val rsiPath = Path()
                            var rsiInit = false
                            for (i in visibleCandles.indices) {
                                val globalIndex = startIdx + i
                                val x = (i * candleSpacing) + (candleSpacing / 2f)
                                indicators.rsi14.getOrNull(globalIndex)?.let { rVal ->
                                    val ry = rsiY(rVal)
                                    if (!rsiInit) {
                                        rsiPath.moveTo(x, ry)
                                        rsiInit = true
                                    } else {
                                        rsiPath.lineTo(x, ry)
                                    }
                                }
                            }
                            if (rsiInit) {
                                drawPath(path = rsiPath, color = RsiPurple, style = Stroke(width = 2f))
                            }
                            drawContext.canvas.nativeCanvas.drawText("RSI (14)", 10f, subTop + 24f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#E040FB")
                                textSize = 22f
                            })
                        }
                        SubChartType.MACD -> {
                            // Zero line
                            val zeroY = subTop + (subHeight / 2f)
                            drawLine(color = ChartGridLine, start = Offset(0f, zeroY), end = Offset(mainPlotWidth, zeroY), strokeWidth = 1f)

                            for (i in visibleCandles.indices) {
                                val globalIndex = startIdx + i
                                val x = (i * candleSpacing) + (candleSpacing / 2f)
                                indicators.macd.getOrNull(globalIndex)?.let { macdPt ->
                                    val barH = (macdPt.histogram * 20f).toFloat().coerceIn(-subHeight / 2f, subHeight / 2f)
                                    val barColor = if (macdPt.histogram >= 0) BullGreen else BearRed
                                    drawLine(
                                        color = barColor,
                                        start = Offset(x, zeroY),
                                        end = Offset(x, zeroY - barH),
                                        strokeWidth = (candleBodyWidth * 0.7f).coerceAtLeast(1.5f)
                                    )
                                }
                            }
                            drawContext.canvas.nativeCanvas.drawText("MACD (12,26,9)", 10f, subTop + 24f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#2979FF")
                                textSize = 22f
                            })
                        }
                        SubChartType.VOLUME -> {
                            val maxVol = visibleCandles.maxOfOrNull { it.volume } ?: 1.0
                            for (i in visibleCandles.indices) {
                                val c = visibleCandles[i]
                                val x = (i * candleSpacing) + (candleSpacing / 2f)
                                val vH = ((c.volume / maxVol) * (subHeight * 0.9f)).toFloat()
                                val vColor = if (c.isBullish) BullGreen.copy(alpha = 0.5f) else BearRed.copy(alpha = 0.5f)
                                drawRect(
                                    color = vColor,
                                    topLeft = Offset(x - (candleBodyWidth / 2f), subBottom - vH),
                                    size = Size(candleBodyWidth, vH)
                                )
                            }
                            drawContext.canvas.nativeCanvas.drawText("Volume", 10f, subTop + 24f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#94A3B8")
                                textSize = 22f
                            })
                        }
                        SubChartType.NONE -> {}
                    }
                }
            }
        }
    }
}
