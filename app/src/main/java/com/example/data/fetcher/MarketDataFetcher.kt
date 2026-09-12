package com.example.data.fetcher

import com.example.data.model.AssetType
import com.example.data.model.CandleStick
import com.example.data.model.ExchangePlatform
import com.example.data.model.LivePriceUpdate
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
            // Top Crypto (Binance & Bybit via TradingView)
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
                symbol = "DOGEUSDT",
                tvSymbol = "BINANCE:DOGEUSDT",
                displayName = "DOGE/USDT",
                name = "Dogecoin",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 0.22,
                change24h = 4.15,
                high24h = 0.24,
                low24h = 0.20,
                volume24h = 28000000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "ADAUSDT",
                tvSymbol = "BINANCE:ADAUSDT",
                displayName = "ADA/USDT",
                name = "Cardano",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 0.74,
                change24h = 1.80,
                high24h = 0.77,
                low24h = 0.71,
                volume24h = 15000000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "AVAXUSDT",
                tvSymbol = "BINANCE:AVAXUSDT",
                displayName = "AVAX/USDT",
                name = "Avalanche",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 28.60,
                change24h = 2.45,
                high24h = 29.80,
                low24h = 27.50,
                volume24h = 4200000.0,
                decimals = 2
            ),
            MarketAsset(
                symbol = "SUIUSDT",
                tvSymbol = "BINANCE:SUIUSDT",
                displayName = "SUI/USDT",
                name = "Sui Network",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 3.12,
                change24h = 5.60,
                high24h = 3.35,
                low24h = 2.95,
                volume24h = 8900000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "LINKUSDT",
                tvSymbol = "BINANCE:LINKUSDT",
                displayName = "LINK/USDT",
                name = "Chainlink",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 14.85,
                change24h = 2.10,
                high24h = 15.30,
                low24h = 14.20,
                volume24h = 2100000.0,
                decimals = 2
            ),
            MarketAsset(
                symbol = "PEPEUSDT",
                tvSymbol = "BINANCE:PEPEUSDT",
                displayName = "PEPE/USDT",
                name = "Pepe Coin",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 0.0000033,
                change24h = 2.16,
                high24h = 0.0000035,
                low24h = 0.0000031,
                volume24h = 10056822.0,
                decimals = 7
            ),
            MarketAsset(
                symbol = "NEARUSDT",
                tvSymbol = "BINANCE:NEARUSDT",
                displayName = "NEAR/USDT",
                name = "NEAR Protocol",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.BINANCE_SPOT,
                currentPrice = 4.85,
                change24h = 3.20,
                high24h = 5.10,
                low24h = 4.65,
                volume24h = 3100000.0,
                decimals = 2
            ),

            // Commodities & Precious Metals (OANDA & TVC via TradingView)
            MarketAsset(
                symbol = "XAUUSD",
                tvSymbol = "OANDA:XAUUSD",
                displayName = "XAU/USD",
                name = "Spot Gold (Emas)",
                type = AssetType.COMMODITY,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 4349.4,
                change24h = 0.75,
                high24h = 4353.9,
                low24h = 4342.7,
                volume24h = 960000.0,
                decimals = 2
            ),
            MarketAsset(
                symbol = "XAGUSD",
                tvSymbol = "OANDA:XAGUSD",
                displayName = "XAG/USD",
                name = "Spot Silver (Perak)",
                type = AssetType.COMMODITY,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 32.85,
                change24h = 1.15,
                high24h = 33.20,
                low24h = 32.40,
                volume24h = 450000.0,
                decimals = 3
            ),
            MarketAsset(
                symbol = "USOIL",
                tvSymbol = "TVC:USOIL",
                displayName = "USOIL (WTI)",
                name = "Crude Oil (Minyak Mentah)",
                type = AssetType.COMMODITY,
                platform = ExchangePlatform.TVC,
                currentPrice = 71.40,
                change24h = -0.45,
                high24h = 72.30,
                low24h = 70.80,
                volume24h = 680000.0,
                decimals = 2
            ),

            // Forex Majors & Crosses (OANDA & FXCM via TradingView)
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
            ),
            MarketAsset(
                symbol = "USDCAD=X",
                tvSymbol = "OANDA:USDCAD",
                displayName = "USD/CAD",
                name = "USD / Canadian Dollar",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 1.3785,
                change24h = 0.08,
                high24h = 1.3810,
                low24h = 1.3760,
                volume24h = 94000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "USDCHF=X",
                tvSymbol = "OANDA:USDCHF",
                displayName = "USD/CHF",
                name = "USD / Swiss Franc",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 0.8845,
                change24h = -0.15,
                high24h = 0.8870,
                low24h = 0.8820,
                volume24h = 76000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "NZDUSD=X",
                tvSymbol = "OANDA:NZDUSD",
                displayName = "NZD/USD",
                name = "New Zealand / US Dollar",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 0.5890,
                change24h = 0.22,
                high24h = 0.5915,
                low24h = 0.5870,
                volume24h = 58000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "EURGBP=X",
                tvSymbol = "OANDA:EURGBP",
                displayName = "EUR/GBP",
                name = "Euro / British Pound",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 0.8575,
                change24h = -0.05,
                high24h = 0.8600,
                low24h = 0.8550,
                volume24h = 65000.0,
                decimals = 4
            ),
            MarketAsset(
                symbol = "GBPJPY=X",
                tvSymbol = "OANDA:GBPJPY",
                displayName = "GBP/JPY",
                name = "British Pound / Japanese Yen",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 207.60,
                change24h = -0.48,
                high24h = 208.50,
                low24h = 206.90,
                volume24h = 185000.0,
                decimals = 2
            ),
            MarketAsset(
                symbol = "EURJPY=X",
                tvSymbol = "OANDA:EURJPY",
                displayName = "EUR/JPY",
                name = "Euro / Japanese Yen",
                type = AssetType.FOREX,
                platform = ExchangePlatform.OANDA_TRADINGVIEW,
                currentPrice = 178.05,
                change24h = -0.72,
                high24h = 179.20,
                low24h = 177.40,
                volume24h = 162000.0,
                decimals = 2
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

    private fun mapSymbolToYahoo(symbol: String): String {
        if (symbol.contains("=")) return symbol
        return when (symbol.uppercase()) {
            "XAUUSD" -> "GC=F"
            "XAGUSD" -> "SI=F"
            "USOIL" -> "CL=F"
            else -> if (symbol.length == 6) "$symbol=X" else symbol
        }
    }

    private fun fetchForexCandlesFromYahoo(symbol: String, timeframe: Timeframe, limit: Int): List<CandleStick> {
        val mappedSymbol = mapSymbolToYahoo(symbol)
        val (interval, range) = when (timeframe) {
            Timeframe.M15 -> Pair("15m", "5d")
            Timeframe.H1 -> Pair("1h", "1mo")
            Timeframe.H4 -> Pair("1h", "3mo")
            Timeframe.D1 -> Pair("1d", "1y")
        }

        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$mappedSymbol?interval=$interval&range=$range"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Gagal mengambil data feed ($mappedSymbol: code ${response.code})")
        }

        val body = response.body?.string() ?: throw IOException("Respons kosong dari data feed")
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
            throw IOException("Data lilin untuk $symbol tidak tersedia.")
        }
        return list
    }

    suspend fun fetchTradingViewScanner(assets: List<MarketAsset>): Map<String, TradingViewRating> =
        withContext(Dispatchers.IO) {
            val ratings = mutableMapOf<String, TradingViewRating>()

            // 1. Scan Forex (OANDA, FXCM, etc.) & Commodities
            val nonCryptoAssets = assets.filter { it.type != AssetType.CRYPTO }
            if (nonCryptoAssets.isNotEmpty()) {
                try {
                    val tickers = nonCryptoAssets.map { it.tvSymbol }
                    val forexRatings = queryTvScanner(
                        endpoint = "https://scanner.tradingview.com/forex/scan",
                        tickers = tickers
                    )
                    ratings.putAll(forexRatings)
                } catch (_: Exception) {}
            }

            // 2. Scan Crypto (Binance, Bybit, Coinbase, OKX, etc.)
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

    suspend fun searchTradingViewSymbols(query: String, typeFilter: String? = null): List<com.example.data.model.SearchResultItem> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val typeParam = when (typeFilter?.uppercase()) {
                "CRYPTO" -> "&type=crypto"
                "FOREX" -> "&type=forex"
                "COMMODITY" -> "&type=commodity"
                else -> ""
            }
            val url = "https://symbol-search.tradingview.com/symbol_search/?text=$encodedQuery$typeParam&lang=en"
            val request = Request.Builder()
                .url(url)
                .header("Origin", "https://www.tradingview.com")
                .header("Referer", "https://www.tradingview.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(body)
                val results = mutableListOf<com.example.data.model.SearchResultItem>()

                for (i in 0 until kotlin.math.min(array.length(), 40)) {
                    val obj = array.getJSONObject(i)
                    val rawSymbol = obj.optString("symbol")
                    val exchange = obj.optString("exchange")
                    val prefix = obj.optString("prefix", exchange).uppercase()
                    val description = obj.optString("description", rawSymbol)
                    val typeStr = obj.optString("type").lowercase()
                    val typespecs = obj.optJSONArray("typespecs")

                    val detectedType = when {
                        typeStr.contains("crypto") || typespecs?.toString()?.contains("crypto") == true ||
                                prefix.contains("BINANCE") || prefix.contains("BYBIT") || prefix.contains("OKX") || prefix.contains("COINBASE") -> AssetType.CRYPTO
                        typeStr.contains("commodity") || rawSymbol.startsWith("XAU") || rawSymbol.startsWith("XAG") || rawSymbol.contains("OIL") || rawSymbol == "GOLD" -> AssetType.COMMODITY
                        typeStr.contains("forex") || prefix.contains("OANDA") || prefix.contains("FX") -> AssetType.FOREX
                        else -> if (rawSymbol.endsWith("USDT") || rawSymbol.endsWith("BTC")) AssetType.CRYPTO else AssetType.FOREX
                    }

                    val formattedTvSymbol = if (prefix.isNotBlank()) "$prefix:$rawSymbol" else rawSymbol
                    val platform = ExchangePlatform.fromExchange(prefix)

                    val displayName = when {
                        rawSymbol.endsWith("USDT") -> "${rawSymbol.removeSuffix("USDT")}/USDT"
                        rawSymbol.endsWith("USD") && rawSymbol.length == 6 -> "${rawSymbol.take(3)}/${rawSymbol.takeLast(3)}"
                        rawSymbol.length == 6 && detectedType == AssetType.FOREX -> "${rawSymbol.take(3)}/${rawSymbol.takeLast(3)}"
                        else -> rawSymbol
                    }

                    results.add(
                        com.example.data.model.SearchResultItem(
                            symbol = rawSymbol,
                            tvSymbol = formattedTvSymbol,
                            displayName = displayName,
                            name = description,
                            exchange = prefix.ifBlank { exchange },
                            type = detectedType,
                            platform = platform
                        )
                    )
                }
                results
            } catch (e: Exception) {
                println("[MarketDataFetcher] TradingView symbol search error: ${e.message}")
                emptyList()
            }
        }

    fun createMarketAssetFromSearch(item: com.example.data.model.SearchResultItem): MarketAsset {
        val decimals = when (item.type) {
            AssetType.CRYPTO -> if (item.symbol.contains("PEPE") || item.symbol.contains("SHIB") || item.symbol.contains("BONK")) 7 else 2
            AssetType.FOREX -> if (item.symbol.contains("JPY")) 2 else 4
            AssetType.COMMODITY -> 2
        }
        return MarketAsset(
            symbol = item.symbol,
            tvSymbol = item.tvSymbol,
            displayName = item.displayName,
            name = item.name,
            type = item.type,
            platform = item.platform,
            currentPrice = 0.0,
            change24h = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            decimals = decimals
        )
    }

    suspend fun fetchTopMarketsFromScanner(type: AssetType, limit: Int = 15): List<MarketAsset> =
        withContext(Dispatchers.IO) {
            val endpoint = if (type == AssetType.CRYPTO) {
                "https://scanner.tradingview.com/crypto/scan"
            } else {
                "https://scanner.tradingview.com/forex/scan"
            }

            val filter = if (type == AssetType.CRYPTO) {
                """[{"left":"exchange","operation":"equal","right":"BINANCE"},{"left":"name,description","operation":"match","right":"USDT"}]"""
            } else {
                """[{"left":"exchange","operation":"equal","right":"OANDA"}]"""
            }

            val payload = """{
                "filter": $filter,
                "options": {"lang":"en"},
                "symbols": {"query":{"types":[]},"tickers":[]},
                "columns": ["name","description","close","change","volume","high","low"],
                "sort": {"sortBy":"volume","sortOrder":"desc"},
                "range": [0, $limit]
            }"""

            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Content-Type", "application/json")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val data = root.optJSONArray("data") ?: return@withContext emptyList()

                val list = mutableListOf<MarketAsset>()
                for (i in 0 until data.length()) {
                    val row = data.getJSONObject(i)
                    val s = row.getString("s") // e.g. "BINANCE:BTCUSDT" or "OANDA:EURUSD"
                    val d = row.getJSONArray("d")
                    val rawName = d.optString(0)
                    val description = d.optString(1, rawName)
                    val close = d.optDouble(2, 0.0)
                    val change = d.optDouble(3, 0.0)
                    val volume = d.optDouble(4, 0.0)
                    val high = d.optDouble(5, close)
                    val low = d.optDouble(6, close)

                    val exchange = s.substringBefore(":", "")
                    val symbolPart = s.substringAfter(":", rawName)
                    val displayName = when {
                        symbolPart.endsWith("USDT") -> "${symbolPart.removeSuffix("USDT")}/USDT"
                        symbolPart.length == 6 && type == AssetType.FOREX -> "${symbolPart.take(3)}/${symbolPart.takeLast(3)}"
                        else -> symbolPart
                    }

                    val decimals = when (type) {
                        AssetType.CRYPTO -> if (close < 0.001) 7 else if (close < 1.0) 4 else 2
                        AssetType.FOREX -> if (symbolPart.contains("JPY")) 2 else 4
                        AssetType.COMMODITY -> 2
                    }

                    list.add(
                        MarketAsset(
                            symbol = symbolPart,
                            tvSymbol = s,
                            displayName = displayName,
                            name = description,
                            type = type,
                            platform = ExchangePlatform.fromExchange(exchange),
                            currentPrice = close,
                            change24h = change,
                            high24h = high,
                            low24h = low,
                            volume24h = volume,
                            decimals = decimals
                        )
                    )
                }
                list
            } catch (e: Exception) {
                println("[MarketDataFetcher] Scanner top markets error: ${e.message}")
                emptyList()
            }
        }

    suspend fun fetchLiveRealPriceDetails(asset: MarketAsset): LivePriceUpdate = withContext(Dispatchers.IO) {
        if (asset.type == AssetType.CRYPTO) {
            try {
                val cleanSymbol = if (asset.symbol.endsWith("USDT")) asset.symbol else "${asset.symbol}USDT"
                val url = "https://api.binance.com/api/v3/ticker/24hr?symbol=$cleanSymbol"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "TradingView-MarketAI-Client/2.0")
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: throw IOException("Empty ticker")
                    val json = JSONObject(body)
                    val lastPrice = json.getString("lastPrice").toDouble()
                    val priceChangePercent = json.optString("priceChangePercent", "0.0").toDouble()
                    val highPrice = json.optString("highPrice", "$lastPrice").toDouble()
                    val lowPrice = json.optString("lowPrice", "$lastPrice").toDouble()
                    return@withContext LivePriceUpdate(
                        price = lastPrice,
                        change24h = priceChangePercent,
                        high24h = highPrice,
                        low24h = lowPrice
                    )
                }
            } catch (_: Exception) {}

            // Fallback to TradingView scanner real price
            try {
                val tvPrice = fetchTvScannerPrice(asset.tvSymbol, "crypto")
                if (tvPrice != null) return@withContext tvPrice
            } catch (_: Exception) {}

            return@withContext LivePriceUpdate(price = asset.currentPrice, change24h = asset.change24h)
        } else {
            // For Forex & Commodities: query TradingView scanner first, then Yahoo Finance
            try {
                val tvPrice = fetchTvScannerPrice(asset.tvSymbol, "forex")
                if (tvPrice != null) return@withContext tvPrice
            } catch (_: Exception) {}

            try {
                val mappedSymbol = mapSymbolToYahoo(asset.symbol)
                val url = "https://query1.finance.yahoo.com/v8/finance/chart/$mappedSymbol?interval=1m&range=1d"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: throw IOException("Empty forex price")
                    val root = JSONObject(body)
                    val meta = root.getJSONObject("chart")
                        .getJSONArray("result")
                        .getJSONObject(0)
                        .getJSONObject("meta")
                    val regularPrice = meta.optDouble("regularMarketPrice", asset.currentPrice)
                    val prevClose = meta.optDouble("previousClose", regularPrice)
                    val changePercent = if (prevClose > 0) ((regularPrice - prevClose) / prevClose) * 100.0 else asset.change24h
                    return@withContext LivePriceUpdate(
                        price = regularPrice,
                        change24h = changePercent
                    )
                }
            } catch (_: Exception) {}

            return@withContext LivePriceUpdate(price = asset.currentPrice, change24h = asset.change24h)
        }
    }

    private fun fetchTvScannerPrice(tvSymbol: String, type: String): LivePriceUpdate? {
        try {
            val endpoint = if (type == "crypto") "https://scanner.tradingview.com/crypto/scan" else "https://scanner.tradingview.com/forex/scan"
            val jsonPayload = JSONObject().apply {
                put("symbols", JSONObject().apply {
                    put("tickers", JSONArray().apply { put(tvSymbol) })
                })
                put("columns", JSONArray().apply {
                    put("close")
                    put("change")
                    put("high")
                    put("low")
                })
            }
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", "Mozilla/5.0")
                .header("Content-Type", "application/json")
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val root = JSONObject(body)
                val data = root.optJSONArray("data") ?: return null
                if (data.length() > 0) {
                    val d = data.getJSONObject(0).getJSONArray("d")
                    val close = d.optDouble(0, 0.0)
                    val change = d.optDouble(1, 0.0)
                    val high = d.optDouble(2, close)
                    val low = d.optDouble(3, close)
                    if (close > 0.0) {
                        return LivePriceUpdate(price = close, change24h = change, high24h = high, low24h = low)
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    suspend fun fetchLiveRealPrice(asset: MarketAsset): Double =
        fetchLiveRealPriceDetails(asset).price
}
