package com.example.ui.components.bot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.calculator.IndicatorCalculator
import com.example.data.bot.BacktestReport
import com.example.data.bot.BotStrategyType
import com.example.data.bot.BotUiState
import com.example.data.bot.CoinScreenCategory
import com.example.data.mexc.Mexc24hTicker
import com.example.data.model.AssetType
import com.example.data.model.CandleStick
import com.example.data.model.ExchangePlatform
import com.example.data.model.MarketAsset
import com.example.data.model.TechnicalIndicators
import com.example.ui.components.InteractiveCandlestickChart
import com.example.ui.components.LiveIndicatorsPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandCyan = Color(0xFF00E5FF)
private val BullGreen = Color(0xFF00E676)
private val BearRed = Color(0xFFFF3D57)
private val DarkBg = Color(0xFF0D1117)
private val CardBg = Color(0xFF161B26)
private val BorderColor = Color(0xFF252D3D)
private val TextMuted = Color(0xFF8B949E)
private val GeminiPurple = Color(0xFF6366F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingBotDialog(
    botState: BotUiState,
    currentSymbol: String,
    currentPrice: Double,
    candles: List<CandleStick>,
    backtestReport: BacktestReport?,
    isBacktesting: Boolean,
    onDismiss: () -> Unit,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onToggleSandbox: (Boolean) -> Unit,
    onSaveCredentials: (String, String) -> Unit,
    onTestMexcConnection: () -> Unit,
    onSendMexcTestOrder: (String) -> Unit,
    onSelectStrategy: (BotStrategyType) -> Unit,
    onSaveRiskParameters: (Double, Double, Double, Double) -> Unit,
    onRunBacktest: (String, String, Int) -> Unit,
    onManualClosePosition: () -> Unit,
    onResetSandboxBalance: () -> Unit,
    onClearHistory: () -> Unit,
    onExecuteGeminiTrader: () -> Unit = {},
    onExecuteInstantBuy: () -> Unit = {},
    onSaveGeminiKey: (String) -> Unit = {},
    onRefreshScreener: () -> Unit = {},
    onRunAiCoinSelection: (CoinScreenCategory) -> Unit = {},
    onSelectCoin: (Mexc24hTicker, Double?, Double?) -> Unit = { _, _, _ -> },
    onSelectScreenerCategory: (CoinScreenCategory) -> Unit = {},
    asset: MarketAsset? = null,
    indicators: TechnicalIndicators? = null,
    refreshCountdown: Int = 5,
    isRefreshingPrice: Boolean = false
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🤖 Gemini Bot", "🪙 Koin MEXC", "📈 Uji Coba", "⚙️ Akun & API")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(18.dp)),
            color = DarkBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (botState.isRunning) BullGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Bot",
                            tint = BrandCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Gemini Trading Bot",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (botState.isSandbox) Color(0xFF1E3A8A)
                                            else Color(0xFF7F1D1D)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (botState.isSandbox) "LATIHAN SANDBOX" else "REAL MEXC",
                                        color = if (botState.isSandbox) Color(0xFF93C5FD) else Color(0xFFFCA5A5),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Pasar: $currentSymbol • Harga: $${String.format(Locale.US, "%,.2f", currentPrice)}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_bot_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White
                        )
                    }
                }

                // Simplified 3 Tabs Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CardBg,
                    contentColor = BrandCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = BrandCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) BrandCyan else TextMuted
                                )
                            }
                        )
                    }
                }

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> SimpleGeminiBotTab(
                            botState = botState,
                            currentSymbol = currentSymbol,
                            currentPrice = currentPrice,
                            candles = candles,
                            asset = asset,
                            indicators = indicators,
                            refreshCountdown = refreshCountdown,
                            isRefreshingPrice = isRefreshingPrice,
                            onStartBot = onStartBot,
                            onStopBot = onStopBot,
                            onToggleSandbox = onToggleSandbox,
                            onExecuteGeminiTrader = onExecuteGeminiTrader,
                            onExecuteInstantBuy = onExecuteInstantBuy,
                            onManualClosePosition = onManualClosePosition,
                            onResetSandboxBalance = onResetSandboxBalance,
                            onOpenScreenerTab = { selectedTab = 1 },
                            onSelectCoin = onSelectCoin
                        )
                        1 -> MexcCoinScreenerTab(
                            botState = botState,
                            currentSelectedSymbol = currentSymbol,
                            onRefreshScreener = onRefreshScreener,
                            onRunAiSelection = onRunAiCoinSelection,
                            onSelectCoin = { ticker, tp, sl ->
                                onSelectCoin(ticker, tp, sl)
                                selectedTab = 0
                            },
                            onSelectCategory = onSelectScreenerCategory
                        )
                        2 -> SimpleBacktestTab(
                            currentSymbol = currentSymbol,
                            backtestReport = backtestReport,
                            isBacktesting = isBacktesting,
                            onRunBacktest = onRunBacktest
                        )
                        3 -> SimpleMexcSettingsTab(
                            botState = botState,
                            currentSymbol = currentSymbol,
                            onSaveCredentials = onSaveCredentials,
                            onTestMexcConnection = onTestMexcConnection,
                            onSendMexcTestOrder = onSendMexcTestOrder,
                            onSaveRiskParameters = onSaveRiskParameters,
                            onClearHistory = onClearHistory,
                            onSaveGeminiKey = onSaveGeminiKey
                        )
                    }
                }
            }
        }
    }
}

