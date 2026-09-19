package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketAsset
import com.example.data.model.TechnicalIndicators
import com.example.ui.theme.BearRed
import com.example.ui.theme.BollingerLine
import com.example.ui.theme.BullGreen
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
import java.util.Locale
import kotlin.math.abs

@Composable
fun LiveIndicatorsPanel(
    asset: MarketAsset,
    indicators: TechnicalIndicators,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    // Pulsing animation for live tick indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live_indicator")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val currentPrice = asset.currentPrice
    val rsi = indicators.currentRsi ?: 50.0
    val macd = indicators.currentMacd
    val lastEma9 = indicators.ema9.lastOrNull { it != null } ?: currentPrice
    val lastEma21 = indicators.ema21.lastOrNull { it != null } ?: currentPrice
    val bollinger = indicators.currentBollinger
    val stoch = indicators.currentStochastic
    val atr = indicators.currentAtr ?: 0.0

    // Compute Overall Technical Bias Consensus
    var bullPoints = 0
    var bearPoints = 0
    if (rsi in 50.0..70.0) bullPoints += 2 else if (rsi > 70.0) bullPoints += 1 else if (rsi in 30.0..50.0) bearPoints += 2 else bearPoints += 1
    if (lastEma9 != null && lastEma21 != null) {
        if (lastEma9 > lastEma21) bullPoints += 2 else bearPoints += 2
        if (currentPrice > lastEma9) bullPoints += 1 else bearPoints += 1
    }
    if (macd != null) {
        if (macd.histogram > 0) bullPoints += 2 else bearPoints += 2
    }
    if (bollinger != null && currentPrice > bollinger.middle) bullPoints += 1 else bearPoints += 1

    val totalPoints = bullPoints + bearPoints
    val bullRatio = if (totalPoints > 0) (bullPoints.toFloat() / totalPoints.toFloat()) else 0.5f

    val consensusLabel = when {
        bullRatio >= 0.70f -> "SANGAT BULLISH (${(bullRatio * 100).toInt()}%)"
        bullRatio >= 0.55f -> "BULLISH (${(bullRatio * 100).toInt()}%)"
        bullRatio in 0.45f..0.54f -> "NETRAL / KONSOLIDASI (50%)"
        bullRatio in 0.30f..0.44f -> "BEARISH (${((1f - bullRatio) * 100).toInt()}%)"
        else -> "SANGAT BEARISH (${((1f - bullRatio) * 100).toInt()}%)"
    }

    val consensusColor = when {
        bullRatio >= 0.55f -> BullGreen
        bullRatio <= 0.44f -> BearRed
        else -> WarningGold
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_indicators_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Ema9Cyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Indikator Live",
                            tint = Ema9Cyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Indikator Teknikal Live",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // 1s Pulsing Badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BullGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BullGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .graphicsLayer { alpha = pulseAlpha }
                                            .background(BullGreen, CircleShape)
                                    )
                                    Text(
                                        text = "TICK 1 DETIK",
                                        color = BullGreen,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Kalkulasi real-time per detik langsung dari harga pasar",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Tutup" else "Buka",
                    tint = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Consensus Meter Banner (Always visible)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, consensusColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = consensusColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "KONSENSUS INDIKATOR:",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = consensusLabel,
                            color = consensusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Gauge Bar
                    LinearProgressIndicator(
                        progress = { bullRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = consensusColor,
                        trackColor = SurfaceElevated
                    )
                }
            }

            // Expanded Detailed Indicators
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: RSI & MACD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. RSI Card
                        IndicatorItemCard(
                            modifier = Modifier.weight(1f),
                            title = "RSI (14)",
                            primaryValue = String.format(Locale.US, "%.1f", rsi),
                            primaryColor = when {
                                rsi >= 70 -> BearRed
                                rsi <= 30 -> BullGreen
                                rsi >= 50 -> Ema9Cyan
                                else -> WarningGold
                            },
                            badgeLabel = when {
                                rsi >= 70 -> "OVERBOUGHT"
                                rsi <= 30 -> "OVERSOLD"
                                rsi >= 55 -> "BULLISH"
                                rsi <= 45 -> "BEARISH"
                                else -> "NETRAL"
                            },
                            badgeColor = when {
                                rsi >= 70 -> BearRed
                                rsi <= 30 -> BullGreen
                                rsi >= 55 -> BullGreen
                                rsi <= 45 -> BearRed
                                else -> WarningGold
                            },
                            detail = when {
                                rsi >= 70 -> "Tekanan jual berpotensi muncul"
                                rsi <= 30 -> "Tekanan beli berpotensi rebound"
                                rsi >= 50 -> "Momentum tren naik sehat"
                                else -> "Momentum melemah / sideways"
                            }
                        )

                        // 2. MACD Card
                        val hist = macd?.histogram ?: 0.0
                        val isMacdBull = hist > 0.0
                        IndicatorItemCard(
                            modifier = Modifier.weight(1f),
                            title = "MACD (12, 26, 9)",
                            primaryValue = String.format(Locale.US, "%+.2f", hist),
                            primaryColor = if (isMacdBull) BullGreen else BearRed,
                            badgeLabel = if (isMacdBull) "BULL CROSS" else "BEAR CROSS",
                            badgeColor = if (isMacdBull) BullGreen else BearRed,
                            detail = "Line: ${String.format(Locale.US, "%.2f", macd?.macd ?: 0.0)} | Sig: ${String.format(Locale.US, "%.2f", macd?.signal ?: 0.0)}"
                        )
                    }

                    // Row 2: EMA Alignment & Bollinger Bands
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 3. EMA 9 vs 21 Card
                        val emaDiff = if (lastEma9 != null && lastEma21 != null && lastEma21 > 0) {
                            ((lastEma9 - lastEma21) / lastEma21) * 100.0
                        } else 0.0
                        val isEmaGolden = (lastEma9 ?: 0.0) >= (lastEma21 ?: 0.0)

                        IndicatorItemCard(
                            modifier = Modifier.weight(1f),
                            title = "EMA 9 / EMA 21",
                            primaryValue = if (lastEma9 != null) String.format(Locale.US, "%,.${asset.decimals}f", lastEma9) else "--",
                            primaryColor = if (isEmaGolden) BullGreen else BearRed,
                            badgeLabel = if (isEmaGolden) "GOLDEN CROSS" else "DEATH CROSS",
                            badgeColor = if (isEmaGolden) BullGreen else BearRed,
                            detail = "Spread: ${String.format(Locale.US, "%+.2f%%", emaDiff)} vs EMA 21"
                        )

                        // 4. Bollinger Bands Card
                        val bbUpper = bollinger?.upper ?: 0.0
                        val bbLower = bollinger?.lower ?: 0.0
                        val bbRange = bbUpper - bbLower
                        val bbPos = if (bbRange > 0) ((currentPrice - bbLower) / bbRange) * 100.0 else 50.0

                        IndicatorItemCard(
                            modifier = Modifier.weight(1f),
                            title = "BOLLINGER BANDS",
                            primaryValue = "${bbPos.toInt()}% Band",
                            primaryColor = BollingerLine,
                            badgeLabel = if (bbPos >= 80) "UPPER BAND" else if (bbPos <= 20) "LOWER BAND" else "MID ZONE",
                            badgeColor = if (bbPos >= 80) WarningGold else if (bbPos <= 20) BullGreen else Ema9Cyan,
                            detail = "Mid SMA20: ${String.format(Locale.US, "%,.${asset.decimals}f", bollinger?.middle ?: 0.0)}"
                        )
                    }

                    // Row 3: ATR & Stochastic
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 5. ATR Volatility
                        val atrPct = if (currentPrice > 0) (atr / currentPrice) * 100.0 else 0.0
                        IndicatorItemCard(
                            modifier = Modifier.weight(1f),
                            title = "ATR VOLATILITAS (14)",
                            primaryValue = String.format(Locale.US, "%,.${asset.decimals}f", atr),
                            primaryColor = WarningGold,
                            badgeLabel = if (atrPct > 1.5) "VOLATIL TINGGI" else "VOLATIL NORMAL",
                            badgeColor = if (atrPct > 1.5) WarningGold else Ema9Cyan,
                            detail = "Rentang gerak ±${String.format(Locale.US, "%.2f%%", atrPct)} per candle"
                        )

                        // 6. Stochastic (14, 3, 3)
                        val stochK = stoch?.k ?: 50.0
                        val stochD = stoch?.d ?: 50.0
                        IndicatorItemCard(
                            modifier = Modifier.weight(1f),
                            title = "STOCHASTIC (14, 3)",
                            primaryValue = "${stochK.toInt()} / ${stochD.toInt()}",
                            primaryColor = StochK,
                            badgeLabel = if (stochK >= 80) "OVERBOUGHT" else if (stochK <= 20) "OVERSOLD" else "NETRAL",
                            badgeColor = if (stochK >= 80) BearRed else if (stochK <= 20) BullGreen else TextSecondary,
                            detail = "%K: ${String.format(Locale.US, "%.1f", stochK)} | %D: ${String.format(Locale.US, "%.1f", stochD)}"
                        )
                    }

                    // Row 4: Support & Resistance Pivot Matrix
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LEVEL PIVOT & SUPPORT / RESISTANCE",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Target Titik Masuk / Keluar",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // S1
                                LevelColumn(
                                    label = "Support (S1)",
                                    price = indicators.supportLevel1,
                                    currentPrice = currentPrice,
                                    decimals = asset.decimals,
                                    color = SupportGreen
                                )

                                // Pivot Point
                                LevelColumn(
                                    label = "Pivot (PP)",
                                    price = indicators.pivotPoint,
                                    currentPrice = currentPrice,
                                    decimals = asset.decimals,
                                    color = PivotAmber
                                )

                                // R1
                                LevelColumn(
                                    label = "Resistance (R1)",
                                    price = indicators.resistanceLevel1,
                                    currentPrice = currentPrice,
                                    decimals = asset.decimals,
                                    color = ResistanceRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicatorItemCard(
    modifier: Modifier = Modifier,
    title: String,
    primaryValue: String,
    primaryColor: Color,
    badgeLabel: String,
    badgeColor: Color,
    detail: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = primaryValue,
                color = primaryColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = detail,
                color = TextSecondary,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LevelColumn(
    label: String,
    price: Double,
    currentPrice: Double,
    decimals: Int,
    color: Color
) {
    val deltaPct = if (currentPrice > 0 && price > 0) {
        ((price - currentPrice) / currentPrice) * 100.0
    } else 0.0

    Column {
        Text(text = label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Text(
            text = String.format(Locale.US, "%,.${decimals}f", price),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = String.format(Locale.US, "%+.2f%%", deltaPct),
            color = if (deltaPct >= 0) BullGreen else BearRed,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
