package com.example.ui.components.bot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bot.BotUiState
import com.example.data.bot.CoinScreenCategory
import com.example.data.mexc.Mexc24hTicker
import java.util.Locale

private val BrandCyan = Color(0xFF00E5FF)
private val BullGreen = Color(0xFF00E676)
private val BearRed = Color(0xFFFF3D57)
private val CardBg = Color(0xFF161B26)
private val BorderColor = Color(0xFF252D3D)
private val TextMuted = Color(0xFF8B949E)
private val GeminiPurple = Color(0xFF6366F1)

@Composable
fun MexcCoinScreenerTab(
    botState: BotUiState,
    currentSelectedSymbol: String,
    onRefreshScreener: () -> Unit,
    onRunAiSelection: (CoinScreenCategory) -> Unit,
    onSelectCoin: (Mexc24hTicker, Double?, Double?) -> Unit,
    onSelectCategory: (CoinScreenCategory) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Load coins automatically on enter if empty
    LaunchedEffect(Unit) {
        if (botState.coinScreenerResults.isEmpty()) {
            onRefreshScreener()
        }
    }

    val currentCategoryList = botState.coinScreenerResults[botState.selectedScreenerCategory].orEmpty()
    val filteredCoins = remember(currentCategoryList, searchQuery) {
        if (searchQuery.isBlank()) currentCategoryList
        else {
            val q = searchQuery.trim().uppercase(Locale.US)
            currentCategoryList.filter { it.symbol.contains(q) || it.baseAsset.contains(q) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. AI Gemini Recommendation Card
        item {
            AiGeminiRecommendationCard(
                botState = botState,
                currentSelectedSymbol = currentSelectedSymbol,
                onRunAiSelection = { onRunAiSelection(botState.selectedScreenerCategory) },
                onSelectSymbol = { sym, tp, sl ->
                    val ticker = currentCategoryList.firstOrNull { it.symbol.equals(sym, ignoreCase = true) }
                        ?: Mexc24hTicker(sym, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                    onSelectCoin(ticker, tp, sl)
                }
            )
        }

        // 2. Search & Refresh Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari koin MEXC (BTC, SOL, PEPE, SUI...)", color = TextMuted, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandCyan, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg,
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("screener_search_input")
                )

                IconButton(
                    onClick = onRefreshScreener,
                    modifier = Modifier
                        .background(CardBg, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .testTag("refresh_screener_btn")
                ) {
                    if (botState.isScanningCoins) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BrandCyan)
                    }
                }
            }
        }

        // 3. Category Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CoinScreenCategory.values().forEach { cat ->
                    val isSelected = botState.selectedScreenerCategory == cat
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectCategory(cat) }
                            .background(if (isSelected) GeminiPurple.copy(alpha = 0.25f) else CardBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) GeminiPurple else BorderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        color = Color.Transparent
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = cat.label,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Section Label & Count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${botState.selectedScreenerCategory.description} (${filteredCoins.size} koin)",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                if (botState.isScanningCoins) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(10.dp), color = BrandCyan, strokeWidth = 1.5.dp)
                        Text("Memperbarui MEXC...", color = BrandCyan, fontSize = 10.sp)
                    }
                }
            }
        }

        // 5. Coin Items
        if (filteredCoins.isEmpty() && !botState.isScanningCoins) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Tidak ada koin yang sesuai kriteria", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRefreshScreener,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCyan)
                        ) {
                            Text("Muat Ulang Koin MEXC", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            itemsIndexed(filteredCoins) { index, ticker ->
                MexcCoinItemCard(
                    rank = index + 1,
                    ticker = ticker,
                    isSelected = currentSelectedSymbol.equals(ticker.symbol, ignoreCase = true),
                    onSelect = { onSelectCoin(ticker, null, null) }
                )
            }
        }
    }
}

@Composable
private fun AiGeminiRecommendationCard(
    botState: BotUiState,
    currentSelectedSymbol: String,
    onRunAiSelection: () -> Unit,
    onSelectSymbol: (String, Double, Double) -> Unit
) {
    val rec = botState.aiRecommendedCoin
    val isAlreadySelected = rec != null && currentSelectedSymbol.equals(rec.recommendedSymbol, ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_coin_recommendation_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131127)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(GeminiPurple)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = BrandCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Rekomendasi Koin AI Gemini (3.5 Flash)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = GeminiPurple.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPurple)
                ) {
                    Text(
                        text = if (rec?.isLiveGemini == true) "GEMINI PRO LIVE" else "ENGINE KUANTITATIF",
                        color = BrandCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (rec != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = rec.recommendedSymbol,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BullGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BullGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.0f%%", rec.confidence * 100)} Akurasi",
                                    color = BullGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = rec.setupCategory,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Target Profit: +${rec.suggestedTpPct}% • Stop Loss: -${rec.suggestedSlPct}% • Risiko: ${rec.riskLevel}",
                            color = BrandCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            onSelectSymbol(rec.recommendedSymbol, rec.suggestedTpPct, rec.suggestedSlPct)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAlreadySelected) Color(0xFF0F5132) else BrandCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("trade_ai_recommended_coin_btn")
                    ) {
                        Text(
                            text = if (isAlreadySelected) "✓ AKTIF" else "TRADE KOIN INI",
                            color = if (isAlreadySelected) Color.White else Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rec.thesis,
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            } else {
                Text(
                    text = "Klik tombol di bawah untuk meminta Gemini AI menganalisis seluruh koin MEXC dan memilih 1 koin terbaik dengan probabilitas profit tertinggi.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GeminiPurple.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPurple.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { onRunAiSelection() }
                        .clip(RoundedCornerShape(6.dp))
                        .testTag("re_scan_ai_coin_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BrandCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (botState.isScanningCoins) "Gemini Menganalisis..." else "Scan Koin Terbaik via AI",
                            color = BrandCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MexcCoinItemCard(
    rank: Int,
    ticker: Mexc24hTicker,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isPositive = ticker.isPositive

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelect() }
            .background(if (isSelected) Color(0xFF0F232E) else CardBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) BrandCyan else BorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .testTag("mexc_coin_item_${ticker.symbol}"),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Rank, Symbol, Base, Volume
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = "#$rank",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = ticker.baseAsset,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "/USDT",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BrandCyan.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyan)
                            ) {
                                Text(
                                    text = "DIPILIH",
                                    color = BrandCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Vol 24j: ${ticker.formattedVolume} • Spread: ${String.format(Locale.US, "%.1f%%", ticker.spreadPercent)}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // Right: Price, 24h Change, Select Button
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    val priceFormat = if (ticker.lastPrice < 0.01) "%.6f" else if (ticker.lastPrice < 1.0) "%.4f" else "%.2f"
                    Text(
                        text = "$${String.format(Locale.US, priceFormat, ticker.lastPrice)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPositive) BullGreen.copy(alpha = 0.15f) else BearRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%+.2f%%", ticker.priceChangePercent),
                            color = if (isPositive) BullGreen else BearRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) BullGreen else Color(0xFF1F2937),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) BullGreen else BorderColor
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = if (isSelected) "AKTIF" else "PILIH",
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
