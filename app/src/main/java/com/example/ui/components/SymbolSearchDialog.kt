package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AssetType
import com.example.data.model.ExchangePlatform
import com.example.data.model.SearchResultItem
import com.example.ui.theme.BullGreen
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymbolSearchDialog(
    searchResults: List<SearchResultItem>,
    isSearching: Boolean,
    onSearch: (query: String, filter: String?) -> Unit,
    onSelectResult: (SearchResultItem) -> Unit,
    onAddDirectSymbol: (symbol: String, exchange: String, type: AssetType) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "CRYPTO", "FOREX", "COMMODITY"
    var directSymbolInput by remember { mutableStateOf("") }
    var directExchange by remember { mutableStateOf("BINANCE") }
    var showDirectInputSection by remember { mutableStateOf(false) }

    // Debounce search query
    LaunchedEffect(searchQuery, selectedFilter) {
        if (searchQuery.isNotBlank()) {
            delay(350)
            val filterParam = if (selectedFilter == "ALL") null else selectedFilter
            onSearch(searchQuery, filterParam)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("symbol_search_dialog"),
            shape = RoundedCornerShape(18.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Header
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
                                .size(32.dp)
                                .background(Ema9Cyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Ema9Cyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Eksplorasi Pasar Global",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Didukung Feed TradingView (Crypto, Forex, Emas & Komoditas)",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_search_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_symbol_input"),
                    placeholder = {
                        Text("Cari: SOL, PEPE, GOLD, GBP, EUR, DOGE, USOIL...", color = TextMuted, fontSize = 13.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Ema9Cyan)
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Ema9Cyan
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TerminalBg,
                        unfocusedContainerColor = TerminalBg,
                        focusedBorderColor = Ema9Cyan,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Pair("ALL", "Semua"),
                        Pair("CRYPTO", "Crypto"),
                        Pair("FOREX", "Forex"),
                        Pair("COMMODITY", "Komoditas")
                    ).forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ema9Cyan.copy(alpha = 0.2f),
                                selectedLabelColor = Ema9Cyan,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Popular Assets (when search is empty)
                if (searchQuery.isBlank()) {
                    Text(
                        text = "Pencarian Populer",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Triple("SOLUSDT", "BINANCE:SOLUSDT", "SOL/USDT"),
                            Triple("PEPEUSDT", "BINANCE:PEPEUSDT", "PEPE/USDT"),
                            Triple("XAUUSD", "OANDA:XAUUSD", "Gold (Emas)"),
                            Triple("XAGUSD", "OANDA:XAGUSD", "Silver (Perak)"),
                            Triple("USOIL", "TVC:USOIL", "Crude Oil"),
                            Triple("GBPUSD", "OANDA:GBPUSD", "GBP/USD"),
                            Triple("USDJPY", "OANDA:USDJPY", "USD/JPY"),
                            Triple("DOGEUSDT", "BINANCE:DOGEUSDT", "DOGE/USDT"),
                            Triple("SUIUSDT", "BINANCE:SUIUSDT", "SUI/USDT"),
                            Triple("BTCUSDT", "BINANCE:BTCUSDT", "BTC/USDT")
                        ).forEach { (sym, tv, label) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceCard,
                                border = BorderStroke(1.dp, SurfaceCardBorder),
                                modifier = Modifier.clickable {
                                    searchQuery = sym
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Results List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (searchResults.isEmpty() && searchQuery.isNotBlank() && !isSearching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Simbol tidak ditemukan di pencarian instan.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Gunakan input langsung di bawah untuk memasukkan ticker custom.",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    items(searchResults) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectResult(item)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, SurfaceCardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.displayName,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (item.platform) {
                                                ExchangePlatform.BINANCE_SPOT -> WarningGold.copy(alpha = 0.15f)
                                                ExchangePlatform.OANDA_TRADINGVIEW -> Ema9Cyan.copy(alpha = 0.15f)
                                                ExchangePlatform.BYBIT -> WarningGold.copy(alpha = 0.2f)
                                                ExchangePlatform.OKX -> Ema9Cyan.copy(alpha = 0.2f)
                                                ExchangePlatform.FXCM -> Ema9Cyan.copy(alpha = 0.15f)
                                                else -> BullGreen.copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = item.exchangeBadge,
                                                color = when (item.platform) {
                                                    ExchangePlatform.BINANCE_SPOT -> WarningGold
                                                    ExchangePlatform.OANDA_TRADINGVIEW -> Ema9Cyan
                                                    ExchangePlatform.BYBIT -> WarningGold
                                                    ExchangePlatform.OKX -> Ema9Cyan
                                                    ExchangePlatform.FXCM -> Ema9Cyan
                                                    else -> BullGreen
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SurfaceElevated
                                        ) {
                                            Text(
                                                text = item.type.label,
                                                color = TextSecondary,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.name} • ${item.tvSymbol}",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    onClick = {
                                        onSelectResult(item)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Ema9Cyan.copy(alpha = 0.2f),
                                        contentColor = Ema9Cyan
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Pilih", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Direct Custom TradingView Symbol Input Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = TerminalBg,
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDirectInputSection = !showDirectInputSection },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Input Ticker TradingView Langsung",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (showDirectInputSection) "Tutup" else "Buka",
                                color = Ema9Cyan,
                                fontSize = 11.sp
                            )
                        }

                        if (showDirectInputSection) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = directSymbolInput,
                                    onValueChange = { directSymbolInput = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = {
                                        Text("misal: BINANCE:PEPEUSDT", color = TextMuted, fontSize = 11.sp)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceDark,
                                        unfocusedContainerColor = SurfaceDark,
                                        focusedBorderColor = Ema9Cyan,
                                        unfocusedBorderColor = SurfaceCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (directSymbolInput.isNotBlank()) {
                                            val exchange = if (directSymbolInput.contains(":")) {
                                                directSymbolInput.substringBefore(":")
                                            } else {
                                                directExchange
                                            }
                                            val type = if (directSymbolInput.contains("USDT") || directSymbolInput.contains("BTC")) {
                                                AssetType.CRYPTO
                                            } else if (directSymbolInput.startsWith("XAU") || directSymbolInput.contains("OIL")) {
                                                AssetType.COMMODITY
                                            } else {
                                                AssetType.FOREX
                                            }
                                            onAddDirectSymbol(directSymbolInput, exchange, type)
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BullGreen,
                                        contentColor = TerminalBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Muat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
