package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertType
import com.example.data.model.LiveSignal
import com.example.data.model.MarketAsset
import com.example.data.model.PriceAlert
import com.example.data.model.SignalAction
import com.example.ui.theme.BearRed
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertsDialog(
    currentAsset: MarketAsset,
    activeAlerts: List<PriceAlert>,
    triggeredSignals: List<LiveSignal>,
    onDismiss: () -> Unit,
    onAddAlert: (PriceAlert) -> Unit,
    onDeleteAlert: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Buat Alert & Aktif, 1: Riwayat Sinyal Real-Time

    // Create Alert State
    var alertType by remember { mutableStateOf(AlertType.PRICE_ABOVE) }
    var targetPriceStr by remember {
        val defaultTarget = if (alertType == AlertType.PRICE_ABOVE) {
            currentAsset.currentPrice * 1.01
        } else {
            currentAsset.currentPrice * 0.99
        }
        mutableStateOf(String.format(Locale.US, "%.${currentAsset.decimals}f", defaultTarget))
    }
    var alertNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("alerts_dialog"),
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = WarningGold)
                Text(
                    text = "Sinyal & Peringatan Harga",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceCard,
                    contentColor = Ema9Cyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Ema9Cyan
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Peringatan (${activeAlerts.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Feed Sinyal (${triggeredSignals.size})", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // Create Alert Form
                    Text(
                        text = "Setel Peringatan untuk ${currentAsset.displayName}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = alertType == AlertType.PRICE_ABOVE,
                            onClick = {
                                alertType = AlertType.PRICE_ABOVE
                                targetPriceStr = String.format(Locale.US, "%.${currentAsset.decimals}f", currentAsset.currentPrice * 1.015)
                            },
                            label = { Text("Harga Naik >", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BullGreen.copy(alpha = 0.2f),
                                selectedLabelColor = BullGreen
                            )
                        )
                        FilterChip(
                            selected = alertType == AlertType.PRICE_BELOW,
                            onClick = {
                                alertType = AlertType.PRICE_BELOW
                                targetPriceStr = String.format(Locale.US, "%.${currentAsset.decimals}f", currentAsset.currentPrice * 0.985)
                            },
                            label = { Text("Harga Turun <", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BearRed.copy(alpha = 0.2f),
                                selectedLabelColor = BearRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = targetPriceStr,
                        onValueChange = { targetPriceStr = it },
                        label = { Text("Target Harga") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Ema9Cyan,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_target_price")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick percentage offset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-2.0, -1.0, 1.0, 2.0).forEach { pct ->
                            val target = currentAsset.currentPrice * (1.0 + (pct / 100.0))
                            Surface(
                                onClick = {
                                    alertType = if (pct > 0) AlertType.PRICE_ABOVE else AlertType.PRICE_BELOW
                                    targetPriceStr = String.format(Locale.US, "%.${currentAsset.decimals}f", target)
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceElevated,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${if (pct > 0) "+" else ""}${pct.toInt()}%",
                                        color = if (pct > 0) BullGreen else BearRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val priceVal = targetPriceStr.toDoubleOrNull() ?: currentAsset.currentPrice
                            val alert = PriceAlert(
                                symbol = currentAsset.symbol,
                                type = alertType,
                                targetValue = priceVal,
                                note = if (alertType == AlertType.PRICE_ABOVE) "Target resistance tercapai" else "Target support tersentuh"
                            )
                            onAddAlert(alert)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BullGreen,
                            contentColor = TerminalBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_alert")
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pasang Peringatan", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Daftar Alert Aktif (${activeAlerts.size})",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (activeAlerts.isEmpty()) {
                        Text(
                            text = "Belum ada alert harga aktif.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(130.dp)) {
                            items(activeAlerts, key = { it.id }) { alert ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(SurfaceCard, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (alert.type == AlertType.PRICE_ABOVE) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (alert.type == AlertType.PRICE_ABOVE) BullGreen else BearRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "${alert.symbol} ${if (alert.type == AlertType.PRICE_ABOVE) "≥" else "≤"} ${alert.targetValue}",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                            Text(text = alert.note, color = TextMuted, fontSize = 10.sp)
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteAlert(alert.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Feed Sinyal Real-Time
                    if (triggeredSignals.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada sinyal terpicu saat ini.\nSinyal real-time dari AI dan indikator akan muncul otomatis di sini.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.height(280.dp)) {
                            items(triggeredSignals, key = { it.id }) { signal ->
                                val actionColor = when (signal.action) {
                                    SignalAction.STRONG_BUY, SignalAction.BUY -> BullGreen
                                    SignalAction.STRONG_SELL, SignalAction.SELL -> BearRed
                                    SignalAction.NEUTRAL -> WarningGold
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(actionColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (signal.action in listOf(SignalAction.BUY, SignalAction.STRONG_BUY)) "BUY" else "SELL",
                                                color = actionColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${signal.symbol} • ${signal.action.label}",
                                                    color = actionColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),
                                                        color = Ema9Cyan.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = signal.platform,
                                                            color = Ema9Cyan,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.timestamp)),
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = signal.message,
                                                color = TextPrimary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = Ema9Cyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}
