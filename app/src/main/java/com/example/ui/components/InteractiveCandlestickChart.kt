package com.example.ui.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.theme.PivotAmber
import com.example.ui.theme.ResistanceRed
import com.example.ui.theme.RsiPurple
import com.example.ui.theme.StochD
import com.example.ui.theme.StochK
import com.example.ui.theme.SupportGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class SubChartType {
    RSI, MACD, STOCHASTIC, ATR, VOLUME, NONE
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
                .background(SurfaceDark, RoundedCornerShape(14.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(14.dp)),
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
    var showBollinger by remember { mutableStateOf(false) }
    var showSupportResistance by remember { mutableStateOf(true) }
    var subChart by remember { mutableStateOf(SubChartType.RSI) }
    var isFullscreen by remember { mutableStateOf(false) }

    var candleSpacing by remember { mutableFloatStateOf(16f) }
    var scrollOffsetIndex by remember { mutableIntStateOf(0) }
    var crosshairCandleIndex by remember { mutableStateOf<Int?>(null) }
    var crosshairTouchY by remember { mutableFloatStateOf(-1f) }

    var chartPlotWidth by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("candlestick_chart_container"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Sleek Toolstrip: Indicators, Subchart Selector & Zoom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Indicator Toggles (EMA, Bollinger, Support/Resistance)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = { showEma = !showEma },
                    shape = RoundedCornerShape(8.dp),
                    color = if (showEma) Ema9Cyan.copy(alpha = 0.2f) else SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showEma) Ema9Cyan else SurfaceCardBorder
                    ),
                    modifier = Modifier.testTag("toggle_ema")
                ) {
                    Text(
                        text = "EMA",
                        color = if (showEma) Ema9Cyan else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (showEma) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    onClick = { showBollinger = !showBollinger },
                    shape = RoundedCornerShape(8.dp),
                    color = if (showBollinger) BollingerLine.copy(alpha = 0.2f) else SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showBollinger) BollingerLine else SurfaceCardBorder
                    ),
                    modifier = Modifier.testTag("toggle_bollinger")
                ) {
                    Text(
                        text = "BOLL",
                        color = if (showBollinger) BollingerLine else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (showBollinger) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    onClick = { showSupportResistance = !showSupportResistance },
                    shape = RoundedCornerShape(8.dp),
                    color = if (showSupportResistance) SupportGreen.copy(alpha = 0.2f) else SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showSupportResistance) SupportGreen else SurfaceCardBorder
                    ),
                    modifier = Modifier.testTag("toggle_sr")
                ) {
                    Text(
                        text = "S/R",
                        color = if (showSupportResistance) SupportGreen else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (showSupportResistance) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }
            }

            // Right: Subchart Selector (RSI / MACD / STOCH / ATR / VOL / OFF) + Zoom Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    SubChartType.RSI to "RSI",
                    SubChartType.MACD to "MACD",
                    SubChartType.STOCHASTIC to "STOCH",
                    SubChartType.ATR to "ATR",
                    SubChartType.VOLUME to "VOL",
                    SubChartType.NONE to "OFF"
                ).forEach { (type, label) ->
                    val isSel = subChart == type
                    Surface(
                        onClick = { subChart = type },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) SurfaceElevated else Color.Transparent,
                        border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder) else null
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) Ema9Cyan else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Zoom Out
                Surface(
                    onClick = { candleSpacing = (candleSpacing * 0.85f).coerceIn(8f, 36f) },
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextSecondary, modifier = Modifier.size(13.dp))
                    }
                }

                // Zoom In
                Surface(
                    onClick = { candleSpacing = (candleSpacing * 1.18f).coerceIn(8f, 36f) },
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TextSecondary, modifier = Modifier.size(13.dp))
                    }
                }

                // Reset zoom/pan if modified
                if (scrollOffsetIndex > 0 || candleSpacing != 16f || crosshairCandleIndex != null) {
                    Surface(
                        onClick = {
                            scrollOffsetIndex = 0
                            candleSpacing = 16f
                            crosshairCandleIndex = null
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Chart", tint = WarningGold, modifier = Modifier.size(13.dp))
                        }
                    }
                }

                // Fullscreen Button
                Surface(
                    onClick = { isFullscreen = true },
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier.testTag("btn_fullscreen_chart")
                ) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Layar Penuh", tint = Ema9Cyan, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // 2. High-Precision OHLC HUD Bar (Rapi, Terstruktur & Tidak Berantakan)
        val selectedCandle = crosshairCandleIndex?.let { idx ->
            candles.getOrNull(idx)
        } ?: candles.lastOrNull()

        selectedCandle?.let { c ->
            val isInspecting = crosshairCandleIndex != null
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(c.timestamp))
            val change = if (c.open > 0) ((c.close - c.open) / c.open) * 100.0 else 0.0
            val isBull = c.isBullish

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ohlc_hud_bar"),
                shape = RoundedCornerShape(10.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Top line: Status + Timestamp & Change %
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isInspecting) WarningGold.copy(alpha = 0.15f) else BullGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isInspecting) WarningGold.copy(alpha = 0.4f) else BullGreen.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = if (isInspecting) "TINJAU" else "LIVE",
                                    color = if (isInspecting) WarningGold else BullGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = dateStr,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isBull) BullGreen.copy(alpha = 0.15f) else BearRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%+.2f%%", change),
                                color = if (isBull) BullGreen else BearRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Bottom line: 4 equal metric columns for O, H, L, C
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OhlcMetricItem(label = "O", value = String.format(Locale.US, "%,.${asset.decimals}f", c.open), color = TextPrimary)
                        OhlcMetricItem(label = "H", value = String.format(Locale.US, "%,.${asset.decimals}f", c.high), color = BullGreen)
                        OhlcMetricItem(label = "L", value = String.format(Locale.US, "%,.${asset.decimals}f", c.low), color = BearRed)
                        OhlcMetricItem(label = "C", value = String.format(Locale.US, "%,.${asset.decimals}f", c.close), color = if (isBull) BullGreen else BearRed)
                    }
                }
            }
        }

        // 3. Main Chart Canvas with Pinch-to-Zoom, Pan, and Tap/LongPress Crosshair
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .pointerInput(candles.size) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Smooth pinch-to-zoom
                        if (zoom != 1f) {
                            val newSpacing = (candleSpacing * zoom).coerceIn(6f, 48f)
                            candleSpacing = newSpacing
                        }
                        // Smooth horizontal panning
                        if (pan.x != 0f) {
                            val candleDelta = (pan.x / candleSpacing).toInt()
                            if (candleDelta != 0) {
                                val maxVis = (chartPlotWidth / candleSpacing).toInt().coerceAtLeast(8)
                                val maxScroll = (candles.size - maxVis).coerceAtLeast(0)
                                scrollOffsetIndex = (scrollOffsetIndex + candleDelta).coerceIn(0, maxScroll)
                            }
                        }
                    }
                }
                .pointerInput(candles.size, candleSpacing, scrollOffsetIndex) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (crosshairCandleIndex != null) {
                                crosshairCandleIndex = null
                            } else {
                                val totalCandles = candles.size
                                val maxVis = (chartPlotWidth / candleSpacing).toInt().coerceAtLeast(8)
                                val start = (totalCandles - maxVis - scrollOffsetIndex).coerceIn(0, totalCandles - 1)
                                val local = (offset.x / candleSpacing).toInt()
                                val target = (start + local).coerceIn(0, totalCandles - 1)
                                crosshairCandleIndex = target
                                crosshairTouchY = offset.y
                            }
                        },
                        onLongPress = { offset ->
                            val totalCandles = candles.size
                            val maxVis = (chartPlotWidth / candleSpacing).toInt().coerceAtLeast(8)
                            val start = (totalCandles - maxVis - scrollOffsetIndex).coerceIn(0, totalCandles - 1)
                            val local = (offset.x / candleSpacing).toInt()
                            val target = (start + local).coerceIn(0, totalCandles - 1)
                            crosshairCandleIndex = target
                            crosshairTouchY = offset.y
                        },
                        onDoubleTap = {
                            candleSpacing = 16f
                            scrollOffsetIndex = 0
                            crosshairCandleIndex = null
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
                chartPlotWidth = mainPlotWidth

                // Sub-chart height allocation
                val subChartHeight = if (subChart != SubChartType.NONE) 70.dp.toPx() else 0f
                val subChartSpacing = if (subChart != SubChartType.NONE) 10.dp.toPx() else 0f
                val mainChartHeight = canvasHeight - subChartHeight - subChartSpacing

                // Calculate visible candle slice
                val maxVisible = (mainPlotWidth / candleSpacing).toInt().coerceAtLeast(8)
                val totalCandles = candles.size

                val startIdx = (totalCandles - maxVisible - scrollOffsetIndex).coerceIn(0, totalCandles - 1)
                val endIdx = (startIdx + maxVisible).coerceAtMost(totalCandles)
                val visibleCandles = candles.subList(startIdx, endIdx)

                if (visibleCandles.isEmpty()) return@Canvas

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
                val paddedMax = maxP + (pRange * 0.06)
                val paddedMin = minP - (pRange * 0.06)

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
                        String.format(Locale.US, "%,.${asset.decimals}f", p),
                        mainPlotWidth + 6f,
                        y + 4f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#64748B")
                            textSize = 22f
                            isAntiAlias = true
                        }
                    )
                }

                // Vertical separator for right price scale
                drawLine(
                    color = SurfaceCardBorder,
                    start = Offset(mainPlotWidth, 0f),
                    end = Offset(mainPlotWidth, canvasHeight),
                    strokeWidth = 1f
                )

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

                // Support & Resistance Price Levels with labels
                if (showSupportResistance) {
                    val r1Y = priceToY(indicators.resistanceLevel1)
                    val s1Y = priceToY(indicators.supportLevel1)
                    val pvY = priceToY(indicators.pivotPoint)

                    if (r1Y in 0f..mainChartHeight) {
                        drawLine(
                            color = ResistanceRed.copy(alpha = 0.5f),
                            start = Offset(0f, r1Y),
                            end = Offset(mainPlotWidth, r1Y),
                            strokeWidth = 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                        )
                        drawContext.canvas.nativeCanvas.drawText("R1", 10f, r1Y - 4f, android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#FF3D57")
                            textSize = 18f
                            isFakeBoldText = true
                            isAntiAlias = true
                        })
                    }

                    if (s1Y in 0f..mainChartHeight) {
                        drawLine(
                            color = SupportGreen.copy(alpha = 0.5f),
                            start = Offset(0f, s1Y),
                            end = Offset(mainPlotWidth, s1Y),
                            strokeWidth = 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                        )
                        drawContext.canvas.nativeCanvas.drawText("S1", 10f, s1Y - 4f, android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#00E676")
                            textSize = 18f
                            isFakeBoldText = true
                            isAntiAlias = true
                        })
                    }

                    if (pvY in 0f..mainChartHeight) {
                        drawLine(
                            color = PivotAmber.copy(alpha = 0.4f),
                            start = Offset(0f, pvY),
                            end = Offset(mainPlotWidth, pvY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                        )
                        drawContext.canvas.nativeCanvas.drawText("PV", 10f, pvY - 4f, android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#FFD600")
                            textSize = 18f
                            isAntiAlias = true
                        })
                    }
                }

                // Current Price Live Line & Highlight Badge on Price Scale
                val lastClosePrice = visibleCandles.last().close
                val currentY = priceToY(lastClosePrice)
                val isBullPrice = asset.change24h >= 0
                val liveBadgeColor = if (isBullPrice) android.graphics.Color.parseColor("#00E676") else android.graphics.Color.parseColor("#FF3D57")

                drawLine(
                    color = if (isBullPrice) BullGreen else BearRed,
                    start = Offset(0f, currentY),
                    end = Offset(mainPlotWidth, currentY),
                    strokeWidth = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                // Draw live price pill on the right scale
                drawContext.canvas.nativeCanvas.drawRoundRect(
                    mainPlotWidth + 3f,
                    currentY - 13f,
                    canvasWidth - 2f,
                    currentY + 13f,
                    6f,
                    6f,
                    android.graphics.Paint().apply {
                        color = liveBadgeColor
                        style = android.graphics.Paint.Style.FILL
                        isAntiAlias = true
                    }
                )
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%,.${asset.decimals}f", lastClosePrice),
                    mainPlotWidth + 6f,
                    currentY + 5f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 20f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                )

                // 3. Crosshair Overlay (Vertical & Horizontal lines + Inspected Price Badge)
                crosshairCandleIndex?.let { cIdx ->
                    val localIdx = cIdx - startIdx
                    if (localIdx in 0 until visibleCandles.size) {
                        val chX = (localIdx * candleSpacing) + (candleSpacing / 2f)
                        val chY = crosshairTouchY.coerceIn(0f, mainChartHeight)

                        // Vertical dashed line
                        drawLine(
                            color = CrosshairLine,
                            start = Offset(chX, 0f),
                            end = Offset(chX, canvasHeight),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )

                        // Horizontal dashed line
                        drawLine(
                            color = CrosshairLine,
                            start = Offset(0f, chY),
                            end = Offset(mainPlotWidth, chY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )

                        // Price tag badge on right axis at chY
                        val touchedPrice = paddedMax - (chY / mainChartHeight) * (paddedMax - paddedMin)
                        drawContext.canvas.nativeCanvas.drawRoundRect(
                            mainPlotWidth + 3f,
                            chY - 13f,
                            canvasWidth - 2f,
                            chY + 13f,
                            6f,
                            6f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#1E293B")
                                style = android.graphics.Paint.Style.FILL
                                isAntiAlias = true
                            }
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%,.${asset.decimals}f", touchedPrice),
                            mainPlotWidth + 6f,
                            chY + 5f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 20f
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                        )
                    }
                }

                // 4. Sub-chart rendering (RSI / MACD / Volume)
                if (subChart != SubChartType.NONE) {
                    val subTop = mainChartHeight + subChartSpacing
                    val subBottom = canvasHeight
                    val subHeight = subBottom - subTop

                    // Sub-chart horizontal separator line
                    drawLine(
                        color = SurfaceCardBorder,
                        start = Offset(0f, subTop - subChartSpacing / 2f),
                        end = Offset(canvasWidth, subTop - subChartSpacing / 2f),
                        strokeWidth = 1f
                    )

                    // Sub-chart background
                    drawRect(
                        color = Color(0xFF0F131C),
                        topLeft = Offset(0f, subTop),
                        size = Size(mainPlotWidth, subHeight)
                    )

                    when (subChart) {
                        SubChartType.RSI -> {
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
                            val curRsi = indicators.currentRsi?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
                            drawContext.canvas.nativeCanvas.drawText("RSI(14): $curRsi", 10f, subTop + 22f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#E040FB")
                                textSize = 22f
                                isAntiAlias = true
                            })
                        }
                        SubChartType.MACD -> {
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
                            drawContext.canvas.nativeCanvas.drawText("MACD(12,26,9)", 10f, subTop + 22f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#2979FF")
                                textSize = 22f
                                isAntiAlias = true
                            })
                        }
                        SubChartType.STOCHASTIC -> {
                            fun stochY(v: Double): Float {
                                val clamped = v.coerceIn(0.0, 100.0)
                                return (subBottom - ((clamped / 100.0) * subHeight)).toFloat()
                            }
                            val y80 = stochY(80.0)
                            val y20 = stochY(20.0)

                            drawRect(
                                color = Ema9Cyan.copy(alpha = 0.05f),
                                topLeft = Offset(0f, y80),
                                size = Size(mainPlotWidth, y20 - y80)
                            )
                            drawLine(color = ChartGridLine, start = Offset(0f, y80), end = Offset(mainPlotWidth, y80), strokeWidth = 1f)
                            drawLine(color = ChartGridLine, start = Offset(0f, y20), end = Offset(mainPlotWidth, y20), strokeWidth = 1f)

                            val kPath = Path()
                            val dPath = Path()
                            var kInit = false
                            var dInit = false

                            for (i in visibleCandles.indices) {
                                val globalIndex = startIdx + i
                                val x = (i * candleSpacing) + (candleSpacing / 2f)
                                indicators.stochastic.getOrNull(globalIndex)?.let { pt ->
                                    val ky = stochY(pt.k)
                                    val dy = stochY(pt.d)
                                    if (!kInit) {
                                        kPath.moveTo(x, ky)
                                        kInit = true
                                    } else {
                                        kPath.lineTo(x, ky)
                                    }
                                    if (!dInit) {
                                        dPath.moveTo(x, dy)
                                        dInit = true
                                    } else {
                                        dPath.lineTo(x, dy)
                                    }
                                }
                            }
                            if (kInit) drawPath(path = kPath, color = StochK, style = Stroke(width = 2f))
                            if (dInit) drawPath(path = dPath, color = StochD, style = Stroke(width = 1.5f))

                            val curStoch = indicators.currentStochastic
                            val stochLabel = if (curStoch != null) {
                                String.format(Locale.US, "%%K: %.1f  %%D: %.1f", curStoch.k, curStoch.d)
                            } else {
                                "Stoch(14,3)"
                            }
                            drawContext.canvas.nativeCanvas.drawText("STOCH(14,3): $stochLabel", 10f, subTop + 22f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#00E5FF")
                                textSize = 22f
                                isAntiAlias = true
                            })
                        }
                        SubChartType.ATR -> {
                            val visibleAtrs = visibleCandles.indices.mapNotNull { i ->
                                indicators.atr14.getOrNull(startIdx + i)
                            }
                            val maxAtr = visibleAtrs.maxOrNull() ?: 1.0
                            val minAtr = visibleAtrs.minOrNull() ?: 0.0
                            val atrRange = (maxAtr - minAtr).coerceAtLeast(0.00001)

                            fun atrToY(v: Double): Float {
                                val norm = ((v - minAtr) / atrRange).coerceIn(0.0, 1.0)
                                return (subBottom - (norm * subHeight * 0.8f) - (subHeight * 0.1f)).toFloat()
                            }

                            val atrPath = Path()
                            var atrInit = false
                            for (i in visibleCandles.indices) {
                                val globalIndex = startIdx + i
                                val x = (i * candleSpacing) + (candleSpacing / 2f)
                                indicators.atr14.getOrNull(globalIndex)?.let { aVal ->
                                    val ay = atrToY(aVal)
                                    if (!atrInit) {
                                        atrPath.moveTo(x, ay)
                                        atrInit = true
                                    } else {
                                        atrPath.lineTo(x, ay)
                                    }
                                }
                            }
                            if (atrInit) {
                                drawPath(path = atrPath, color = WarningGold, style = Stroke(width = 2f))
                            }
                            val curAtr = indicators.currentAtr?.let { String.format(Locale.US, "%,.${asset.decimals}f", it) } ?: "--"
                            drawContext.canvas.nativeCanvas.drawText("ATR(14 Volatility): $curAtr", 10f, subTop + 22f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#FFD600")
                                textSize = 22f
                                isAntiAlias = true
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
                            drawContext.canvas.nativeCanvas.drawText("Volume", 10f, subTop + 22f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#94A3B8")
                                textSize = 22f
                                isAntiAlias = true
                            })
                        }
                        SubChartType.NONE -> {}
                    }
                }
            }
        }
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FullScreenCandlestickChartDialog(
                asset = asset,
                candles = candles,
                indicators = indicators,
                onDismiss = { isFullscreen = false }
            )
        }
    }
}

