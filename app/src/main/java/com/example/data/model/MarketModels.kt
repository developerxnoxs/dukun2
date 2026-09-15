package com.example.data.model

enum class AssetType(val label: String) {
    CRYPTO("Crypto"),
    FOREX("Forex"),
    COMMODITY("Komoditas")
}

enum class ExchangePlatform(
    val title: String,
    val provider: String,
    val badgeLabel: String,
    val isProductionApi: Boolean = true
) {
    BINANCE_SPOT(
        title = "TradingView (Binance Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "BINANCE"
    ),
    OANDA_TRADINGVIEW(
        title = "TradingView (OANDA Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "OANDA"
    ),
    BYBIT(
        title = "TradingView (Bybit Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "BYBIT"
    ),
    COINBASE(
        title = "TradingView (Coinbase Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "COINBASE"
    ),
    OKX(
        title = "TradingView (OKX Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "OKX"
    ),
    FXCM(
        title = "TradingView (FXCM Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "FXCM"
    ),
    TVC(
        title = "TradingView (TVC Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "TVC"
    ),
    KRAKEN(
        title = "TradingView (Kraken Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "KRAKEN"
    ),
    KUCOIN(
        title = "TradingView (KuCoin Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "KUCOIN"
    ),
    BITGET(
        title = "TradingView (Bitget Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "BITGET"
    ),
    PEPPERSTONE(
        title = "TradingView (Pepperstone Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "PEPPERSTONE"
    ),
    SAXO(
        title = "TradingView (Saxo Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "SAXO"
    ),
    CAPITALCOM(
        title = "TradingView (Capital.com Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "CAPITAL.COM"
    ),
    NASDAQ(
        title = "TradingView (NASDAQ Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "NASDAQ"
    ),
    NYSE(
        title = "TradingView (NYSE Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "NYSE"
    ),
    IDX(
        title = "TradingView (BEI / IDX Feed)",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "BEI"
    ),
    MEXC(
        title = "MEXC Spot Engine Feed",
        provider = "MEXC Spot V3 API (api.mexc.com)",
        badgeLabel = "MEXC"
    ),
    OTHER(
        title = "TradingView Live Feed",
        provider = "TradingView WebSocket via Xnoxs Engine",
        badgeLabel = "TRADINGVIEW"
    );

    companion object {
        fun fromExchange(exchange: String?): ExchangePlatform {
            val ex = exchange?.uppercase() ?: ""
            return when {
                ex.contains("MEXC") -> MEXC
                ex.contains("BINANCE") -> BINANCE_SPOT
                ex.contains("OANDA") -> OANDA_TRADINGVIEW
                ex.contains("BYBIT") -> BYBIT
                ex.contains("COINBASE") -> COINBASE
                ex.contains("OKX") -> OKX
                ex.contains("FXCM") -> FXCM
                ex.contains("TVC") -> TVC
                ex.contains("KRAKEN") -> KRAKEN
                ex.contains("KUCOIN") -> KUCOIN
                ex.contains("BITGET") -> BITGET
                ex.contains("PEPPERSTONE") -> PEPPERSTONE
                ex.contains("SAXO") -> SAXO
                ex.contains("CAPITAL") -> CAPITALCOM
                ex.contains("NASDAQ") -> NASDAQ
                ex.contains("NYSE") -> NYSE
                ex.contains("IDX") -> IDX
                else -> OTHER
            }
        }
    }
}

data class SearchResultItem(
    val symbol: String,
    val tvSymbol: String,
    val displayName: String,
    val name: String,
    val exchange: String,
    val type: AssetType,
    val platform: ExchangePlatform
) {
    val exchangeBadge: String get() = if (exchange.isNotBlank()) exchange else platform.badgeLabel
}

data class TradingViewRating(
    val score: Double, // -1.0 to 1.0 from Recommend.All
    val action: SignalAction,
    val rsi: Double?,
    val macdSignal: Double?,
    val timestamp: Long = System.currentTimeMillis()
)

