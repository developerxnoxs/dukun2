package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExchangePlatform
import com.example.data.model.MarketAsset
import com.example.data.model.SignalAction
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.Ema9Cyan
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarningGold
import java.util.Locale

@Composable
fun WatchlistBar(
    assets: List<MarketAsset>,
    selectedAsset: MarketAsset,
    onSelectAsset: (MarketAsset) -> Unit,
    onOpenSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uniqueAssets = androidx.compose.runtime.remember(assets) { assets.distinctBy { it.tvSymbol } }
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("watchlist_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(uniqueAssets, key = { it.tvSymbol }) { asset ->
            val isSelected = asset.tvSymbol == selectedAsset.tvSymbol
            val isBull = asset.change24h >= 0

            Surface(
                onClick = { onSelectAsset(asset) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) SurfaceElevated else SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) Ema9Cyan else SurfaceCardBorder
                ),
                modifier = Modifier.testTag("asset_item_${asset.tvSymbol.replace(':', '_')}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Header: Symbol & Change %
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = asset.displayName,
                            color = if (isSelected) Ema9Cyan else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isBull) BullGreen.copy(alpha = 0.15f) else BearRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%+.2f%%", asset.change24h),
                                color = if (isBull) BullGreen else BearRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Price
                    Text(
                        text = String.format(Locale.US, "%.${asset.decimals}f", asset.currentPrice),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Platform & TV Consensus Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = when (asset.platform) {
                                ExchangePlatform.BINANCE_SPOT -> WarningGold.copy(alpha = 0.15f)
                                ExchangePlatform.OANDA_TRADINGVIEW -> Ema9Cyan.copy(alpha = 0.15f)
                                ExchangePlatform.BYBIT -> WarningGold.copy(alpha = 0.2f)
                                ExchangePlatform.OKX -> Ema9Cyan.copy(alpha = 0.2f)
                                ExchangePlatform.FXCM -> Ema9Cyan.copy(alpha = 0.15f)
                                else -> BullGreen.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = asset.exchangeBadge,
                                color = when (asset.platform) {
                                    ExchangePlatform.BINANCE_SPOT -> WarningGold
                                    ExchangePlatform.OANDA_TRADINGVIEW -> Ema9Cyan
                                    ExchangePlatform.BYBIT -> WarningGold
                                    ExchangePlatform.OKX -> Ema9Cyan
                                    ExchangePlatform.FXCM -> Ema9Cyan
                                    else -> BullGreen
                                },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        asset.tvRating?.let { rating ->
                            val ratingColor = when (rating.action) {
                                SignalAction.STRONG_BUY, SignalAction.BUY -> BullGreen
                                SignalAction.STRONG_SELL, SignalAction.SELL -> BearRed
                                SignalAction.NEUTRAL -> TextMuted
                            }
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = ratingColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "TV: ${rating.action.label}",
                                    color = ratingColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Symbol button at the end
        item {
            Surface(
                onClick = onOpenSearch,
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.testTag("add_symbol_watchlist_button")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "+ Cari",
                        color = Ema9Cyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Semua Pasar",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