@Composable
private fun OhlcMetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun FullScreenCandlestickChartDialog(
    asset: MarketAsset,
    candles: List<CandleStick>,
    indicators: TechnicalIndicators,
    onDismiss: () -> Unit
) {
    var showEma by remember { mutableStateOf(true) }
    var showBollinger by remember { mutableStateOf(false) }
    var showSupportResistance by remember { mutableStateOf(true) }
    var subChart by remember { mutableStateOf(SubChartType.RSI) }

    var candleSpacing by remember { mutableFloatStateOf(18f) }
    var scrollOffsetIndex by remember { mutableIntStateOf(0) }
    var crosshairCandleIndex by remember { mutableStateOf<Int?>(null) }
    var crosshairTouchY by remember { mutableFloatStateOf(-1f) }

    var chartPlotWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Symbol, Live Price, Indicators, and Close Fullscreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup Layar Penuh", tint = TextPrimary)
                    }
                    Text(
                        text = asset.symbol,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "LIVE",
                        color = BullGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(BullGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // EMA Toggle
                    Surface(
                        onClick = { showEma = !showEma },
                        shape = RoundedCornerShape(6.dp),
                        color = if (showEma) Ema9Cyan.copy(alpha = 0.2f) else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (showEma) Ema9Cyan else SurfaceCardBorder)
                    ) {
                        Text(
                            text = "EMA",
                            color = if (showEma) Ema9Cyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                    // BOLL Toggle
                    Surface(
                        onClick = { showBollinger = !showBollinger },
                        shape = RoundedCornerShape(6.dp),
                        color = if (showBollinger) BollingerLine.copy(alpha = 0.2f) else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (showBollinger) BollingerLine else SurfaceCardBorder)
                    ) {
                        Text(
                            text = "BOLL",
                            color = if (showBollinger) BollingerLine else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    // S/R Toggle
                    Surface(
                        onClick = { showSupportResistance = !showSupportResistance },
                        shape = RoundedCornerShape(6.dp),
                        color = if (showSupportResistance) SupportGreen.copy(alpha = 0.2f) else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (showSupportResistance) SupportGreen else SurfaceCardBorder)
                    ) {
                        Text(
                            text = "S/R",
                            color = if (showSupportResistance) SupportGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    // Subcharts
                    listOf(
                        SubChartType.RSI to "RSI",
                        SubChartType.MACD to "MACD",
                        SubChartType.STOCHASTIC to "STOCH",
                        SubChartType.ATR to "ATR",
                        SubChartType.VOLUME to "VOL",
                        SubChartType.NONE to "OFF"
                    ).forEach { (type, label) ->
                        val isSel = subChart == type
                        Surface(
                            onClick = { subChart = type },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) SurfaceElevated else Color.Transparent,
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder) else null
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Ema9Cyan else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Zoom Out
                    Surface(
                        onClick = { candleSpacing = (candleSpacing * 0.82f).coerceIn(6f, 48f) },
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextSecondary, modifier = Modifier.size(13.dp))
                        }
                    }
                    // Zoom In
                    Surface(
                        onClick = { candleSpacing = (candleSpacing * 1.22f).coerceIn(6f, 48f) },
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TextSecondary, modifier = Modifier.size(13.dp))
                        }
                    }
                    // Reset
                    if (scrollOffsetIndex > 0 || candleSpacing != 18f || crosshairCandleIndex != null) {
                        Surface(
                            onClick = {
                                scrollOffsetIndex = 0
                                candleSpacing = 18f
                                crosshairCandleIndex = null
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                        ) {
                            Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Chart", tint = WarningGold, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }

            // OHLC Metrics Banner in Fullscreen
            val selectedCandle = crosshairCandleIndex?.let { idx ->
                candles.getOrNull(idx)
            } ?: candles.lastOrNull()

            selectedCandle?.let { c ->
                val isInspecting = crosshairCandleIndex != null
                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(c.timestamp))
                val change = if (c.open > 0) ((c.close - c.open) / c.open) * 100.0 else 0.0
                val isBull = c.isBullish

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isInspecting) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(WarningGold, RoundedCornerShape(3.dp))
                                )
                            }
                            Text(
                                text = dateStr,
                                color = if (isInspecting) WarningGold else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OhlcMetricItem(label = "O", value = String.format(Locale.US, "%,.${asset.decimals}f", c.open), color = TextPrimary)
                            OhlcMetricItem(label = "H", value = String.format(Locale.US, "%,.${asset.decimals}f", c.high), color = BullGreen)
                            OhlcMetricItem(label = "L", value = String.format(Locale.US, "%,.${asset.decimals}f", c.low), color = BearRed)
                            OhlcMetricItem(label = "C", value = String.format(Locale.US, "%,.${asset.decimals}f", c.close), color = if (isBull) BullGreen else BearRed)
                            Text(
                                text = String.format(Locale.US, "%+.2f%%", change),
                                color = if (isBull) BullGreen else BearRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Fullscreen Canvas Container (Fills rest of screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(candles.size) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (zoom != 1f) {
                                candleSpacing = (candleSpacing * zoom).coerceIn(6f, 48f)
                            }
                            if (pan.x != 0f) {
                                val delta = (pan.x / candleSpacing).toInt()
                                if (delta != 0) {
                                    val maxVis = (chartPlotWidth / candleSpacing).toInt().coerceAtLeast(8)
                                    val maxScroll = (candles.size - maxVis).coerceAtLeast(0)
                                    scrollOffsetIndex = (scrollOffsetIndex + delta).coerceIn(0, maxScroll)
                                }
                            }
                        }
                    }
                    .pointerInput(candles.size, candleSpacing, scrollOffsetIndex) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (crosshairCandleIndex != null) {
                                    crosshairCandleIndex = null
                                } else {
                                    val totalCandles = candles.size
                                    val maxVis = (chartPlotWidth / candleSpacing).toInt().coerceAtLeast(8)
                                    val start = (totalCandles - maxVis - scrollOffsetIndex).coerceIn(0, totalCandles - 1)
                                    val local = (offset.x / candleSpacing).toInt()
                                    val target = (start + local).coerceIn(0, totalCandles - 1)
                                    crosshairCandleIndex = target
                                    crosshairTouchY = offset.y
                                }
                            },
                            onLongPress = { offset ->
                                val totalCandles = candles.size
                                val maxVis = (chartPlotWidth / candleSpacing).toInt().coerceAtLeast(8)
                                val start = (totalCandles - maxVis - scrollOffsetIndex).coerceIn(0, totalCandles - 1)
                                val local = (offset.x / candleSpacing).toInt()
                                val target = (start + local).coerceIn(0, totalCandles - 1)
                                crosshairCandleIndex = target
                                crosshairTouchY = offset.y
                            },
                            onDoubleTap = {
                                candleSpacing = 18f
                                scrollOffsetIndex = 0
                                crosshairCandleIndex = null
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val priceScaleWidth = 76.dp.toPx()
                    val mainPlotWidth = canvasWidth - priceScaleWidth
                    chartPlotWidth = mainPlotWidth

                    val subChartHeight = if (subChart != SubChartType.NONE) 80.dp.toPx() else 0f
                    val subChartSpacing = if (subChart != SubChartType.NONE) 10.dp.toPx() else 0f
                    val mainChartHeight = canvasHeight - subChartHeight - subChartSpacing

                    val maxVisible = (mainPlotWidth / candleSpacing).toInt().coerceAtLeast(8)
                    val totalCandles = candles.size
                    val startIdx = (totalCandles - maxVisible - scrollOffsetIndex).coerceIn(0, totalCandles - 1)
                    val endIdx = (startIdx + maxVisible).coerceAtMost(totalCandles)
                    val visibleCandles = candles.subList(startIdx, endIdx)

                    if (visibleCandles.isEmpty()) return@Canvas

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
                    val paddedMax = maxP + (pRange * 0.06)
                    val paddedMin = minP - (pRange * 0.06)

                    fun priceToY(price: Double): Float {
                        val ratio = (price - paddedMin) / (paddedMax - paddedMin)
                        return (mainChartHeight - (ratio * mainChartHeight)).toFloat()
                    }

                    // Draw Horizontal Grid Lines
                    val gridSteps = 6
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
                            String.format(Locale.US, "%,.${asset.decimals}f", p),
                            mainPlotWidth + 6f,
                            y + 4f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#64748B")
                                textSize = 22f
                                isAntiAlias = true
                            }
                        )
                    }

                    // Draw Candles & Indicators
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

                        drawLine(
                            color = candleColor,
                            start = Offset(x, highY),
                            end = Offset(x, lowY),
                            strokeWidth = 1.5f
                        )

                        val topY = min(openY, closeY)
                        val bottomY = max(openY, closeY).coerceAtLeast(topY + 1.5f)
                        drawRect(
                            color = candleColor,
                            topLeft = Offset(x - (candleBodyWidth / 2f), topY),
                            size = Size(candleBodyWidth, bottomY - topY)
                        )

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

                    // Support & Resistance Lines on Fullscreen Canvas
                    if (showSupportResistance) {
                        val r1Y = priceToY(indicators.resistanceLevel1)
                        val s1Y = priceToY(indicators.supportLevel1)
                        val pvY = priceToY(indicators.pivotPoint)

                        if (r1Y in 0f..mainChartHeight) {
                            drawLine(
                                color = ResistanceRed.copy(alpha = 0.5f),
                                start = Offset(0f, r1Y),
                                end = Offset(mainPlotWidth, r1Y),
                                strokeWidth = 1.2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                            )
                            drawContext.canvas.nativeCanvas.drawText("R1", 10f, r1Y - 4f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#FF3D57")
                                textSize = 20f
                                isFakeBoldText = true
                                isAntiAlias = true
                            })
                        }

                        if (s1Y in 0f..mainChartHeight) {
                            drawLine(
                                color = SupportGreen.copy(alpha = 0.5f),
                                start = Offset(0f, s1Y),
                                end = Offset(mainPlotWidth, s1Y),
                                strokeWidth = 1.2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                            )
                            drawContext.canvas.nativeCanvas.drawText("S1", 10f, s1Y - 4f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#00E676")
                                textSize = 20f
                                isFakeBoldText = true
                                isAntiAlias = true
                            })
                        }

                        if (pvY in 0f..mainChartHeight) {
                            drawLine(
                                color = PivotAmber.copy(alpha = 0.4f),
                                start = Offset(0f, pvY),
                                end = Offset(mainPlotWidth, pvY),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                            )
                            drawContext.canvas.nativeCanvas.drawText("PV", 10f, pvY - 4f, android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#FFD600")
                                textSize = 20f
                                isAntiAlias = true
                            })
                        }
                    }

                    // Live Current Price Line & Badge in Fullscreen
                    val lastClosePrice = visibleCandles.last().close
                    val currentY = priceToY(lastClosePrice)
                    val isBullPrice = asset.change24h >= 0
                    val liveBadgeColor = if (isBullPrice) android.graphics.Color.parseColor("#00E676") else android.graphics.Color.parseColor("#FF3D57")

                    drawLine(
                        color = if (isBullPrice) BullGreen else BearRed,
                        start = Offset(0f, currentY),
                        end = Offset(mainPlotWidth, currentY),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        mainPlotWidth + 3f,
                        currentY - 13f,
                        canvasWidth - 2f,
                        currentY + 13f,
                        6f,
                        6f,
                        android.graphics.Paint().apply {
                            color = liveBadgeColor
                            style = android.graphics.Paint.Style.FILL
                            isAntiAlias = true
                        }
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format(Locale.US, "%,.${asset.decimals}f", lastClosePrice),
                        mainPlotWidth + 6f,
                        currentY + 5f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 20f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )

                    // Crosshair overlay
                    crosshairCandleIndex?.let { cIdx ->
                        val localIdx = cIdx - startIdx
                        if (localIdx in 0 until visibleCandles.size) {
                            val chX = (localIdx * candleSpacing) + (candleSpacing / 2f)
                            val chY = crosshairTouchY.coerceIn(0f, mainChartHeight)

                            drawLine(
                                color = CrosshairLine,
                                start = Offset(chX, 0f),
                                end = Offset(chX, canvasHeight),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )

                            drawLine(
                                color = CrosshairLine,
                                start = Offset(0f, chY),
                                end = Offset(mainPlotWidth, chY),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )

                            val touchedPrice = paddedMax - (chY / mainChartHeight) * (paddedMax - paddedMin)
                            drawContext.canvas.nativeCanvas.drawRoundRect(
                                mainPlotWidth + 3f,
                                chY - 13f,
                                canvasWidth - 2f,
                                chY + 13f,
                                6f,
                                6f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#1E293B")
                                    style = android.graphics.Paint.Style.FILL
                                    isAntiAlias = true
                                }
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format(Locale.US, "%,.${asset.decimals}f", touchedPrice),
                                mainPlotWidth + 6f,
                                chY + 5f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 20f
                                    isFakeBoldText = true
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                    // Subchart in Fullscreen
                    if (subChart != SubChartType.NONE) {
                        val subTop = mainChartHeight + subChartSpacing
                        val subBottom = canvasHeight
                        val subHeight = subBottom - subTop

                        drawLine(
                            color = SurfaceCardBorder,
                            start = Offset(0f, subTop - subChartSpacing / 2f),
                            end = Offset(canvasWidth, subTop - subChartSpacing / 2f),
                            strokeWidth = 1f
                        )

                        drawRect(
                            color = Color(0xFF0F131C),
                            topLeft = Offset(0f, subTop),
                            size = Size(mainPlotWidth, subHeight)
                        )

                        when (subChart) {
                            SubChartType.RSI -> {
                                fun rsiY(rsiVal: Double): Float =
                                    (subBottom - ((rsiVal.toFloat() / 100f) * subHeight)).coerceIn(subTop, subBottom)

                                drawLine(color = ChartGridLine, start = Offset(0f, rsiY(70.0)), end = Offset(mainPlotWidth, rsiY(70.0)), strokeWidth = 1f)
                                drawLine(color = ChartGridLine, start = Offset(0f, rsiY(30.0)), end = Offset(mainPlotWidth, rsiY(30.0)), strokeWidth = 1f)

                                val rsiPath = Path()
                                var rsiInit = false
                                for (i in visibleCandles.indices) {
                                    val globalIndex = startIdx + i
                                    val x = (i * candleSpacing) + (candleSpacing / 2f)
                                    indicators.rsi14.getOrNull(globalIndex)?.let { rsiVal ->
                                        val y = rsiY(rsiVal)
                                        if (!rsiInit) {
                                            rsiPath.moveTo(x, y)
                                            rsiInit = true
                                        } else {
                                            rsiPath.lineTo(x, y)
                                        }
                                    }
                                }
                                if (rsiInit) drawPath(path = rsiPath, color = RsiPurple, style = Stroke(width = 2f))
                                val curRsi = indicators.rsi14.getOrNull(endIdx - 1)?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
                                drawContext.canvas.nativeCanvas.drawText("RSI(14): $curRsi", 10f, subTop + 22f, android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#E040FB")
                                    textSize = 22f
                                    isAntiAlias = true
                                })
                            }
                            SubChartType.MACD -> {
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
                                drawContext.canvas.nativeCanvas.drawText("MACD(12,26,9)", 10f, subTop + 22f, android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#2979FF")
                                    textSize = 22f
                                    isAntiAlias = true
                                })
                            }
                            SubChartType.STOCHASTIC -> {
                                fun stochY(v: Double): Float {
                                    val clamped = v.coerceIn(0.0, 100.0)
                                    return (subBottom - ((clamped / 100.0) * subHeight)).toFloat()
                                }
                                val y80 = stochY(80.0)
                                val y20 = stochY(20.0)

                                drawRect(
                                    color = Ema9Cyan.copy(alpha = 0.05f),
                                    topLeft = Offset(0f, y80),
                                    size = Size(mainPlotWidth, y20 - y80)
                                )
                                drawLine(color = ChartGridLine, start = Offset(0f, y80), end = Offset(mainPlotWidth, y80), strokeWidth = 1f)
                                drawLine(color = ChartGridLine, start = Offset(0f, y20), end = Offset(mainPlotWidth, y20), strokeWidth = 1f)

                                val kPath = Path()
                                val dPath = Path()
                                var kInit = false
                                var dInit = false

                                for (i in visibleCandles.indices) {
                                    val globalIndex = startIdx + i
                                    val x = (i * candleSpacing) + (candleSpacing / 2f)
                                    indicators.stochastic.getOrNull(globalIndex)?.let { pt ->
                                        val ky = stochY(pt.k)
                                        val dy = stochY(pt.d)
                                        if (!kInit) {
                                            kPath.moveTo(x, ky)
                                            kInit = true
                                        } else {
                                            kPath.lineTo(x, ky)
                                        }
                                        if (!dInit) {
                                            dPath.moveTo(x, dy)
                                            dInit = true
                                        } else {
                                            dPath.lineTo(x, dy)
                                        }
                                    }
                                }
                                if (kInit) drawPath(path = kPath, color = StochK, style = Stroke(width = 2f))
                                if (dInit) drawPath(path = dPath, color = StochD, style = Stroke(width = 1.5f))

                                val curStoch = indicators.currentStochastic
                                val stochLabel = if (curStoch != null) {
                                    String.format(Locale.US, "%%K: %.1f  %%D: %.1f", curStoch.k, curStoch.d)
                                } else {
                                    "Stoch(14,3)"
                                }
                                drawContext.canvas.nativeCanvas.drawText("STOCH(14,3): $stochLabel", 10f, subTop + 22f, android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#00E5FF")
                                    textSize = 22f
                                    isAntiAlias = true
                                })
                            }
                            SubChartType.ATR -> {
                                val visibleAtrs = visibleCandles.indices.mapNotNull { i ->
                                    indicators.atr14.getOrNull(startIdx + i)
                                }
                                val maxAtr = visibleAtrs.maxOrNull() ?: 1.0
                                val minAtr = visibleAtrs.minOrNull() ?: 0.0
                                val atrRange = (maxAtr - minAtr).coerceAtLeast(0.00001)

                                fun atrToY(v: Double): Float {
                                    val norm = ((v - minAtr) / atrRange).coerceIn(0.0, 1.0)
                                    return (subBottom - (norm * subHeight * 0.8f) - (subHeight * 0.1f)).toFloat()
                                }

                                val atrPath = Path()
                                var atrInit = false
                                for (i in visibleCandles.indices) {
                                    val globalIndex = startIdx + i
                                    val x = (i * candleSpacing) + (candleSpacing / 2f)
                                    indicators.atr14.getOrNull(globalIndex)?.let { aVal ->
                                        val ay = atrToY(aVal)
                                        if (!atrInit) {
                                            atrPath.moveTo(x, ay)
                                            atrInit = true
                                        } else {
                                            atrPath.lineTo(x, ay)
                                        }
                                    }
                                }
                                if (atrInit) {
                                    drawPath(path = atrPath, color = WarningGold, style = Stroke(width = 2f))
                                }
                                val curAtr = indicators.currentAtr?.let { String.format(Locale.US, "%,.${asset.decimals}f", it) } ?: "--"
                                drawContext.canvas.nativeCanvas.drawText("ATR(14 Volatility): $curAtr", 10f, subTop + 22f, android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#FFD600")
                                    textSize = 22f
                                    isAntiAlias = true
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
                                drawContext.canvas.nativeCanvas.drawText("Volume", 10f, subTop + 22f, android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#94A3B8")
                                    textSize = 22f
                                    isAntiAlias = true
                                })
                            }
                            SubChartType.NONE -> {}
                        }
                    }
                }
            }
        }
    }
}

