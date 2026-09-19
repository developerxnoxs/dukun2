package com.example.data.mexc

import java.util.Locale

data class MexcServerTime(
    val serverTime: Long
)

data class MexcBalance(
    val asset: String,
    val free: Double,
    val locked: Double
) {
    val total: Double get() = free + locked
}

data class MexcAccountInfo(
    val makerCommission: Int = 10,
    val takerCommission: Int = 10,
    val canTrade: Boolean = true,
    val canWithdraw: Boolean = false,
    val canDeposit: Boolean = false,
    val updateTime: Long = 0L,
    val balances: List<MexcBalance> = emptyList()
) {
    fun getUsdtFree(): Double = balances.find { it.asset.equals("USDT", ignoreCase = true) }?.free ?: 0.0
    fun getAssetFree(asset: String): Double = balances.find { it.asset.equals(asset, ignoreCase = true) }?.free ?: 0.0
}

data class MexcOrderResponse(
    val symbol: String,
    val orderId: String,
    val orderListId: Long = -1,
    val clientOrderId: String? = null,
    val transactTime: Long = System.currentTimeMillis(),
    val price: Double = 0.0,
    val origQty: Double = 0.0,
    val executedQty: Double = 0.0,
    val status: String = "FILLED",
    val timeInForce: String = "GTC",
    val type: String = "MARKET",
    val side: String = "BUY",
    val isTestOrder: Boolean = false
)

data class MexcTickerPrice(
    val symbol: String,
    val price: Double
)

data class Mexc24hTicker(
    val symbol: String,
    val priceChange: Double,
    val priceChangePercent: Double,
    val lastPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volume: Double,
    val quoteVolume: Double
) {
    val baseAsset: String
        get() = symbol.removeSuffix("USDT")

    val isPositive: Boolean
        get() = priceChangePercent >= 0

    val spreadPercent: Double
        get() = if (lowPrice > 0) ((highPrice - lowPrice) / lowPrice) * 100.0 else 0.0

    val formattedVolume: String
        get() = when {
            quoteVolume >= 1_000_000_000 -> String.format(Locale.US, "$%.2fB", quoteVolume / 1_000_000_000.0)
            quoteVolume >= 1_000_000 -> String.format(Locale.US, "$%.2fM", quoteVolume / 1_000_000.0)
            quoteVolume >= 1_000 -> String.format(Locale.US, "$%.1fK", quoteVolume / 1_000.0)
            else -> String.format(Locale.US, "$%.0f", quoteVolume)
        }
}

data class AiCoinSelectionResult(
    val recommendedSymbol: String,
    val confidence: Double,
    val setupCategory: String,
    val thesis: String,
    val riskLevel: String,
    val suggestedTpPct: Double,
    val suggestedSlPct: Double,
    val isLiveGemini: Boolean = false
)

data class MexcApiError(
    val code: Int,
    override val message: String
) : Exception("MEXC API Error ($code): $message")
