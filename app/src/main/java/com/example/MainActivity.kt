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

    var showAlertDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("ALL") } // "ALL", "CRYPTO", "FOREX", "COMMODITY"

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
                                        Icon(
                                            Icons.Default.Circle,
                                            contentDescription = null,
                                            tint = BullGreen,
                                            modifier = Modifier.size(6.dp)
                                        )
                                        Text(
                                            text = "LIVE",
                                            color = BullGreen,
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

            // Current Asset Info & Timeframe Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.selectedAsset.displayName,
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Ema9Cyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = uiState.selectedAsset.platform.badgeLabel,
                                        color = Ema9Cyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.selectedAsset.name} • ${uiState.selectedAsset.tvSymbol}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Timeframe Pills (1m, 5m, 15m, 1h, 4h, 1D)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Timeframe.entries.forEach { tf ->
                                val isSelected = uiState.selectedTimeframe == tf
                                Surface(
                                    onClick = { viewModel.selectTimeframe(tf) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Ema9Cyan else SurfaceCard,
                                    modifier = Modifier.testTag("timeframe_${tf.label}")
                                ) {
                                    Text(
                                        text = tf.label,
                                        color = if (isSelected) TerminalBg else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Price & 24h Stats Row
                    val isBull = uiState.selectedAsset.change24h >= 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.US, "%.${uiState.selectedAsset.decimals}f", uiState.selectedAsset.currentPrice),
                                color = TextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "%+.2f%%", uiState.selectedAsset.change24h),
                                color = if (isBull) BullGreen else BearRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // High / Low Stats
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "24h H: ${String.format(Locale.US, "%.${uiState.selectedAsset.decimals}f", uiState.selectedAsset.high24h)}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "24h L: ${String.format(Locale.US, "%.${uiState.selectedAsset.decimals}f", uiState.selectedAsset.low24h)}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
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

            // Live Feed & Auto-Refresh Status Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_refresh_status_bar"),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Live status dot & Countdown
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulsing Live Indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (uiState.isAutoRefreshEnabled) BullGreen else TextMuted,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (uiState.isAutoRefreshEnabled) {
                                if (uiState.isRefreshingPrice) "Memperbarui..." else "Auto Refresh: ${uiState.refreshCountdown}s"
                            } else {
                                "Auto Refresh: Dijeda"
                            },
                            color = if (uiState.isAutoRefreshEnabled) TextPrimary else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Last updated time
                        val timeStr = remember(uiState.lastRefreshedTimeMillis) {
                            java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date(uiState.lastRefreshedTimeMillis))
                        }
                        Text(
                            text = "($timeStr)",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    // Right: Controls (Interval options, Pause/Resume, Instant Refresh)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Interval pills: 5s, 10s, 30s
                        listOf(5, 10, 30).forEach { sec ->
                            val isSel = uiState.autoRefreshIntervalSeconds == sec
                            Surface(
                                onClick = { viewModel.setAutoRefreshInterval(sec) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) Ema9Cyan.copy(alpha = 0.2f) else SurfaceCard,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, Ema9Cyan) else null,
                                modifier = Modifier.testTag("interval_${sec}s")
                            ) {
                                Text(
                                    text = "${sec}s",
                                    color = if (isSel) Ema9Cyan else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Play/Pause button
                        IconButton(
                            onClick = { viewModel.toggleAutoRefresh() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("toggle_auto_refresh_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isAutoRefreshEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isAutoRefreshEnabled) "Jeda Auto Refresh" else "Lanjutkan Auto Refresh",
                                tint = if (uiState.isAutoRefreshEnabled) WarningGold else BullGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Manual Refresh Button
                        IconButton(
                            onClick = { viewModel.triggerImmediateRefresh() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("instant_refresh_button")
                        ) {
                            if (uiState.isRefreshingPrice) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Ema9Cyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Sekarang",
                                    tint = Ema9Cyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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
    }
}
