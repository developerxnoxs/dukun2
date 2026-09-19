package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssetType
import com.example.data.model.Timeframe
import com.example.ui.MarketViewModel
import com.example.ui.components.AiAnalysisCard
import com.example.ui.components.AlertsDialog
import com.example.ui.components.InteractiveCandlestickChart
import com.example.ui.components.SymbolSearchDialog
import com.example.ui.components.WatchlistBar
import com.example.ui.components.bot.TradingBotDialog
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.Ema9Cyan
import com.example.ui.theme.MyApplicationTheme
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

class MainActivity : ComponentActivity() {

    private val viewModel: MarketViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MarketViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val botState by viewModel.botState.collectAsState()
    val backtestReport by viewModel.backtestReport.collectAsState()
    val isBacktesting by viewModel.isBacktesting.collectAsState()

    var showAlertDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showBotDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("ALL") } // "ALL", "CRYPTO", "FOREX", "COMMODITY"

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
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

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val filteredAssets = when (selectedCategory) {
        "CRYPTO" -> uiState.assets.filter { it.type == AssetType.CRYPTO }
        "FOREX" -> uiState.assets.filter { it.type == AssetType.FOREX }
        "COMMODITY" -> uiState.assets.filter { it.type == AssetType.COMMODITY }
        else -> uiState.assets
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_screen"),
        containerColor = TerminalBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BullGreen.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = BullGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MarketAI",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SurfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .graphicsLayer {
                                                    scaleX = pulseScale
                                                    scaleY = pulseScale
                                                    alpha = pulseAlpha
                                                }
                                                .background(BullGreen, CircleShape)
                                        )
                                        Text(
                                            text = if (uiState.isRefreshingPrice) "SYNC..." else "LIVE ${uiState.refreshCountdown}s",
                                            color = if (uiState.isRefreshingPrice) Ema9Cyan else BullGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Crypto, Forex & Komoditas Technical Analyst",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // MEXC AutoTrading Bot Direct Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (botState.isRunning) BullGreen.copy(alpha = 0.18f) else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (botState.isRunning) BullGreen else SurfaceCardBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showBotDialog = true }
                            .testTag("top_bot_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AutoTrading Bot",
                                tint = if (botState.isRunning) BullGreen else Ema9Cyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (botState.isRunning) "BOT ON" else "MEXC BOT",
                                color = if (botState.isRunning) BullGreen else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Search & Add Symbol Button
                    IconButton(
                        onClick = { showSearchDialog = true },
                        modifier = Modifier.testTag("top_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Cari Simbol Pasar", tint = Ema9Cyan)
                    }

                    IconButton(
                        onClick = { viewModel.triggerImmediateRefresh() },
                        modifier = Modifier.testTag("top_refresh_button")
                    ) {
                        if (uiState.isRefreshingPrice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Ema9Cyan
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                        }
                    }

                    // Alerts and Signals Trigger Bell
                    IconButton(
                        onClick = { showAlertDialog = true },
                        modifier = Modifier.testTag("top_alerts_button")
                    ) {
                        BadgedBox(
                            badge = {
                                val totalSignals = uiState.triggeredSignals.size + uiState.activeAlerts.size
                                if (totalSignals > 0) {
                                    Badge(
                                        containerColor = BullGreen,
                                        contentColor = TerminalBg
                                    ) {
                                        Text("$totalSignals", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts & Signals",
                                tint = if (uiState.triggeredSignals.isNotEmpty()) WarningGold else TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Pair("ALL", "Semua"),
                    Pair("CRYPTO", "Crypto"),
                    Pair("FOREX", "Forex"),
                    Pair("COMMODITY", "Komoditas")
                ).forEach { (catKey, catLabel) ->
                    val isSelected = selectedCategory == catKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = catKey },
                        label = { Text(catLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Ema9Cyan.copy(alpha = 0.2f),
                            selectedLabelColor = Ema9Cyan,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("filter_chip_$catKey")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    onClick = { showSearchDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Ema9Cyan.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Ema9Cyan.copy(alpha = 0.3f)),
                    modifier = Modifier.testTag("filter_add_symbol_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Ema9Cyan, modifier = Modifier.size(14.dp))
                        Text("+ Cari", color = Ema9Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Watchlist Horizontal Scroll
            WatchlistBar(
                assets = filteredAssets,
                selectedAsset = uiState.selectedAsset,
                onSelectAsset = { viewModel.selectAsset(it) },
                onOpenSearch = { showSearchDialog = true }
            )

            // Current Asset Info Card (Clean & Spacious Symbol Display)
            val isBull = uiState.selectedAsset.change24h >= 0
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("asset_info_card"),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row: Symbol Name & Badges (Left) | Price & 24h % (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Symbol and tags
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = uiState.selectedAsset.displayName,
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Ema9Cyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Ema9Cyan.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = uiState.selectedAsset.platform.badgeLabel,
                                        color = Ema9Cyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SurfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                                ) {
                                    Text(
                                        text = uiState.selectedAsset.type.name,
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.selectedAsset.name} • ${uiState.selectedAsset.tvSymbol}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        // Right: Live Price & 24h Change Pill
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .graphicsLayer {
                                            alpha = pulseAlpha
                                        }
                                        .background(if (isBull) BullGreen else BearRed, CircleShape)
                                )
                                Text(
                                    text = String.format(Locale.US, "%,.${uiState.selectedAsset.decimals}f", uiState.selectedAsset.currentPrice),
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isBull) BullGreen.copy(alpha = 0.15f) else BearRed.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isBull) BullGreen.copy(alpha = 0.35f) else BearRed.copy(alpha = 0.35f)
                                )
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%+.2f%%", uiState.selectedAsset.change24h),
                                    color = if (isBull) BullGreen else BearRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Bottom Row: 24h High & Low Range Stats
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
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
                                Text(text = "24h High", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%,.${uiState.selectedAsset.decimals}f", uiState.selectedAsset.high24h),
                                    color = BullGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "24h Low", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%,.${uiState.selectedAsset.decimals}f", uiState.selectedAsset.low24h),
                                    color = BearRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Dedicated Timeframe Segmented Control (Rapi, Seimbang & Presisi)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeframe_selector_bar"),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Timeframe.entries.forEach { tf ->
                        val isSelected = uiState.selectedTimeframe == tf
                        Surface(
                            onClick = { viewModel.selectTimeframe(tf) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Ema9Cyan else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("timeframe_${tf.label}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tf.label,
                                    color = if (isSelected) TerminalBg else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Error / Network Glitch Banner
            uiState.errorMessage?.let { errorMsg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BearRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BearRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pemberitahuan Feed Data Server",
                                color = BearRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = errorMsg,
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.loadMarketData() },
                            colors = ButtonDefaults.buttonColors(containerColor = BearRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Hubungkan Ulang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Interactive Candlestick Chart (drag/zoom/crosshair, EMA, Bollinger, RSI/MACD)
            InteractiveCandlestickChart(
                asset = uiState.selectedAsset,
                candles = uiState.candles,
                indicators = uiState.indicators
            )

            // MEXC AutoTrading Bot Quick Control Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                    .clickable { showBotDialog = true }
                    .testTag("bot_quick_card"),
                color = SurfaceCard
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (botState.isRunning) BullGreen.copy(alpha = 0.2f) else SurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = if (botState.isRunning) BullGreen else Ema9Cyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Bot Trading Gemini AI",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (botState.isSandbox) Color(0xFF1E3A8A) else Color(0xFF7F1D1D)
                                    ) {
                                        Text(
                                            text = if (botState.isSandbox) "LATIHAN SANDBOX" else "REAL MEXC",
                                            color = if (botState.isSandbox) Color(0xFF93C5FD) else Color(0xFFFCA5A5),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (botState.isGeminiThinking) "🧠 Gemini sedang menganalisis pasar..."
                                           else if (botState.isRunning) "🟢 Bot aktif mencari profit di ${uiState.selectedAsset.symbol}"
                                           else "⚪ Standby • Tekan untuk kontrol bot",
                                    color = if (botState.isGeminiThinking) Color(0xFFA5B4FC) else if (botState.isRunning) BullGreen else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Status / Action Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (botState.isRunning) BullGreen.copy(alpha = 0.15f) else Ema9Cyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (botState.isRunning) BullGreen.copy(alpha = 0.5f) else Ema9Cyan.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = if (botState.isRunning) "KONTROL BOT (AKTIF)" else "BUKA KONTROL BOT",
                                color = if (botState.isRunning) BullGreen else Ema9Cyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // If active position, display live summary inside card
                    val activePos = botState.activePosition
                    if (activePos != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Posisi Terbuka: ${activePos.symbol} ($${String.format(Locale.US, "%,.2f", activePos.entryPrice)})",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = (if (activePos.unrealizedPnl >= 0) "+$" else "-$") +
                                        String.format(Locale.US, "%,.2f", kotlin.math.abs(activePos.unrealizedPnl)) +
                                        " (${String.format(Locale.US, "%+.2f%%", activePos.unrealizedPnlPct)})",
                                color = if (activePos.unrealizedPnl >= 0) BullGreen else BearRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // AI Technical Analysis Card (Gemini Multimodal Vision)
            AiAnalysisCard(
                asset = uiState.selectedAsset,
                timeframe = uiState.selectedTimeframe,
                analysisResult = uiState.aiAnalysisResult,
                isAnalyzing = uiState.isAiAnalyzing,
                latestChartBitmap = uiState.latestChartBitmap,
                onAnalyzeClick = { viewModel.requestAiAnalysis() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Price Alerts & Live Signals Dialog
        if (showAlertDialog) {
            AlertsDialog(
                currentAsset = uiState.selectedAsset,
                activeAlerts = uiState.activeAlerts,
                triggeredSignals = uiState.triggeredSignals,
                onDismiss = { showAlertDialog = false },
                onAddAlert = { alert ->
                    viewModel.addAlert(alert)
                },
                onDeleteAlert = { alertId ->
                    viewModel.deleteAlert(alertId)
                }
            )
        }

        // Global Market Symbol Search & Explore Dialog
        if (showSearchDialog) {
            SymbolSearchDialog(
                searchResults = uiState.searchResults,
                isSearching = uiState.isSearching,
                onSearch = { query, filter ->
                    viewModel.searchSymbols(query, filter)
                },
                onSelectResult = { resultItem ->
                    viewModel.addCustomAsset(resultItem)
                },
                onAddDirectSymbol = { symbol, exchange, type ->
                    viewModel.addDirectSymbol(symbol, exchange, type)
                },
                onDismiss = {
                    showSearchDialog = false
                    viewModel.clearSearchResults()
                }
            )
        }

        // MEXC AutoTrading Bot & Sandbox Backtest Dialog
        if (showBotDialog) {
            TradingBotDialog(
                botState = botState,
                currentSymbol = uiState.selectedAsset.symbol,
                currentPrice = uiState.selectedAsset.currentPrice,
                candles = uiState.candles,
                backtestReport = backtestReport,
                isBacktesting = isBacktesting,
                onDismiss = { showBotDialog = false },
                onStartBot = { viewModel.tradingBotManager.startBot() },
                onStopBot = { viewModel.tradingBotManager.stopBot() },
                onToggleSandbox = { viewModel.tradingBotManager.setSandboxMode(it) },
                onSaveCredentials = { key, sec -> viewModel.tradingBotManager.setCredentials(key, sec) },
                onTestMexcConnection = { viewModel.tradingBotManager.testMexcConnection() },
                onSendMexcTestOrder = { sym -> viewModel.tradingBotManager.sendQuickMexcTestOrder(sym) },
                onSelectStrategy = { st -> viewModel.tradingBotManager.setStrategy(st) },
                onSaveRiskParameters = { alloc, sl, tp, tr ->
                    viewModel.tradingBotManager.updateRiskParameters(alloc, sl, tp, tr)
                },
                onRunBacktest = { sym, tf, lim -> viewModel.runMexcBacktest(sym, tf, lim) },
                onManualClosePosition = { viewModel.tradingBotManager.manualClosePosition() },
                onResetSandboxBalance = { viewModel.tradingBotManager.resetSandboxBalance() },
                onClearHistory = { viewModel.tradingBotManager.clearTradeHistory() },
                onExecuteGeminiTrader = { viewModel.executeGeminiTraderNow() },
                onExecuteInstantBuy = { viewModel.executeInstantBuyNow() },
                onSaveGeminiKey = { key -> viewModel.updateGeminiApiKey(key) },
                onRefreshScreener = { viewModel.tradingBotManager.refreshCoinScreener() },
                onRunAiCoinSelection = { cat -> viewModel.tradingBotManager.runAiCoinSelection(cat) },
                onSelectCoin = { ticker, tp, sl -> viewModel.selectMexcCoin(ticker, tp, sl) },
                onSelectScreenerCategory = { cat -> viewModel.tradingBotManager.setScreenerCategory(cat) },
                asset = uiState.selectedAsset,
                indicators = uiState.indicators,
                refreshCountdown = uiState.refreshCountdown,
                isRefreshingPrice = uiState.isRefreshingPrice
            )
        }
    }
}
