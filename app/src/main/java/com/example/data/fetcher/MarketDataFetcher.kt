package com.example.data.fetcher

import com.example.data.model.AssetType
import com.example.data.model.CandleStick
import com.example.data.model.ExchangePlatform
import com.example.data.model.MarketAsset
import com.example.data.model.SignalAction
import com.example.data.model.Timeframe
import com.example.data.model.TradingViewRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MarketDataFetcher {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val xnoxsFetcher = XnoxsTradingViewFetcher(httpClient)

    companion object {
        val DEFAULT_ASSETS = listOf(
            // Crypto (Binance Spot Feed via TradingView)
            MarketAsset(
                symbol = "BTCUSDT",
                tvSymbol = "BINANCE:BTCUSDT",
                displayName = "BTC/USDT",
                name = "Bitcoin",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 77150.0,
                change24h = 0.92,
                high24h = 79890.0,
                low24h = 76040.0,
                volume24h = 18000.0,
                decimals = 2
            ),
            MarketAsset(
                symbol = "ETHUSDT",
                tvSymbol = "BINANCE:ETHUSDT",
                displayName = "ETH/USDT",
                name = "Ethereum",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 2530.0,
                change24h = 3.80,
                high24h = 2665.0,
                low24h = 2433.0,
                volume24h = 611000.0,
                decimals = 2
            ),
            MarketAsset(
                symbol = "SOLUSDT",
                tvSymbol = "BINANCE:SOLUSDT",
                displayName = "SOL/USDT",
                name = "Solana",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 102.5,
                change24h = 3.86,
                high24h = 105.8,
                low24h = 98.0,
                volume24h = 3044000.0,
                decimals = 2
            ),
            MarketAsset(
                symbol = "BNBUSDT",
                tvSymbol = "BINANCE:BNBUSDT",
                displayName = "BNB/USDT",
                name = "BNB",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 628.5,
                change24h = 1.25,
                high24h = 638.0,
                low24h = 615.0,
                volume24h = 320000.0,
                decimals = 2
            ),
            MarketAsset(
                symbol = "XRPUSDT",
                tvSymbol = "BINANCE:XRPUSDT",
                displayName = "XRP/USDT",
                name = "Ripple",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 1.85,
                change24h = 2.10,
                high24h = 1.92,
                low24h = 1.78,
                volume24h = 45000000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "XAUUSD",
                tvSymbol = "OANDA:XAUUSD",
                displayName = "XAU/USD",
                name = "Spot Gold",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 4349.4,
                change24h = 0.75,
                high24h = 4353.9,
                low24h = 4342.7,
                volume24h = 960000.0,
                decimals = 2
            ),

            // Forex (OANDA Institutional via TradingView)
            MarketAsset(
                symbol = "EURUSD=X",
                tvSymbol = "OANDA:EURUSD",
                displayName = "EUR/USD",
                name = "Euro / US Dollar",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 1.1599,
                change24h = -0.09,
                high24h = 1.1618,
                low24h = 1.1569,
                volume24h = 126000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "GBPUSD=X",
                tvSymbol = "OANDA:GBPUSD",
                displayName = "GBP/USD",
                name = "British Pound / USD",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 1.3526,
                change24h = 0.11,
                high24h = 1.3535,
                low24h = 1.3481,
                volume24h = 232000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "USDJPY=X",
                tvSymbol = "OANDA:USDJPY",
                displayName = "USD/JPY",
                name = "USD / Japanese Yen",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 153.53,
                change24h = -0.60,
                high24h = 154.62,
                low24h = 153.24,
                volume24h = 401000.0,
                decimals = 2
            ),
            MarketAsset(
                symbol = "AUDUSD=X",
                tvSymbol = "OANDA:AUDUSD",
                displayName = "AUD/USD",
                name = "Australian / US Dollar",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 0.7170,
                change24h = 0.19,
                high24h = 0.7188,
                low24h = 0.7150,
                volume24h = 83000.0,
                decimals = 4
            )
        )
    }

    suspend fun fetchCandles(asset: MarketAsset, timeframe: Timeframe, limit: Int = 90): List<CandleStick> =
        withContext(Dispatchers.IO) {
            // 1. PRIMARY: Fetch genuine data from TradingView WebSocket via XnoxsFetcher protocol
            try {
                val tvResult = xnoxsFetcher.getHistoricalData(
                    formattedSymbol = asset.tvSymbol,
                    timeframe = timeframe,
                    barsCount = limit,
                    timeoutSeconds = 10
                )
                if (tvResult.candles.isNotEmpty()) {
                    return@withContext tvResult.candles
                }
            } catch (e: Exception) {
                println("[MarketDataFetcher] TradingView WS timeout for ${asset.tvSymbol}: ${e.message}. Using backup REST.")
            }

            // 2. BACKUP: Direct fallback to exchange REST API if WebSocket has connection issue
            if (asset.type == AssetType.CRYPTO) {
                fetchCryptoCandlesFromBinance(asset.symbol, timeframe, limit)
            } else {
                fetchForexCandlesFromYahoo(asset.symbol, timeframe, limit)
            }
        }

    private fun fetchCryptoCandlesFromBinance(symbol: String, timeframe: Timeframe, limit: Int): List<CandleStick> {
        val binanceInterval = when (timeframe) {
            Timeframe.M15 -> "15m"
            Timeframe.H1 -> "1h"
            Timeframe.H4 -> "4h"
            Timeframe.D1 -> "1d"
        }
        val url = "https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$binanceInterval&limit=$limit"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TradingView-MarketAI-Client/2.0")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Gagal mengambil data dari Binance API (${response.code}: ${response.message})")
        }

        val body = response.body?.string() ?: throw IOException("Respons kosong dari Binance")
        val jsonArray = JSONArray(body)

        val list = mutableListOf<CandleStick>()
        for (i in 0 until jsonArray.length()) {
            val candleArray = jsonArray.getJSONArray(i)
            val time = candleArray.getLong(0)
            val open = candleArray.getString(1).toDouble()
            val high = candleArray.getString(2).toDouble()
            val low = candleArray.getString(3).toDouble()
            val close = candleArray.getString(4).toDouble()
            val volume = candleArray.getString(5).toDouble()

            list.add(CandleStick(time, open, high, low, close, volume))
        }

        if (list.isEmpty()) {
            throw IOException("Data candle Binance untuk $symbol kosong.")
        }
        return list
    }

    private fun fetchForexCandlesFromYahoo(symbol: String, timeframe: Timeframe, limit: Int): List<CandleStick> {
        val (interval, range) = when (timeframe) {
            Timeframe.M15 -> Pair("15m", "5d")
            Timeframe.H1 -> Pair("1h", "1mo")
            Timeframe.H4 -> Pair("1h", "3mo")
            Timeframe.D1 -> Pair("1d", "1y")
        }

        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=$interval&range=$range"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Gagal mengambil data Forex (${response.code}: ${response.message})")
        }

        val body = response.body?.string() ?: throw IOException("Respons kosong dari feed Forex")
        val jsonRoot = JSONObject(body)
        val chart = jsonRoot.getJSONObject("chart")
        val results = chart.getJSONArray("result")
        val result = results.getJSONObject(0)
        val timestamps = result.getJSONArray("timestamp")
        val indicators = result.getJSONObject("indicators")
        val quote = indicators.getJSONArray("quote").getJSONObject(0)

        val opens = quote.getJSONArray("open")
        val highs = quote.getJSONArray("high")
        val lows = quote.getJSONArray("low")
        val closes = quote.getJSONArray("close")
        val volumes = quote.optJSONArray("volume")

        val list = mutableListOf<CandleStick>()
        val count = timestamps.length()
        val startIdx = kotlin.math.max(0, count - limit)

        for (i in startIdx until count) {
            if (closes.isNull(i) || opens.isNull(i)) continue
            val time = timestamps.getLong(i) * 1000L
            val open = opens.optDouble(i)
            val high = highs.optDouble(i)
            val low = lows.optDouble(i)
            val close = closes.optDouble(i)
            val volume = if (volumes != null && !volumes.isNull(i)) volumes.optDouble(i) else 1000.0

            if (!open.isNaN() && !high.isNaN() && !low.isNaN() && !close.isNaN()) {
                list.add(CandleStick(time, open, high, low, close, volume))
            }
        }

        if (list.isEmpty()) {
            throw IOException("Data lilin Forex dari OANDA/Yahoo untuk $symbol tidak tersedia.")
        }
        return list
    }

    suspend fun fetchTradingViewScanner(assets: List<MarketAsset>): Map<String, TradingViewRating> =
        withContext(Dispatchers.IO) {
            val ratings = mutableMapOf<String, TradingViewRating>()

            // 1. Scan Forex (OANDA)
            val forexAssets = assets.filter { it.type == AssetType.FOREX }
            if (forexAssets.isNotEmpty()) {
                try {
                    val forexTickers = forexAssets.map { it.tvSymbol }
                    val forexRatings = queryTvScanner(
                        endpoint = "https://scanner.tradingview.com/forex/scan",
                        tickers = forexTickers
                    )
                    ratings.putAll(forexRatings)
                } catch (_: Exception) {}
            }

            // 2. Scan Crypto (Binance)
            val cryptoAssets = assets.filter { it.type == AssetType.CRYPTO }
            if (cryptoAssets.isNotEmpty()) {
                try {
                    val cryptoTickers = cryptoAssets.map { it.tvSymbol }
                    val cryptoRatings = queryTvScanner(
                        endpoint = "https://scanner.tradingview.com/crypto/scan",
                        tickers = cryptoTickers
                    )
                    ratings.putAll(cryptoRatings)
                } catch (_: Exception) {}
            }

            ratings
        }

    private fun queryTvScanner(endpoint: String, tickers: List<String>): Map<String, TradingViewRating> {
        val jsonPayload = JSONObject().apply {
            put("symbols", JSONObject().apply {
                put("tickers", JSONArray(tickers))
            })
            put("columns", JSONArray().apply {
                put("name")
                put("close")
                put("change")
                put("high")
                put("low")
                put("volume")
                put("Recommend.All")
                put("RSI")
                put("MACD.macd")
                put("MACD.signal")
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .header("User-Agent", "Mozilla/5.0")
            .header("Content-Type", "application/json")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyMap()

        val body = response.body?.string() ?: return emptyMap()
        val root = JSONObject(body)
        val data = root.optJSONArray("data") ?: return emptyMap()

        val map = mutableMapOf<String, TradingViewRating>()
        for (i in 0 until data.length()) {
            val row = data.getJSONObject(i)
            val ticker = row.getString("s")
            val d = row.getJSONArray("d")

            val recommendScore = d.optDouble(6, 0.0)
            val rsiVal = if (d.isNull(7)) null else d.optDouble(7)
            val macdSig = if (d.isNull(9)) null else d.optDouble(9)

            val action = when {
                recommendScore >= 0.5 -> SignalAction.STRONG_BUY
                recommendScore >= 0.1 -> SignalAction.BUY
                recommendScore <= -0.5 -> SignalAction.STRONG_SELL
                recommendScore <= -0.1 -> SignalAction.SELL
                else -> SignalAction.NEUTRAL
            }

            map[ticker] = TradingViewRating(
                score = recommendScore,
                action = action,
                rsi = rsiVal,
                macdSignal = macdSig
            )
        }
        return map
    }

    suspend fun fetchLiveRealPrice(asset: MarketAsset): Double = withContext(Dispatchers.IO) {
        if (asset.type == AssetType.CRYPTO) {
            val url = "https://api.binance.com/api/v3/ticker/price?symbol=${asset.symbol}"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TradingView-MarketAI-Client/2.0")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: throw IOException("Empty price")
                val json = JSONObject(body)
                return@withContext json.getString("price").toDouble()
            }
            throw IOException("Binance price error: ${response.code}")
        } else {
            // For forex query live OANDA ticker from TradingView scanner
            val ratings = queryTvScanner(
                endpoint = "https://scanner.tradingview.com/forex/scan",
                tickers = listOf(asset.tvSymbol)
            )
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/${asset.symbol}?interval=1m&range=1d"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: throw IOException("Empty forex price")
                val root = JSONObject(body)
                val regularPrice = root.getJSONObject("chart")
                    .getJSONArray("result")
                    .getJSONObject(0)
                    .getJSONObject("meta")
                    .optDouble("regularMarketPrice", asset.currentPrice)
                return@withContext regularPrice
            }
            throw IOException("Forex price error: ${response.code}")
        }
    }
}
