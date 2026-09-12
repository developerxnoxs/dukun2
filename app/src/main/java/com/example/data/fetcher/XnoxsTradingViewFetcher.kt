package com.example.data.fetcher

import android.util.Log
import com.example.data.model.CandleStick
import com.example.data.model.Timeframe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.random.Random

/**
 * Kotlin implementation of XnoxsFetcher (based on https://github.com/developerxnoxs/xnoxs-fetcher v4.0.0)
 *
 * Implements the native TradingView WebSocket protocol:
 * - wss://data.tradingview.com/socket.io/websocket
 * - Packet framing: ~m~<length>~m~<json>
 * - Session management: chart_create_session (cs_...) and quote_create_session (qs_...)
 * - Series subscription: resolve_symbol, create_series
 * - Parses timescale_update and du packets for OHLCV data
 * - Handles quote updates (qsd) for real-time prices
 */
class XnoxsTradingViewFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "XnoxsFetcher"
        private const val TV_WS_ENDPOINT = "wss://data.tradingview.com/socket.io/websocket"
        private const val TV_ORIGIN = "https://data.tradingview.com"
        private const val TV_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        private const val DEFAULT_TOKEN = "unauthorized_user_token"

        private val CANDLE_V_PATTERN = Pattern.compile(
            "\"v\"\\s*:\\s*\\[\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*,\\s*([0-9.]+)(?:\\s*,\\s*([0-9.]+))?"
        )
        private val LP_PATTERN = Pattern.compile("\"lp\"\\s*:\\s*([0-9.]+)")
        private val CH_PATTERN = Pattern.compile("\"ch\"\\s*:\\s*([-\\d.]+)")
        private val CHP_PATTERN = Pattern.compile("\"chp\"\\s*:\\s*([-\\d.]+)")
        private val VOL_PATTERN = Pattern.compile("\"volume\"\\s*:\\s*([0-9.]+)")
        private val HIGH_PATTERN = Pattern.compile("\"high_price\"\\s*:\\s*([0-9.]+)")
        private val LOW_PATTERN = Pattern.compile("\"low_price\"\\s*:\\s*([0-9.]+)")
    }

    data class QuoteData(
        val lastPrice: Double,
        val change: Double,
        val changePercent: Double,
        val volume: Double,
        val high: Double? = null,
        val low: Double? = null
    )

    data class ResolvedPlatformInfo(
        val symbol: String,
        val exchange: String,
        val listedExchange: String,
        val fullTvSymbol: String,
        val description: String,
        val type: String,
        val currency: String
    )

    data class FetchResult(
        val candles: List<CandleStick>,
        val latestQuote: QuoteData?,
        val resolvedPlatform: ResolvedPlatformInfo? = null
    )

    /**
     * Map app timeframe to TradingView interval string as used by XnoxsFetcher
     */
    fun mapTimeframeToTv(timeframe: Timeframe): String {
        return when (timeframe) {
            Timeframe.M1 -> "1"
            Timeframe.M5 -> "5"
            Timeframe.M15 -> "15"
            Timeframe.H1 -> "1H"
            Timeframe.H4 -> "4H"
            Timeframe.D1 -> "1D"
        }
    }

    private fun generateRandomSessionId(prefix: String, length: Int = 12): String {
        val allowedChars = "abcdefghijklmnopqrstuvwxyz"
        val randomStr = (1..length)
            .map { allowedChars[Random.nextInt(allowedChars.length)] }
            .joinToString("")
        return "$prefix$randomStr"
    }

    /**
     * Build pure Kotlin JSON message {"m": func, "p": [...]} and wrap in ~m~<length>~m~ framing
     */
    private fun formatTvMessage(func: String, params: List<Any>): String {
        val sb = StringBuilder()
        sb.append("{\"m\":\"").append(func).append("\",\"p\":[")
        params.forEachIndexed { index, param ->
            if (index > 0) sb.append(",")
            when (param) {
                is String -> {
                    sb.append("\"")
                    sb.append(param.replace("\\", "\\\\").replace("\"", "\\\""))
                    sb.append("\"")
                }
                is Number -> sb.append(param)
                is Boolean -> sb.append(param)
                is List<*> -> {
                    sb.append("[")
                    param.forEachIndexed { i, p ->
                        if (i > 0) sb.append(",")
                        sb.append("\"").append(p.toString().replace("\\", "\\\\").replace("\"", "\\\"")).append("\"")
                    }
                    sb.append("]")
                }
                else -> sb.append("\"").append(param.toString()).append("\"")
            }
        }
        sb.append("]}")
        val json = sb.toString()
        return "~m~${json.length}~m~$json"
    }

    /**
     * Retrieve historical OHLCV data directly from TradingView via WebSocket
     * following the exact XnoxsFetcher flow.
     */
    suspend fun getHistoricalData(
        formattedSymbol: String,
        timeframe: Timeframe,
        barsCount: Int = 100,
        timeoutSeconds: Long = 12
    ): FetchResult = withContext(Dispatchers.IO) {
        val deferredResult = CompletableDeferred<FetchResult>()
        val chartSession = generateRandomSessionId("cs_")
        val quoteSession = generateRandomSessionId("qs_")
        val interval = mapTimeframeToTv(timeframe)

        val candleMap = java.util.concurrent.ConcurrentSkipListMap<Long, CandleStick>()
        var latestQuote: QuoteData? = null
        var resolvedPlatform: ResolvedPlatformInfo? = null

        val request = Request.Builder()
            .url(TV_WS_ENDPOINT)
            .header("Origin", TV_ORIGIN)
            .header("User-Agent", TV_USER_AGENT)
            .build()

        var webSocketRef: WebSocket? = null

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("[XnoxsFetcher] TradingView WebSocket connected for $formattedSymbol")

                // 1. set_auth_token
                webSocket.send(formatTvMessage("set_auth_token", listOf(DEFAULT_TOKEN)))

                // 2. chart_create_session
                webSocket.send(formatTvMessage("chart_create_session", listOf(chartSession, "")))

                // 3. quote_create_session
                webSocket.send(formatTvMessage("quote_create_session", listOf(quoteSession)))

                // 4. quote_set_fields
                webSocket.send(
                    formatTvMessage(
                        "quote_set_fields",
                        listOf(
                            quoteSession,
                            "ch", "chp", "current_session", "description", "exchange",
                            "lp", "lp_time", "pricescale", "volume", "rchp", "rtc",
                            "high_price", "low_price", "prev_close_price"
                        )
                    )
                )

                // 5. quote_add_symbols
                webSocket.send(
                    formatTvMessage(
                        "quote_add_symbols",
                        listOf(quoteSession, formattedSymbol)
                    )
                )

                // 6. quote_fast_symbols
                webSocket.send(
                    formatTvMessage(
                        "quote_fast_symbols",
                        listOf(quoteSession, formattedSymbol)
                    )
                )

                // 7. resolve_symbol
                val resolvePayload = "={\"symbol\":\"$formattedSymbol\",\"adjustment\":\"splits\",\"session\":\"regular\"}"
                webSocket.send(
                    formatTvMessage(
                        "resolve_symbol",
                        listOf(chartSession, "symbol_1", resolvePayload)
                    )
                )

                // 8. create_series
                webSocket.send(
                    formatTvMessage(
                        "create_series",
                        listOf(chartSession, "s1", "s1", "symbol_1", interval, barsCount)
                    )
                )

                // 9. switch_timezone
                webSocket.send(
                    formatTvMessage(
                        "switch_timezone",
                        listOf(chartSession, "exchange")
                    )
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Heartbeat ping handling: TradingView uses ~h~<number>
                if (text.startsWith("~h~") || text.contains("~h~")) {
                    webSocket.send(text)
                }

                // Parse candles from timescale_update / du
                if (text.contains("timescale_update") || text.contains("\"v\":[")) {
                    val matcher = CANDLE_V_PATTERN.matcher(text)
                    while (matcher.find()) {
                        try {
                            val timeSec = matcher.group(1)?.toDoubleOrNull() ?: continue
                            val open = matcher.group(2)?.toDoubleOrNull() ?: continue
                            val high = matcher.group(3)?.toDoubleOrNull() ?: continue
                            val low = matcher.group(4)?.toDoubleOrNull() ?: continue
                            val close = matcher.group(5)?.toDoubleOrNull() ?: continue
                            val volume = matcher.group(6)?.toDoubleOrNull() ?: 0.0

                            val timeMillis = (timeSec * 1000L).toLong()
                            candleMap[timeMillis] = CandleStick(
                                timestamp = timeMillis,
                                open = open,
                                high = high,
                                low = low,
                                close = close,
                                volume = volume
                            )
                        } catch (_: Exception) {}
                    }
                }

                // Parse live quotes from qsd
                if (text.contains("qsd") || text.contains("\"lp\":")) {
                    val lpMatcher = LP_PATTERN.matcher(text)
                    if (lpMatcher.find()) {
                        val lp = lpMatcher.group(1)?.toDoubleOrNull()
                        if (lp != null) {
                            val chMatcher = CH_PATTERN.matcher(text)
                            val ch = if (chMatcher.find()) chMatcher.group(1)?.toDoubleOrNull() ?: 0.0 else 0.0

                            val chpMatcher = CHP_PATTERN.matcher(text)
                            val chp = if (chpMatcher.find()) chpMatcher.group(1)?.toDoubleOrNull() ?: 0.0 else 0.0

                            val volMatcher = VOL_PATTERN.matcher(text)
                            val vol = if (volMatcher.find()) volMatcher.group(1)?.toDoubleOrNull() ?: 0.0 else 0.0

                            val highMatcher = HIGH_PATTERN.matcher(text)
                            val high = if (highMatcher.find()) highMatcher.group(1)?.toDoubleOrNull() else latestQuote?.high

                            val lowMatcher = LOW_PATTERN.matcher(text)
                            val low = if (lowMatcher.find()) lowMatcher.group(1)?.toDoubleOrNull() else latestQuote?.low

                            latestQuote = QuoteData(
                                lastPrice = lp,
                                change = ch,
                                changePercent = chp,
                                volume = vol,
                                high = high,
                                low = low
                            )
                        }
                    }
                }

                // Parse symbol_resolved: returns real platform, exchange, description, and currency
                if (text.contains("symbol_resolved")) {
                    try {
                        val objIdx = text.indexOf("{\"name\":")
                        if (objIdx != -1) {
                            val sub = text.substring(objIdx)
                            val endIdx = sub.indexOf("}]}")
                            val jsonStr = if (endIdx != -1) sub.substring(0, endIdx + 1) else sub
                            val obj = JSONObject(jsonStr)
                            val exName = obj.optString("exchange")
                            val listedEx = obj.optString("listed_exchange", exName)
                            val symName = obj.optString("name")
                            val desc = obj.optString("description", symName)
                            val type = obj.optString("type")
                            val curr = obj.optString("currency_code")
                            resolvedPlatform = ResolvedPlatformInfo(
                                symbol = symName,
                                exchange = exName,
                                listedExchange = listedEx,
                                fullTvSymbol = if (exName.isNotBlank()) "$exName:$symName" else symName,
                                description = desc,
                                type = type,
                                currency = curr
                            )
                        }
                    } catch (_: Exception) {}
                }

                // When series is completed or we have collected bars
                if (text.contains("series_completed")) {
                    val candles = candleMap.values.toList()
                    println("[XnoxsFetcher] series_completed. Total candles: ${candles.size}")
                    if (candles.isNotEmpty()) {
                        deferredResult.complete(FetchResult(candles, latestQuote, resolvedPlatform))
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("[XnoxsFetcher] WebSocket failure: ${t.message}, response code: ${response?.code}")
                if (!deferredResult.isCompleted) {
                    if (candleMap.isNotEmpty()) {
                        deferredResult.complete(FetchResult(candleMap.values.toList(), latestQuote, resolvedPlatform))
                    } else {
                        deferredResult.completeExceptionally(
                            IOException("Koneksi TradingView gagal (code: ${response?.code}): ${t.localizedMessage ?: t.message}", t)
                        )
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!deferredResult.isCompleted) {
                    val candles = candleMap.values.toList()
                    if (candles.isNotEmpty()) {
                        deferredResult.complete(FetchResult(candles, latestQuote, resolvedPlatform))
                    }
                }
            }
        }

        webSocketRef = client.newWebSocket(request, listener)

        // Await with timeout
        val result = withTimeoutOrNull(timeoutSeconds * 1000L) {
            deferredResult.await()
        }

        try {
            webSocketRef.close(1000, "Fetch complete")
        } catch (_: Exception) {}

        if (result != null && result.candles.isNotEmpty()) {
            return@withContext result
        }

        // If deferred didn't complete but we got candles in the map
        if (candleMap.isNotEmpty()) {
            return@withContext FetchResult(candleMap.values.toList(), latestQuote, resolvedPlatform)
        }

        throw IOException("Timeout mengambil data lilin TradingView untuk $formattedSymbol ($interval)")
    }

    /**
     * Mengetahui platform/bursa penyedia dari simbol tertentu secara dinamis.
     * Mengakses TradingView Symbol Search engine untuk menemukan bursa (Binance, Bybit, OKX, Oanda, FXCM, Nasdaq, dll.).
     */
    suspend fun searchPlatformsForSymbol(query: String): List<ResolvedPlatformInfo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://symbol-search.tradingview.com/symbol_search/?text=$encodedQuery&lang=en"
        val request = Request.Builder()
            .url(url)
            .header("Origin", TV_ORIGIN)
            .header("Referer", "$TV_ORIGIN/")
            .header("User-Agent", TV_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val array = JSONArray(body)
            val list = mutableListOf<ResolvedPlatformInfo>()
            for (i in 0 until kotlin.math.min(array.length(), 25)) {
                val obj = array.getJSONObject(i)
                val rawSymbol = obj.optString("symbol")
                val exchange = obj.optString("prefix", obj.optString("exchange")).uppercase()
                val desc = obj.optString("description", rawSymbol)
                val type = obj.optString("type", "unknown")
                val currency = obj.optString("currency_code", "")
                list.add(
                    ResolvedPlatformInfo(
                        symbol = rawSymbol,
                        exchange = exchange,
                        listedExchange = obj.optString("listed_exchange", exchange),
                        fullTvSymbol = if (exchange.isNotBlank()) "$exchange:$rawSymbol" else rawSymbol,
                        description = desc,
                        type = type,
                        currency = currency
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Resolusi otomatis untuk mengetahui platform/bursa default dari suatu simbol
     * (misalnya jika pengguna hanya memasukkan "BTCUSDT" atau "EURUSD" tanpa prefiks bursa).
     */
    suspend fun resolveSymbolPlatform(symbol: String): ResolvedPlatformInfo? = withContext(Dispatchers.IO) {
        val clean = symbol.trim().uppercase()
        if (clean.contains(":")) {
            val ex = clean.substringBefore(":")
            val sym = clean.substringAfter(":")
            return@withContext ResolvedPlatformInfo(
                symbol = sym,
                exchange = ex,
                listedExchange = ex,
                fullTvSymbol = clean,
                description = sym,
                type = "market",
                currency = ""
            )
        }
        val matches = searchPlatformsForSymbol(clean)
        matches.firstOrNull { it.symbol.equals(clean, ignoreCase = true) } ?: matches.firstOrNull()
    }
}