/**
 * TAB 1: Tampilan Utama Bot Gemini yang Sederhana & Ramah Pemula
 */
@Composable
private fun SimpleGeminiBotTab(
    botState: BotUiState,
    currentSymbol: String,
    currentPrice: Double,
    candles: List<CandleStick>,
    asset: MarketAsset?,
    indicators: TechnicalIndicators?,
    refreshCountdown: Int,
    isRefreshingPrice: Boolean,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onToggleSandbox: (Boolean) -> Unit,
    onExecuteGeminiTrader: () -> Unit,
    onExecuteInstantBuy: () -> Unit,
    onManualClosePosition: () -> Unit,
    onResetSandboxBalance: () -> Unit,
    onOpenScreenerTab: () -> Unit = {},
    onSelectCoin: (Mexc24hTicker, Double?, Double?) -> Unit = { _, _, _ -> }
) {
    val scrollState = rememberScrollState()
    val position = botState.activePosition

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Warning Banner jika Gemini Terkena Rate Limit (429)
        if (botState.isGeminiRateLimited) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1E08)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF59E0B)))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Batas Kuota Gemini API (Rate Limit 429)",
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Jeda cooldown: ${botState.geminiRateLimitCooldownSec}d. Bot TETAP AKTIF mengeksekusi order via Engine Kuantitatif Lokal otomatis!",
                            color = Color(0xFFFCD34D),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
        // 1. Pilihan Mode: Latihan Bebas Risiko vs Akun Asli
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "PILIH MODE TRADING:",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Sandbox
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleSandbox(true) }
                            .background(if (botState.isSandbox) Color(0xFF1E3A8A) else Color(0xFF1E2430))
                            .border(
                                width = if (botState.isSandbox) 1.5.dp else 1.dp,
                                color = if (botState.isSandbox) Color(0xFF60A5FA) else BorderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        color = Color.Transparent
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🎮 Mode Latihan (Sandbox)",
                                color = if (botState.isSandbox) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Uang Virtual $1,000 (Aman)",
                                color = if (botState.isSandbox) Color(0xFF93C5FD) else TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Tombol Real MEXC
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleSandbox(false) }
                            .background(if (!botState.isSandbox) Color(0xFF450A0A) else Color(0xFF1E2430))
                            .border(
                                width = if (!botState.isSandbox) 1.5.dp else 1.dp,
                                color = if (!botState.isSandbox) Color(0xFFF87171) else BorderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        color = Color.Transparent
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "💼 Akun Asli MEXC",
                                color = if (!botState.isSandbox) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Saldo Nyata Bursa MEXC",
                                color = if (!botState.isSandbox) Color(0xFFFCA5A5) else TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // 1.5. Kartu Target Koin MEXC & Rekomendasi AI Gemini
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("target_coin_selection_card"),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PASAR / KOIN AKTIF:",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = currentSymbol,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BrandCyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "MEXC SPOT",
                                    color = BrandCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onOpenScreenerTab,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyan.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_screener_tab_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = BrandCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Pilih Koin MEXC",
                                color = BrandCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val aiRec = botState.aiRecommendedCoin
                if (aiRec != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GeminiPurple.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPurple.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = BrandCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Rekomendasi AI: ${aiRec.recommendedSymbol} (${String.format(Locale.US, "%.0f%%", aiRec.confidence * 100)} Akurasi)",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Target TP: +${aiRec.suggestedTpPct}% • SL: -${aiRec.suggestedSlPct}% • ${aiRec.setupCategory}",
                                    color = BrandCyan,
                                    fontSize = 9.sp
                                )
                            }

                            if (!currentSymbol.equals(aiRec.recommendedSymbol, ignoreCase = true)) {
                                Button(
                                    onClick = {
                                        val dummy = Mexc24hTicker(aiRec.recommendedSymbol, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                                        onSelectCoin(dummy, aiRec.suggestedTpPct, aiRec.suggestedSlPct)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("apply_ai_coin_btn")
                                ) {
                                    Text("Pakai Koin Ini", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1.6. Kartu Harga Real-Time, Detak Pasar & Status Sinkronisasi
        val infiniteTransition = rememberInfiniteTransition(label = "bot_pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        val displayAsset = remember(asset, currentSymbol, currentPrice, candles) {
            asset ?: MarketAsset(
                symbol = currentSymbol,
                tvSymbol = "MEXC:$currentSymbol",
                displayName = currentSymbol,
                name = currentSymbol,
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.MEXC,
                currentPrice = currentPrice,
                change24h = candles.lastOrNull()?.let { last ->
                    candles.firstOrNull()?.let { first ->
                        if (first.open > 0) ((last.close - first.open) / first.open) * 100.0 else 0.0
                    }
                } ?: 0.0,
                high24h = candles.maxOfOrNull { it.high } ?: currentPrice,
                low24h = candles.minOfOrNull { it.low } ?: currentPrice,
                volume24h = candles.sumOf { it.volume },
                decimals = if (currentPrice < 1.0) 4 else 2
            )
        }

        val displayIndicators = remember(indicators, candles) {
            indicators ?: IndicatorCalculator.calculateAllIndicators(candles)
        }

        val isBull = displayAsset.change24h >= 0

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bot_realtime_price_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141C)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .background(BullGreen, CircleShape)
                        )
                        Text(
                            text = "HARGA PASAR REAL-TIME",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = pulseAlpha
                            }
                            .background(BullGreen, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = String.format(Locale.US, "$%,.${displayAsset.decimals}f", currentPrice),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isBull) BullGreen.copy(alpha = 0.15f) else BearRed.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isBull) BullGreen.copy(alpha = 0.4f) else BearRed.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%+.2f%%", displayAsset.change24h),
                                    color = if (isBull) BullGreen else BearRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Bursa: MEXC Global • Update langsung per detik",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "24h High", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = String.format(Locale.US, "$%,.${displayAsset.decimals}f", displayAsset.high24h),
                                color = BullGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "24h Low", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = String.format(Locale.US, "$%,.${displayAsset.decimals}f", displayAsset.low24h),
                                color = BearRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 1.7. Posisi Aktif & Floating PnL Real-Time (Jika ada posisi berjalan)
        if (position != null) {
            val isPositionProfit = position.unrealizedPnl >= 0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bot_active_position_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPositionProfit) Color(0xFF042013) else Color(0xFF260D12)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isPositionProfit) BullGreen else BearRed)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPositionProfit) BullGreen else BearRed)
                            )
                            Text(
                                text = "POSISI AKTIF: ${position.symbol}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Button(
                            onClick = onManualClosePosition,
                            colors = ButtonDefaults.buttonColors(containerColor = BearRed),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_close_position_now")
                        ) {
                            Text("Tutup Posisi Sekarang", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Harga Beli (Entry)", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = String.format(Locale.US, "$%,.2f", position.entryPrice),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Harga Terkini", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = String.format(Locale.US, "$%,.2f", position.currentPrice),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Floating Profit/Loss", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%s$%,.2f (%+.2f%%)",
                                    if (position.unrealizedPnl >= 0) "+" else "",
                                    position.unrealizedPnl,
                                    position.unrealizedPnlPct
                                ),
                                color = if (isPositionProfit) BullGreen else BearRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TP Target: $${String.format(Locale.US, "%,.2f", position.takeProfitPrice)}",
                            color = BullGreen,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "SL Proteksi: $${String.format(Locale.US, "%,.2f", position.stopLossPrice)}",
                            color = BearRed,
                            fontSize = 10.sp
                        )
                        if (position.trailingStopPrice > 0) {
                            Text(
                                text = "Trailing Stop: $${String.format(Locale.US, "%,.2f", position.trailingStopPrice)}",
                                color = BrandCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // 1.8. Grafik Candlestick Interaktif & Indikator Mandat
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bot_candlestick_chart_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141C)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = BrandCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "GRAFIK CANDLESTICK ($currentSymbol)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${candles.size} Candle Terkini",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Candlestick Chart
                InteractiveCandlestickChart(
                    asset = displayAsset,
                    candles = candles,
                    indicators = displayIndicators,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bot_candlestick_chart")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Live Real-Time Technical Indicators Panel (1s Pulse)
                LiveIndicatorsPanel(
                    asset = displayAsset,
                    indicators = displayIndicators,
                    initialExpanded = false
                )
            }
        }

        // 1.9. Status & Verifikasi Strategi Mandat Berjalan (QUANT DASHBOARD GRADE)
        val mandatePulseTransition = rememberInfiniteTransition(label = "pulse_mandate")
        val mandatePulseAlpha by mandatePulseTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_mandate_alpha"
        )

        val tpPrice = currentPrice * (1.0 + (botState.customTpPct / 100.0))
        val slPrice = currentPrice * (1.0 - (botState.customSlPct / 100.0))
        val rrRatio = if (botState.customSlPct > 0) String.format(Locale.US, "1 : %.1f", botState.customTpPct / botState.customSlPct) else "1 : 2.0"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bot_mandate_strategy_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (botState.isRunning) Color(0xFF091422) else Color(0xFF0F1722)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (botState.isRunning) BullGreen.copy(alpha = 0.55f) else BorderColor
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header Mandat
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
                                .size(34.dp)
                                .background(
                                    if (botState.isRunning) BullGreen.copy(alpha = 0.15f) else BrandCyan.copy(alpha = 0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (botState.isRunning) BullGreen else BrandCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "MANDAT STRATEGI BOT",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = "1s REALTIME",
                                        color = BrandCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = botState.strategy.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (botState.isRunning) BullGreen.copy(alpha = 0.18f) else Color(0xFF1F2937),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (botState.isRunning) BullGreen.copy(alpha = 0.5f) else Color(0xFF374151)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (botState.isRunning) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .graphicsLayer { alpha = mandatePulseAlpha }
                                        .background(BullGreen, CircleShape)
                                )
                            }
                            Text(
                                text = if (botState.isRunning) "MANDAT AKTIF" else "STANDBY",
                                color = if (botState.isRunning) BullGreen else Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Matriks 2x2 Parameter Mandat Strategi (Rapi, Luas & Informatif)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Baris 1: Take Profit & Stop Loss
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // TP Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF13202E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BullGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "TARGET PROFIT (TP)", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = BullGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "R:R $rrRatio",
                                            color = BullGreen,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", botState.customTpPct)}%",
                                    color = BullGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Target: $${String.format(Locale.US, "%,.2f", tpPrice)}",
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // SL Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF13202E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BearRed.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "STOP LOSS (SL)", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = BearRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "PROTEKSI",
                                            color = BearRed,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "-${String.format(Locale.US, "%.1f", botState.customSlPct)}%",
                                    color = BearRed,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Batas: $${String.format(Locale.US, "%,.2f", slPrice)}",
                                    color = Color(0xFFFECDD3),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Baris 2: Trailing Stop & Break-Even Shield
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Trailing Stop
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF13202E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyan.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "TRAILING STOP", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = BrandCyan.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "DINAMIS",
                                            color = BrandCyan,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", botState.customTrailingPct)}%",
                                    color = BrandCyan,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Kunci cuan otomatis saat tren naik",
                                    color = Color(0xFFBAE6FD),
                                    fontSize = 9.sp
                                )
                            }
                        }

                        // Break-Even Shield
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF13202E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "BREAK-EVEN SHIELD", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = Color(0xFFFBBF24).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "ZERO-LOSS",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Aktif (≥1.2%)",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "SL digeser ke modal beli saat cuan",
                                    color = Color(0xFFFEF3C7),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detail Logika Mandat Berjalan & Telemetri
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0C1322),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BullGreen, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Telemetri Mandat Per Detik:",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Confluence Badges
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF13202E)) {
                                    Text(
                                        text = "TICK 1S: ON",
                                        color = BullGreen,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF13202E)) {
                                    Text(
                                        text = "SLIPPAGE: 0.1%",
                                        color = BrandCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = botState.lastSignalReason,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        if (botState.lastGeminiConfidence > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Keyakinan Gemini AI:",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "${(botState.lastGeminiConfidence * 100).toInt()}%",
                                    color = BrandCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { botState.lastGeminiConfidence.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = BrandCyan,
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }

        // 2. Kartu Saldo & Keuntungan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141A24)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (botState.isSandbox) "Saldo Virtual Tersedia" else "Saldo Akun MEXC",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        val currentBalance = if (botState.isSandbox) botState.sandboxBalance else botState.realUsdtBalance
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", currentBalance)} USDT",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Total Keuntungan (PnL)", color = TextMuted, fontSize = 11.sp)
                        val pnl = botState.totalRealizedPnl
                        Text(
                            text = (if (pnl >= 0) "+$" else "-$") +
                                    String.format(Locale.US, "%,.2f", kotlin.math.abs(pnl)),
                            color = if (pnl >= 0) BullGreen else BearRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (botState.isSandbox) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Menang: ${botState.winTradesCount} dari ${botState.totalTradesCount} transaksi",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Reset Modal $1,000",
                            color = BrandCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onResetSandboxBalance() }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        // 3. Status Bot & Tombol Utama (Jalankan / Hentikan)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (botState.isRunning) Color(0xFF062316) else Color(0xFF161B26)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(if (botState.isRunning) BullGreen else BorderColor)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (botState.isRunning) BullGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (botState.isRunning) "Bot Aktif Memantau Pasar" else "Bot Berhenti (Standby)",
                                color = if (botState.isRunning) Color.White else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (botState.isRunning)
                                    "Gemini otomatis mengeksekusi order saat ada peluang aman"
                                else
                                    "Tekan tombol hijau untuk memulai otomatisasi",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (botState.isRunning) {
                    Button(
                        onClick = onStopBot,
                        colors = ButtonDefaults.buttonColors(containerColor = BearRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HENTIKAN BOT OTOMATIS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onStartBot,
                        colors = ButtonDefaults.buttonColors(containerColor = BullGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "JALANKAN BOT OTOMATIS SEKARANG",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 4. Tombol Aksi Instan: "Minta Gemini Eksekusi Sekarang"
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF6366F1)))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Eksekusi Cepat Gemini AI",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Minta Gemini menganalisis candle pasar saat ini secara langsung, atau lakukan eksekusi instan langsung dengan pengamanan Trailing Stop otomatis.",
                    color = Color(0xFFC7D2FE),
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onExecuteGeminiTrader,
                    enabled = !botState.isGeminiThinking,
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiPurple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    if (botState.isGeminiThinking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini sedang menimbang peluang profit...",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ MINTA GEMINI ANALISIS & EKSEKUSI",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExecuteInstantBuy,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BullGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = BullGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🚀 LANGSUNG BELI PASAR SEKARANG (MARKET BUY)",
                        color = BullGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 5. Kartu Posisi Berjalan (Jika ada koin yang sedang dibeli)
        if (position != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2E)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BrandCyan))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Koin yang Sedang Dibeli",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = position.symbol,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        val pnl = position.unrealizedPnl
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Keuntungan Berjalan", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = (if (pnl >= 0) "+$" else "-$") +
                                        String.format(Locale.US, "%,.2f", kotlin.math.abs(pnl)) +
                                        " (${String.format(Locale.US, "%+.2f", position.unrealizedPnlPct)}%)",
                                color = if (pnl >= 0) BullGreen else BearRed,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = BorderColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Harga Beli:", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", position.entryPrice)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column {
                            Text(text = "Batas Rugi (Stop Loss):", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", position.stopLossPrice)}",
                                color = BearRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Target Untung (Take Profit):", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", position.takeProfitPrice)}",
                                color = BullGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onManualClosePosition,
                        colors = ButtonDefaults.buttonColors(containerColor = BearRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = "AMBIL UNTUNG / TUTUP POSISI SEKARANG",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 6. Kartu Rencana Patokan Pemicu Gemini (Zero-API Trigger Blueprint)
        val activePlan = botState.activeBlueprint
        if (activePlan != null && !activePlan.isExpired() && position == null) {
            val triggerTarget = if (activePlan.triggerMinPrice > 0) activePlan.triggerMinPrice else activePlan.triggerMaxPrice
            val distancePct = if (currentPrice > 0 && triggerTarget > 0) {
                ((currentPrice - triggerTarget) / currentPrice) * 100.0
            } else 0.0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bot_active_blueprint_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF091B28)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Patokan Rencana Gemini (Menunggu Pemicu)",
                                color = Color(0xFFE0F2FE),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0284C7)
                        ) {
                            Text(
                                text = "Hemat API (${activePlan.getRemainingSeconds()}s)",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Perbandingan 3 Titik Harga: Analisis Awal, Pasar Terkini, Target Pemicu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF13364A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "Harga Analisis Pertama", color = TextMuted, fontSize = 9.sp)
                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", activePlan.initialAnalysisPrice)}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "Saat AI periksa chart", color = TextMuted, fontSize = 8.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF13364A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "Harga Pasar Terkini", color = Color(0xFF93C5FD), fontSize = 9.sp)
                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", currentPrice)}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (distancePct > 0) "${String.format(Locale.US, "+%.2f%%", distancePct)} dr pemicu" else "Di area pemicu",
                                    color = Color(0xFFBAE6FD),
                                    fontSize = 8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F435C),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "Target Pemicu Masuk", color = Color(0xFF7DD3FC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", activePlan.triggerMinPrice)}",
                                    color = BullGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(text = "Jaring Beli Diskon", color = BullGreen, fontSize = 8.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Kotak Penjelasan "Kenapa Belum Ada Eksekusi?"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF071C27),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E4D68)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Kenapa bot belum mengeksekusi order?",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Gemini menilai membeli langsung di harga $${String.format(Locale.US, "%,.2f", activePlan.initialAnalysisPrice)} terlalu berisiko (membeli di resistensi/pucuk). Mandat bot memasang jaring tunggu pada support $${String.format(Locale.US, "%,.2f", activePlan.triggerMinPrice)}. Bot memantau pergerakan harga per detik, dan akan langsung mengeksekusi order seketika harga pasar turun menyentuh titik pemicu.",
                                color = Color(0xFFBAE6FD),
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tombol Aksi Alternatif
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExecuteInstantBuy,
                            colors = ButtonDefaults.buttonColors(containerColor = BullGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_instant_market_buy_blueprint")
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Beli Sekarang (Pasar)",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onExecuteGeminiTrader,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_reanalyze_blueprint")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Analisis Ulang AI",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 7. Kartu Penjelasan Hasil Keputusan Terakhir Gemini
        if (botState.lastGeminiThesis.isNotBlank() || botState.lastGeminiConfidence > 0.0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B26)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keputusan Terakhir Gemini AI",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (botState.lastGeminiConfidence >= 0.75) BullGreen.copy(alpha = 0.2f) else Color(0xFF334155)
                        ) {
                            Text(
                                text = "Tingkat Keyakinan: ${String.format(Locale.US, "%.0f%%", botState.lastGeminiConfidence * 100)}",
                                color = if (botState.lastGeminiConfidence >= 0.75) BullGreen else Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = botState.lastGeminiThesis,
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 7. Log Aktivitas Terakhir (Mudah Dipahami)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Catatan Aktivitas Bot Terbaru",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val logs = botState.terminalLogs.takeLast(4).reversed()
                if (logs.isEmpty()) {
                    Text(
                        text = "Belum ada aktivitas. Bot akan mencatat setiap pemeriksaan pasar di sini.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                } else {
                    logs.forEach { log ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(text = "•", color = BrandCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = log,
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * TAB 2: Uji Coba Strategi (Backtest) pada Data Nyata MEXC
 * Memberi keyakinan pada pengguna bahwa strategi benar-benar menghasilkan profit pada data riil.
 */
@Composable
private fun SimpleBacktestTab(
    currentSymbol: String,
    backtestReport: BacktestReport?,
    isBacktesting: Boolean,
    onRunBacktest: (String, String, Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kartu Penjelasan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2A)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BrandCyan))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = BrandCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bagaimana Memastikan Bot Bisa Profit?",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gunakan fitur Backtest ini untuk membuktikan sendiri performa strategi kuantitatif pada 250 candle riil bursa MEXC. Algoritma akan disimulasikan dari masa lalu hingga sekarang lengkap dengan komisi fee bursa MEXC.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onRunBacktest(currentSymbol, "15m", 250) },
                    enabled = !isBacktesting,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    if (isBacktesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sedang Menguji pada Data MEXC...",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UJI COBA STRATEGI SEKARANG ($currentSymbol)",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Hasil Backtest
        if (backtestReport != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Hasil Pengujian pada Data Riil MEXC",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card Profit
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1722))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(text = "Keuntungan Bersih", color = TextMuted, fontSize = 9.sp)
                                val netPnl = backtestReport.totalPnlUsdt
                                Text(
                                    text = (if (netPnl >= 0) "+$" else "-$") +
                                            String.format(Locale.US, "%,.2f", kotlin.math.abs(netPnl)),
                                    color = if (netPnl >= 0) BullGreen else BearRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = String.format(Locale.US, "%+.2f%%", backtestReport.totalPnlPercent),
                                    color = if (netPnl >= 0) BullGreen else BearRed,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Card Win Rate
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1722))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(text = "Tingkat Kemenangan", color = TextMuted, fontSize = 9.sp)
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", backtestReport.winRate),
                                    color = if (backtestReport.winRate >= 50.0) BullGreen else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${backtestReport.winningTrades} Menang / ${backtestReport.losingTrades} Kalah",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Card Transaksi
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1722))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(text = "Total Transaksi", color = TextMuted, fontSize = 9.sp)
                                Text(
                                    text = "${backtestReport.totalTrades}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Rasio R:R ${String.format(Locale.US, "%.2f", backtestReport.profitFactor)}",
                                    color = BrandCyan,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Daftar Transaksi Hasil Uji Coba:",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    backtestReport.trades.take(5).forEach { trade ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Beli di $${String.format(Locale.US, "%,.2f", trade.entryPrice)} → Jual $${String.format(Locale.US, "%,.2f", trade.exitPrice)} (${trade.exitReason})",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            )
                            Text(
                                text = (if (trade.pnlUsdt >= 0) "+$" else "-$") +
                                        String.format(Locale.US, "%,.2f", kotlin.math.abs(trade.pnlUsdt)),
                                color = if (trade.pnlUsdt >= 0) BullGreen else BearRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Divider(color = BorderColor.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

/**
 * TAB 3: Pengaturan MEXC API Sederhana
 */
@Composable
private fun SimpleMexcSettingsTab(
    botState: BotUiState,
    currentSymbol: String,
    onSaveCredentials: (String, String) -> Unit,
    onTestMexcConnection: () -> Unit,
    onSendMexcTestOrder: (String) -> Unit,
    onSaveRiskParameters: (Double, Double, Double, Double) -> Unit,
    onClearHistory: () -> Unit,
    onSaveGeminiKey: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var apiKey by remember { mutableStateOf(botState.apiKey) }
    var secretKey by remember { mutableStateOf(botState.secretKey) }
    var isSecretVisible by remember { mutableStateOf(false) }
    var selectedAlloc by remember { mutableStateOf(botState.tradeAllocationPct) }
    var geminiKeyInput by remember { mutableStateOf(botState.customGeminiApiKey) }
    var isGeminiKeySaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kartu Penjelasan Ramah
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Koneksi Akun Bursa MEXC",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Jika Anda ingin bot mengeksekusi order dengan saldo asli di bursa MEXC, masukkan API Key dan Secret Key resmi Anda. Jika tidak, Anda tetap dapat menggunakan Mode Sandbox secara gratis tanpa risiko.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // Form API Key & Secret Key
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { newKey: String -> apiKey = newKey },
                    label = { Text("MEXC API Key", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { newSec: String -> secretKey = newSec },
                    label = { Text("MEXC Secret Key", fontSize = 11.sp) },
                    visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                            Icon(
                                imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextMuted
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveCredentials(apiKey.trim(), secretKey.trim())
                            onTestMexcConnection()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text("Simpan & Tes Koneksi", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { onSendMexcTestOrder(currentSymbol) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text("Kirim Test Order", color = BrandCyan, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status Koneksi: ${botState.mexcConnectionStatus}",
                    color = if (botState.mexcConnectionStatus.contains("Sukses", ignoreCase = true)) BullGreen else Color(0xFFFBBF24),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Pengaturan Modal Sederhana (Pilihan Chip)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Modal per Transaksi (Alokasi)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pilih berapa persentase modal yang digunakan setiap kali bot membuka posisi:",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(10.0, 20.0, 30.0, 50.0).forEach { pct ->
                        val isSel = (selectedAlloc == pct)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedAlloc = pct
                                    onSaveRiskParameters(pct, botState.customSlPct, botState.customTpPct, botState.customTrailingPct)
                                }
                                .background(if (isSel) BrandCyan else Color(0xFF1E2430))
                                .padding(vertical = 8.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = "${pct.toInt()}%",
                                color = if (isSel) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Pengaturan Gemini API Key Pribadi (Opsional)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = GeminiPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kunci Gemini API (Opsional / Kuota Pribadi)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gunakan API Key Gemini pribadi Anda untuk kuota tak terbatas dan bebas 429 rate limit. Jika dikosongkan, bot otomatis menggunakan kunci bawaan dengan fallback kuantitatif lokal.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = geminiKeyInput,
                    onValueChange = { 
                        geminiKeyInput = it
                        isGeminiKeySaved = false
                    },
                    label = { Text("Gemini API Key (AI Studio)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeminiPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onSaveGeminiKey(geminiKeyInput.trim())
                            isGeminiKeySaved = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Simpan Kunci Gemini", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    if (isGeminiKeySaved) {
                        Text(
                            text = "✓ Kunci Gemini Disimpan!",
                            color = BullGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tombol Hapus Riwayat
        OutlinedButton(
            onClick = onClearHistory,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            Text(text = "Hapus Riwayat Transaksi Bot", color = TextMuted, fontSize = 10.sp)
        }
    }
}
