package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiAnalysisResult
import com.example.data.model.MarketAsset
import com.example.data.model.SignalAction
import com.example.data.model.Timeframe
import com.example.ui.theme.BearRed
import com.example.ui.theme.BearRedGlow
import com.example.ui.theme.BullGreen
import com.example.ui.theme.BullGreenGlow
import com.example.ui.theme.Ema21Orange
import com.example.ui.theme.Ema9Cyan
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

@Composable
fun AiAnalysisCard(
    asset: MarketAsset,
    timeframe: Timeframe,
    analysisResult: AiAnalysisResult?,
    isAnalyzing: Boolean,
    latestChartBitmap: Bitmap?,
    onAnalyzeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showChartImagePreview by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_analysis_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Ema9Cyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = Ema9Cyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Analisis AI Gemini",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Ema9Cyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Multimodal Vision",
                                    color = Ema9Cyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (analysisResult != null) {
                                "Diperbarui: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(analysisResult.analyzedAt))}"
                            } else {
                                "Rekomendasi teknikal otomatis dari gambar chart"
                            },
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onAnalyzeClick,
                    enabled = !isAnalyzing,
                    modifier = Modifier.testTag("refresh_ai_button")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Ema9Cyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Analisa Ulang",
                            tint = Ema9Cyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = Ema9Cyan)
                        Text(
                            text = "Membuat gambar chart & menganalisa via Gemini API...",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (analysisResult == null) {
                // Empty state CTA
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Dapatkan sinyal rekomendasi trading real-time untuk ${asset.displayName} (${timeframe.label})",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Button(
                            onClick = onAnalyzeClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BullGreen,
                                contentColor = TerminalBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_start_ai_analysis")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mulai Analisa AI Gemini", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Render Full Analysis Result
                val actionColor = when (analysisResult.action) {
                    SignalAction.STRONG_BUY, SignalAction.BUY -> BullGreen
                    SignalAction.STRONG_SELL, SignalAction.SELL -> BearRed
                    SignalAction.NEUTRAL -> WarningGold
                }
                val actionGlow = when (analysisResult.action) {
                    SignalAction.STRONG_BUY, SignalAction.BUY -> BullGreenGlow
                    SignalAction.STRONG_SELL, SignalAction.SELL -> BearRedGlow
                    SignalAction.NEUTRAL -> WarningGold.copy(alpha = 0.2f)
                }

                // Main Signal & Confidence Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, actionColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SINYAL REKOMENDASI",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = actionGlow
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (analysisResult.action in listOf(SignalAction.BUY, SignalAction.STRONG_BUY)) {
                                            Icons.AutoMirrored.Filled.TrendingUp
                                        } else if (analysisResult.action in listOf(SignalAction.SELL, SignalAction.STRONG_SELL)) {
                                            Icons.AutoMirrored.Filled.TrendingDown
                                        } else {
                                            Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = actionColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = analysisResult.action.label,
                                        color = actionColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        // Confidence Gauge
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Keyakinan AI: ${analysisResult.confidence}%",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { analysisResult.confidence / 100f },
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = actionColor,
                                trackColor = SurfaceElevated
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tren: ${analysisResult.trend}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Platform Source & TradingView Rating Verification Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SUMBER PLATFORM DATA",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = analysisResult.platformSource,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        asset.tvRating?.let { rating ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "KONSENSUS TRADINGVIEW",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${rating.action.label} (${String.format(Locale.US, "%+.2f", rating.score)})",
                                    color = when (rating.action) {
                                        SignalAction.STRONG_BUY, SignalAction.BUY -> BullGreen
                                        SignalAction.STRONG_SELL, SignalAction.SELL -> BearRed
                                        else -> WarningGold
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price Targets Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriceLevelBox(
                        title = "Entry Zone",
                        value = analysisResult.entryZone,
                        color = Ema9Cyan,
                        modifier = Modifier.weight(1f)
                    )
                    PriceLevelBox(
                        title = "Target Profit 1",
                        value = String.format(Locale.US, "%.${asset.decimals}f", analysisResult.takeProfit1),
                        color = BullGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriceLevelBox(
                        title = "Target Profit 2",
                        value = String.format(Locale.US, "%.${asset.decimals}f", analysisResult.takeProfit2),
                        color = BullGreen,
                        modifier = Modifier.weight(1f)
                    )
                    PriceLevelBox(
                        title = "Stop Loss (SL)",
                        value = String.format(Locale.US, "%.${asset.decimals}f", analysisResult.stopLoss),
                        color = BearRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Risk Reward Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rasio Risk : Reward",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = analysisResult.riskRewardRatio,
                        color = WarningGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Detected Candlestick Patterns
                if (analysisResult.patternsDetected.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pola Candlestick Terdeteksi:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        analysisResult.patternsDetected.forEach { pattern ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                            ) {
                                Text(
                                    text = "• $pattern",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Detailed Analysis Breakdown Toggle
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Penjelasan & Alasan Strategi",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalysisDetailItem("Rangkuman Eksekutif", analysisResult.keySummary, Ema9Cyan)
                        AnalysisDetailItem("Kondisi RSI (14)", analysisResult.rsiAnalysis, TextSecondary)
                        AnalysisDetailItem("Momentum MACD", analysisResult.macdAnalysis, TextSecondary)
                        AnalysisDetailItem("Moving Average", analysisResult.maAnalysis, TextSecondary)

                        // Risk Note
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BearRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, BearRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = BearRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = analysisResult.riskWarning,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Chart Image Preview Button (Shows the actual image rendered and sent to Gemini)
                if (latestChartBitmap != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showChartImagePreview = !showChartImagePreview },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceElevated,
                            contentColor = Ema9Cyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showChartImagePreview) "Sembunyikan Gambar Chart AI" else "Lihat Gambar Chart Yang Dikirim Ke AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    AnimatedVisibility(visible = showChartImagePreview) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Image(
                                bitmap = latestChartBitmap.asImageBitmap(),
                                contentDescription = "Generated Chart Image for Gemini",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = "Gambar di atas dibuat secara otomatis dari data candlestick & indikator teknikal, lalu dianalisis oleh Gemini AI Vision.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceLevelBox(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AnalysisDetailItem(
    title: String,
    content: String,
    titleColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                color = TextPrimary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
