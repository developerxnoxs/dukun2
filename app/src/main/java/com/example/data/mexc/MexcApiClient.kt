package com.example.data.mexc

import com.example.data.model.CandleStick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class MexcApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val BASE_URL = "https://api.mexc.com"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun hmacSha256(data: String, secretKey: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            val keySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(keySpec)
            val raw = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            return raw.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Test connectivity to MEXC REST API (GET /api/v3/ping)
     */
    suspend fun ping(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v3/ping")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("MEXC Ping HTTP error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check MEXC server time to avoid clock drift issues (GET /api/v3/time)
     */
    suspend fun getServerTime(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v3/time")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val serverTime = json.optLong("serverTime", System.currentTimeMillis())
                    Result.success(serverTime)
                } else {
                    Result.failure(IOException("Failed to get server time: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch real spot price for a symbol (GET /api/v3/ticker/price)
     */
    suspend fun getTickerPrice(symbol: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val cleanSymbol = symbol.replace("/", "").replace(":", "").uppercase()
            val request = Request.Builder()
                .url("$BASE_URL/api/v3/ticker/price?symbol=$cleanSymbol")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val price = json.optString("price", "0.0").toDoubleOrNull() ?: 0.0
                    Result.success(price)
                } else {
                    Result.failure(IOException("Failed to fetch price for $cleanSymbol: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch 24-hour ticker price change statistics for all symbols or USDT pairs on MEXC
     * GET /api/v3/ticker/24hr
     */
    suspend fun get24hTickers(): Result<List<Mexc24hTicker>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v3/ticker/24hr")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val array = JSONArray(body)
                    val resultList = mutableListOf<Mexc24hTicker>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val sym = obj.optString("symbol")
                        if (sym.endsWith("USDT")) {
                            val lastPrice = obj.optString("lastPrice", "0").toDoubleOrNull() ?: 0.0
                            val priceChange = obj.optString("priceChange", "0").toDoubleOrNull() ?: 0.0
                            val priceChangePercent = obj.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0
                            val highPrice = obj.optString("highPrice", "0").toDoubleOrNull() ?: 0.0
                            val lowPrice = obj.optString("lowPrice", "0").toDoubleOrNull() ?: 0.0
                            val volume = obj.optString("volume", "0").toDoubleOrNull() ?: 0.0
                            val quoteVolume = obj.optString("quoteVolume", "0").toDoubleOrNull() ?: 0.0

                            // Filter out inactive zero-volume pairs
                            if (quoteVolume > 10_000.0 && lastPrice > 0.0) {
                                resultList.add(
                                    Mexc24hTicker(
                                        symbol = sym,
                                        priceChange = priceChange,
                                        priceChangePercent = priceChangePercent,
                                        lastPrice = lastPrice,
                                        highPrice = highPrice,
                                        lowPrice = lowPrice,
                                        volume = volume,
                                        quoteVolume = quoteVolume
                                    )
                                )
                            }
                        }
                    }
                    // Sort primarily by highest USDT quote volume
                    resultList.sortByDescending { it.quoteVolume }
                    Result.success(resultList)
                } else {
                    Result.failure(IOException("Failed to fetch 24h tickers from MEXC: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch single 24h ticker for a specific symbol
     */
    suspend fun getSingle24hTicker(symbol: String): Result<Mexc24hTicker> = withContext(Dispatchers.IO) {
        try {
            val cleanSymbol = symbol.replace("/", "").replace(":", "").uppercase()
            val request = Request.Builder()
                .url("$BASE_URL/api/v3/ticker/24hr?symbol=$cleanSymbol")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val obj = JSONObject(body)
                    val lastPrice = obj.optString("lastPrice", "0").toDoubleOrNull() ?: 0.0
                    val priceChange = obj.optString("priceChange", "0").toDoubleOrNull() ?: 0.0
                    val priceChangePercent = obj.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0
                    val highPrice = obj.optString("highPrice", "0").toDoubleOrNull() ?: 0.0
                    val lowPrice = obj.optString("lowPrice", "0").toDoubleOrNull() ?: 0.0
                    val volume = obj.optString("volume", "0").toDoubleOrNull() ?: 0.0
                    val quoteVolume = obj.optString("quoteVolume", "0").toDoubleOrNull() ?: 0.0

                    Result.success(
                        Mexc24hTicker(
                            symbol = cleanSymbol,
                            priceChange = priceChange,
                            priceChangePercent = priceChangePercent,
                            lastPrice = lastPrice,
                            highPrice = highPrice,
                            lowPrice = lowPrice,
                            volume = volume,
                            quoteVolume = quoteVolume
                        )
                    )
                } else {
                    Result.failure(IOException("Failed to fetch 24h ticker for $cleanSymbol: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch historical Klines from MEXC API (GET /api/v3/klines)
     * Format: [openTime, open, high, low, close, volume, closeTime, quoteAssetVolume]
     */
    suspend fun getKlines(
        symbol: String,
        interval: String = "15m",
        limit: Int = 100
    ): Result<List<CandleStick>> = withContext(Dispatchers.IO) {
        try {
            val cleanSymbol = symbol.replace("/", "").replace(":", "").uppercase()
            val url = "$BASE_URL/api/v3/klines?symbol=$cleanSymbol&interval=$interval&limit=$limit"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val array = JSONArray(body)
                    val list = mutableListOf<CandleStick>()
                    for (i in 0 until array.length()) {
                        val kline = array.getJSONArray(i)
                        val openTime = kline.getLong(0)
                        val open = kline.getString(1).toDoubleOrNull() ?: 0.0
                        val high = kline.getString(2).toDoubleOrNull() ?: 0.0
                        val low = kline.getString(3).toDoubleOrNull() ?: 0.0
                        val close = kline.getString(4).toDoubleOrNull() ?: 0.0
                        val volume = kline.getString(5).toDoubleOrNull() ?: 0.0

                        list.add(
                            CandleStick(
                                timestamp = openTime,
                                open = open,
                                high = high,
                                low = low,
                                close = close,
                                volume = volume
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(IOException("Failed to fetch klines from MEXC: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Query account information & spot balances (GET /api/v3/account)
     * Uses HMAC-SHA256 signature with API Key and Secret Key
     */
    suspend fun getAccountInfo(
        apiKey: String,
        secretKey: String
    ): Result<MexcAccountInfo> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank() || secretKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("API Key & Secret Key tidak boleh kosong"))
            }

            val timestamp = System.currentTimeMillis()
            val queryString = "recvWindow=5000&timestamp=$timestamp"
            val signature = hmacSha256(queryString, secretKey)
            val fullUrl = "$BASE_URL/api/v3/account?$queryString&signature=$signature"

            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("X-MEXC-APIKEY", apiKey)
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val canTrade = json.optBoolean("canTrade", true)
                    val balancesArray = json.optJSONArray("balances") ?: JSONArray()
                    val balanceList = mutableListOf<MexcBalance>()

                    for (i in 0 until balancesArray.length()) {
                        val bObj = balancesArray.getJSONObject(i)
                        val asset = bObj.getString("asset")
                        val free = bObj.optString("free", "0").toDoubleOrNull() ?: 0.0
                        val locked = bObj.optString("locked", "0").toDoubleOrNull() ?: 0.0
                        if (free > 0.0 || locked > 0.0 || asset == "USDT") {
                            balanceList.add(MexcBalance(asset, free, locked))
                        }
                    }

                    Result.success(
                        MexcAccountInfo(
                            canTrade = canTrade,
                            updateTime = json.optLong("updateTime", timestamp),
                            balances = balanceList
                        )
                    )
                } else {
                    val errJson = runCatching { JSONObject(body) }.getOrNull()
                    val code = errJson?.optInt("code", response.code) ?: response.code
                    val msg = errJson?.optString("msg", body) ?: body
                    Result.failure(MexcApiError(code, msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send Test Order to MEXC official test endpoint (POST /api/v3/order/test)
     * Validates order parameters, exchange filters, price increments, and signature without executing real funds!
     * As documented in https://github.com/mexcdevelop/mexc-api-demo
     */
    suspend fun sendTestOrder(
        apiKey: String,
        secretKey: String,
        symbol: String,
        side: String, // BUY or SELL
        type: String = "MARKET", // MARKET or LIMIT
        quantity: Double? = null,
        quoteOrderQty: Double? = null, // USDT amount for market buy
        price: Double? = null
    ): Result<MexcOrderResponse> = withContext(Dispatchers.IO) {
        executeOrderRequest(
            isTest = true,
            apiKey = apiKey,
            secretKey = secretKey,
            symbol = symbol,
            side = side,
            type = type,
            quantity = quantity,
            quoteOrderQty = quoteOrderQty,
            price = price
        )
    }

    /**
     * Send Real Spot Order to MEXC matching engine (POST /api/v3/order)
     */
    suspend fun sendRealOrder(
        apiKey: String,
        secretKey: String,
        symbol: String,
        side: String, // BUY or SELL
        type: String = "MARKET", // MARKET or LIMIT
        quantity: Double? = null,
        quoteOrderQty: Double? = null,
        price: Double? = null
    ): Result<MexcOrderResponse> = withContext(Dispatchers.IO) {
        executeOrderRequest(
            isTest = false,
            apiKey = apiKey,
            secretKey = secretKey,
            symbol = symbol,
            side = side,
            type = type,
            quantity = quantity,
            quoteOrderQty = quoteOrderQty,
            price = price
        )
    }

    private fun executeOrderRequest(
        isTest: Boolean,
        apiKey: String,
        secretKey: String,
        symbol: String,
        side: String,
        type: String,
        quantity: Double?,
        quoteOrderQty: Double?,
        price: Double?
    ): Result<MexcOrderResponse> {
        return try {
            val cleanSymbol = symbol.replace("/", "").replace(":", "").uppercase()
            val timestamp = System.currentTimeMillis()
            val params = mutableMapOf<String, String>()

            params["symbol"] = cleanSymbol
            params["side"] = side.uppercase()
            params["type"] = type.uppercase()

            if (type.uppercase() == "LIMIT") {
                params["timeInForce"] = "GTC"
                if (price != null && price > 0) {
                    params["price"] = "%.6f".format(price).trimEnd('0').trimEnd('.')
                }
            }

            if (quoteOrderQty != null && quoteOrderQty > 0) {
                params["quoteOrderQty"] = "%.2f".format(quoteOrderQty)
            } else if (quantity != null && quantity > 0) {
                params["quantity"] = "%.6f".format(quantity).trimEnd('0').trimEnd('.')
            }

            params["recvWindow"] = "5000"
            params["timestamp"] = timestamp.toString()

            val queryString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            val signature = hmacSha256(queryString, secretKey)
            val fullQuery = "$queryString&signature=$signature"

            val endpoint = if (isTest) "/api/v3/order/test" else "/api/v3/order"
            val fullUrl = "$BASE_URL$endpoint?$fullQuery"

            val emptyBody = "".toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("X-MEXC-APIKEY", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    if (isTest) {
                        // MEXC returns {} for successful test orders
                        Result.success(
                            MexcOrderResponse(
                                symbol = cleanSymbol,
                                orderId = "TEST-${System.currentTimeMillis()}",
                                status = "TEST_VALIDATED",
                                side = side.uppercase(),
                                type = type.uppercase(),
                                origQty = quantity ?: 0.0,
                                price = price ?: 0.0,
                                isTestOrder = true
                            )
                        )
                    } else {
                        val json = JSONObject(body)
                        Result.success(
                            MexcOrderResponse(
                                symbol = json.optString("symbol", cleanSymbol),
                                orderId = json.optString("orderId", "MEXC-${System.currentTimeMillis()}"),
                                transactTime = json.optLong("transactTime", timestamp),
                                price = json.optString("price", "0").toDoubleOrNull() ?: (price ?: 0.0),
                                origQty = json.optString("origQty", "0").toDoubleOrNull() ?: (quantity ?: 0.0),
                                executedQty = json.optString("executedQty", "0").toDoubleOrNull() ?: 0.0,
                                status = json.optString("status", "FILLED"),
                                side = json.optString("side", side.uppercase()),
                                type = json.optString("type", type.uppercase()),
                                isTestOrder = false
                            )
                        )
                    }
                } else {
                    val errJson = runCatching { JSONObject(body) }.getOrNull()
                    val code = errJson?.optInt("code", response.code) ?: response.code
                    val msg = errJson?.optString("msg", body) ?: body
                    Result.failure(MexcApiError(code, msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