data class MarketAsset(
    val symbol: String,              // e.g. "BTCUSDT", "EURUSD=X"
    val tvSymbol: String,            // e.g. "BINANCE:BTCUSDT", "OANDA:EURUSD"
    val displayName: String,         // e.g. "BTC/USDT", "EUR/USD"
    val name: String,                // e.g. "Bitcoin", "Euro / US Dollar"
    val type: AssetType,
    val platform: ExchangePlatform,
    val currentPrice: Double,
    val change24h: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val decimals: Int = 2,
    val tvRating: TradingViewRating? = null,
    val lastSyncTime: Long = System.currentTimeMillis()
) {
    val exchangeBadge: String get() = if (tvSymbol.contains(":")) tvSymbol.substringBefore(":") else platform.badgeLabel
}

enum class Timeframe(val label: String, val interval: String, val minutes: Long) {
    M1("1m", "1m", 1),
    M5("5m", "5m", 5),
    M15("15m", "15m", 15),
    H1("1h", "1h", 60),
    H4("4h", "4h", 240),
    D1("1D", "1d", 1440)
}

data class CandleStick(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isBullish: Boolean get() = close >= open
    val bodyHeight: Double get() = kotlin.math.abs(close - open)
    val upperWick: Double get() = high - kotlin.math.max(open, close)
    val lowerWick: Double get() = kotlin.math.min(open, close) - low
}

data class BollingerBandPoint(
    val upper: Double,
    val middle: Double,
    val lower: Double
)

data class MacdPoint(
    val macd: Double,
    val signal: Double,
    val histogram: Double
)

data class StochasticPoint(
    val k: Double,
    val d: Double
)

data class LivePriceUpdate(
    val price: Double,
    val change24h: Double? = null,
    val high24h: Double? = null,
    val low24h: Double? = null
)

data class TechnicalIndicators(
    val ema9: List<Double?>,
    val ema21: List<Double?>,
    val sma20: List<Double?>,
    val sma50: List<Double?>,
    val rsi14: List<Double?>,
    val bollingerBands: List<BollingerBandPoint?>,
    val macd: List<MacdPoint?>,
    val stochastic: List<StochasticPoint?> = emptyList(),
    val atr14: List<Double?> = emptyList(),
    val currentRsi: Double?,
    val currentMacd: MacdPoint?,
    val currentBollinger: BollingerBandPoint?,
    val currentStochastic: StochasticPoint? = null,
    val currentAtr: Double? = null,
    val supportLevel1: Double,
    val supportLevel2: Double,
    val resistanceLevel1: Double,
    val resistanceLevel2: Double,
    val pivotPoint: Double,
    val detectedPatterns: List<String>
)

enum class SignalAction(val label: String) {
    STRONG_BUY("STRONG BUY"),
    BUY("BUY"),
    NEUTRAL("NEUTRAL"),
    SELL("SELL"),
    STRONG_SELL("STRONG SELL")
}

data class AiAnalysisResult(
    val action: SignalAction,
    val confidence: Int,              // 0-100%
    val trend: String,                // "BULLISH", "BEARISH", "SIDEWAYS"
    val entryZone: String,            // e.g. "$77,200 - $77,500"
    val takeProfit1: Double,
    val takeProfit2: Double,
    val stopLoss: Double,
    val riskRewardRatio: String,      // e.g. "1 : 2.5"
    val patternsDetected: List<String>,
    val rsiAnalysis: String,
    val macdAnalysis: String,
    val maAnalysis: String,
    val keySummary: String,
    val riskWarning: String,
    val analyzedAt: Long = System.currentTimeMillis(),
    val isRealAi: Boolean = true,
    val platformSource: String = "Binance Spot / OANDA Data Feed"
)

enum class AlertType {
    PRICE_ABOVE, PRICE_BELOW, RSI_OVERBOUGHT, RSI_OVERSOLD, MACD_BULLISH_CROSS, MACD_BEARISH_CROSS
}

data class PriceAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val type: AlertType,
    val targetValue: Double,
    val note: String,
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class LiveSignal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val platform: String,
    val action: SignalAction,
    val price: Double,
    val title: String,
    val message: String,
    val tvRating: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
