package com.example.data.bot

import com.example.BuildConfig
import com.example.data.gemini.GeminiTraderClient
import com.example.data.mexc.AiCoinSelectionResult
import com.example.data.mexc.Mexc24hTicker
import com.example.data.mexc.MexcApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class CoinScreenCategory(val label: String, val description: String) {
    TOP_VOLUME("🔥 Likuiditas Tertinggi", "Volume USDT terbesar, slippage minimal, sangat aman untuk bot"),
    TOP_GAINERS("🚀 Top Gainers", "Momentum tren naik terkuat 24 jam terakhir"),
    OVERSOLD_DIP("💎 Buy The Dip (Oversold)", "Koin koreksi dalam dengan potensi pantulan teknikal tinggi"),
    HIGH_VOLATILITY("⚡ Volatilitas Tinggi", "Rentang harga lebar (spread tinggi), cocok untuk scalping cepat")
}

class MexcCoinScreener(
    private val mexcApiClient: MexcApiClient = MexcApiClient(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Fetch real 24h tickers from MEXC and categorize them for the trader
     */
    suspend fun getCategorizedCoins(): Result<Map<CoinScreenCategory, List<Mexc24hTicker>>> = withContext(Dispatchers.IO) {
        val tickersRes = mexcApiClient.get24hTickers()
        if (tickersRes.isFailure) {
            return@withContext Result.failure(tickersRes.exceptionOrNull() ?: Exception("Gagal memuat koin MEXC"))
        }

        val allTickers = tickersRes.getOrThrow()
        // Ensure only USDT pairs with substantial liquidity (> $50,000 USDT)
        val validTickers = allTickers.filter { it.symbol.endsWith("USDT") && it.quoteVolume >= 50_000.0 && it.lastPrice > 0.0 }

        val topVolume = validTickers.sortedByDescending { it.quoteVolume }.take(30)
        val topGainers = validTickers.filter { it.priceChangePercent > 0.5 && it.quoteVolume >= 100_000.0 }
            .sortedByDescending { it.priceChangePercent }.take(30)
        val oversoldDip = validTickers.filter { it.priceChangePercent < -1.0 && it.quoteVolume >= 100_000.0 }
            .sortedBy { it.priceChangePercent }.take(30)
        val highVolatility = validTickers.filter { it.quoteVolume >= 100_000.0 }
            .sortedByDescending { it.spreadPercent }.take(30)

        val map = mapOf(
            CoinScreenCategory.TOP_VOLUME to topVolume,
            CoinScreenCategory.TOP_GAINERS to topGainers,
            CoinScreenCategory.OVERSOLD_DIP to oversoldDip,
            CoinScreenCategory.HIGH_VOLATILITY to highVolatility
        )
        Result.success(map)
    }

    /**
     * Gemini AI Autonomous Coin Selector:
     * Evaluates live candidates from MEXC market data to find the single highest-accuracy,
     * highest-profit-potential cryptocurrency to trade right now.
     */
    suspend fun aiSelectBestCoinToTrade(
        candidates: List<Mexc24hTicker>,
        customApiKey: String = ""
    ): AiCoinSelectionResult = withContext(Dispatchers.IO) {
        val apiKey = if (customApiKey.isNotBlank()) customApiKey.trim() else BuildConfig.GEMINI_API_KEY
        val isKeyValid = apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY")

        if (candidates.isEmpty()) {
            return@withContext AiCoinSelectionResult(
                recommendedSymbol = "BTCUSDT",
                confidence = 0.88,
                setupCategory = "BLUE_CHIP_STABILITY",
                thesis = "Default likuiditas utama: Bitcoin (BTCUSDT) memiliki kedalaman pasar terdalam dan slippage terendah.",
                riskLevel = "LOW",
                suggestedTpPct = 3.5,
                suggestedSlPct = 1.5,
                isLiveGemini = false
            )
        }

        // Take top 12 representative coins across volume and momentum
        val topCandidates = candidates.take(12)

        if (!isKeyValid || GeminiTraderClient.isCurrentlyRateLimited()) {
            return@withContext evaluateHeuristicBestCoin(topCandidates)
        }

        try {
            val prompt = buildCoinSelectionPrompt(topCandidates)
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.15)
                    put("topP", 0.85)
                    put("responseMimeType", "application/json")
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingBudget", 0)
                    })
                })
            }

            val requestBodyString = requestJson.toString()
            val models = listOf("gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.1-flash-lite", "gemini-flash-latest")

            for (model in models) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val requestBody = requestBodyString.toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseString = response.body?.string()

                    if (response.code == 429) {
                        GeminiTraderClient.markRateLimited(45L)
                        return@withContext evaluateHeuristicBestCoin(topCandidates)
                    }

                    if (response.isSuccessful && !responseString.isNullOrBlank()) {
                        val parsed = parseGeminiCoinSelection(responseString, topCandidates)
                        if (parsed != null) {
                            return@withContext parsed
                        }
                    }
                } catch (_: Exception) {
                    // Try next model
                }
            }

            evaluateHeuristicBestCoin(topCandidates)
        } catch (e: Exception) {
            evaluateHeuristicBestCoin(topCandidates)
        }
    }

    private fun buildCoinSelectionPrompt(tickers: List<Mexc24hTicker>): String {
        val listDetails = tickers.joinToString("\n") { t ->
            "- ${t.symbol}: Harga Terkini=$${t.lastPrice} | Perubahan 24j=${String.format(Locale.US, "%+.2f%%", t.priceChangePercent)} | Volume 24j=${t.formattedVolume} | Range Spread 24j=${String.format(Locale.US, "%.2f%%", t.spreadPercent)} | High=$${t.highPrice} | Low=$${t.lowPrice}"
        }

        return """
            Anda adalah Gemini AI Chief Investment Officer & Quantitative Crypto Portfolio Manager.
            Tugas Anda adalah meneliti daftar koin pasar SPOT MEXC berikut dan memilih SATU KOIN TERBAIK yang memiliki probabilitas profit tertinggi dan rasio Risk/Reward paling menguntungkan saat ini untuk dieksekusi oleh Bot AutoTrading.
            
            KRITERIA SELEKSI:
            1. Likuiditas memadai (menghindari koin mati/manipulatif dengan volume tipis).
            2. Volatilitas konstruktif (cukup dinamis untuk mencapai TP 3-5% dalam waktu wajar, namun tidak berisiko dump ekstrem).
            3. Setup teknikal ideal: Koin dengan momentum sehat (+1.5% s/d +7% breakout berkelanjutan) ATAU koin berkualitas yang sedang diskon/retest support (-2% s/d -6% rebound).
            
            DAFTAR KOIN MEXC REAL-TIME:
            $listDetails
            
            KEMBALIKAN HANYA JSON VALID BERIKUT TANPA TEKS LAIN:
            {
              "recommendedSymbol": "SIMBOLUSDT",
              "confidence": 92,
              "setupCategory": "MOMENTUM_BREAKOUT" | "OVERSOLD_REVERSAL" | "TREND_CONTINUATION",
              "thesis": "Penjelasan kuantitatif mengapa koin ini dipilih di atas koin lainnya, potensi keuntungan, dan timing entri",
              "riskLevel": "LOW" | "MEDIUM" | "HIGH",
              "suggestedTpPct": 4.2,
              "suggestedSlPct": 1.6
            }
        """.trimIndent()
    }

    private fun parseGeminiCoinSelection(
        rawJson: String,
        fallbackCandidates: List<Mexc24hTicker>
    ): AiCoinSelectionResult? {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates")
            val content = candidates?.optJSONObject(0)?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val partObj = parts.optJSONObject(i)
                    val t = partObj?.optString("text") ?: ""
                    if (t.isNotEmpty()) textBuilder.append(t)
                }
            }
            val text = textBuilder.toString()

            val firstBrace = text.indexOf('{')
            val lastBrace = text.lastIndexOf('}')
            val jsonString = if (firstBrace != -1 && lastBrace > firstBrace) {
                text.substring(firstBrace, lastBrace + 1)
            } else {
                text.replace("```json", "").replace("```", "").trim()
            }

            val json = JSONObject(jsonString)
            val symbol = json.optString("recommendedSymbol", fallbackCandidates.first().symbol).uppercase(Locale.US)
            val conf = (json.optInt("confidence", 85) / 100.0).coerceIn(0.5, 0.98)
            val category = json.optString("setupCategory", "MOMENTUM_BREAKOUT")
            val thesis = json.optString("thesis", "Koin dipilih berdasarkan konfluensi volume likuiditas dan momentum harga ideal di bursa MEXC.")
            val risk = json.optString("riskLevel", "MEDIUM")
            val tp = json.optDouble("suggestedTpPct", 4.0).coerceIn(2.0, 10.0)
            val sl = json.optDouble("suggestedSlPct", 1.8).coerceIn(1.0, 4.0)

            AiCoinSelectionResult(
                recommendedSymbol = symbol,
                confidence = conf,
                setupCategory = category,
                thesis = thesis,
                riskLevel = risk,
                suggestedTpPct = tp,
                suggestedSlPct = sl,
                isLiveGemini = true
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun evaluateHeuristicBestCoin(tickers: List<Mexc24hTicker>): AiCoinSelectionResult {
        // Algoritma kuantitatif lokal:
        // Skor = (Volume Score * 0.45) + (Spread/Volatility Score * 0.35) + (Trend Score * 0.20)
        var bestScore = -1.0
        var bestTicker = tickers.firstOrNull() ?: Mexc24hTicker("BTCUSDT", 0.0, 0.0, 65000.0, 66000.0, 64000.0, 1000.0, 65000000.0)

        for (t in tickers) {
            // Volume factor: logarithm of quote volume
            val volScore = kotlin.math.ln(t.quoteVolume.coerceAtLeast(10000.0)) / 20.0
            // Healthy momentum: +2% to +8% is sweet spot
            val momentumScore = when {
                t.priceChangePercent in 1.5..7.5 -> 1.0
                t.priceChangePercent in -4.0..-1.0 -> 0.85 // oversold rebound candidate
                t.priceChangePercent > 7.5 -> 0.70 // extended pump
                else -> 0.50
            }
            // Volatility spread: 3% to 9% is ideal for trading
            val spreadScore = (t.spreadPercent / 8.0).coerceIn(0.3, 1.0)

            val totalScore = (volScore * 0.40) + (momentumScore * 0.40) + (spreadScore * 0.20)
            if (totalScore > bestScore) {
                bestScore = totalScore
                bestTicker = t
            }
        }

        val category = if (bestTicker.priceChangePercent >= 1.0) "MOMENTUM_BREAKOUT" else "OVERSOLD_REVERSAL"
        val tp = (bestTicker.spreadPercent * 0.5).coerceIn(3.0, 6.0)
        val sl = (tp * 0.45).coerceIn(1.2, 2.2)

        return AiCoinSelectionResult(
            recommendedSymbol = bestTicker.symbol,
            confidence = 0.87,
            setupCategory = category,
            thesis = "Analisis Kuantitatif: ${bestTicker.symbol} memiliki volume likuiditas $${bestTicker.formattedVolume} dengan pergerakan 24j (${String.format(Locale.US, "%+.2f%%", bestTicker.priceChangePercent)}) dan rentang volatilitas (${String.format(Locale.US, "%.1f%%", bestTicker.spreadPercent)}) yang sangat optimal untuk bot menghasilkan profit.",
            riskLevel = if (bestTicker.spreadPercent > 8.0) "MEDIUM" else "LOW",
            suggestedTpPct = tp,
            suggestedSlPct = sl,
            isLiveGemini = false
        )
    }
}
