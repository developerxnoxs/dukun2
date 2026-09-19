package com.example.data.gemini

import com.example.BuildConfig
import com.example.data.calculator.IndicatorCalculator
import com.example.data.model.CandleStick
import com.example.data.model.SignalAction
import com.example.data.model.TechnicalIndicators
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
import kotlin.math.max
import kotlin.math.min

data class GeminiTraderDecision(
    val action: SignalAction,
    val confidence: Double,
    val shouldExecute: Boolean,
    val suggestedEntry: Double,
    val suggestedStopLoss: Double,
    val suggestedTakeProfit: Double,
    val tradeThesis: String,
    val riskWarning: String,
    val isLiveGeminiResponse: Boolean,
    val isRateLimited: Boolean = false,
    val rateLimitRemainingSeconds: Long = 0L,
    // Patokan Analisis (Trigger Anchor / Blueprint)
    val triggerMinPrice: Double = 0.0,
    val triggerMaxPrice: Double = 0.0,
    val triggerCondition: String = "IMMEDIATE",
    val triggerRsiMin: Double = 35.0,
    val triggerRsiMax: Double = 68.0,
    val planValidityMinutes: Int = 20
)

class GeminiTraderClient {

    companion object {
        @Volatile
        private var rateLimitedUntilTimestamp: Long = 0L

        fun isCurrentlyRateLimited(): Boolean {
            return System.currentTimeMillis() < rateLimitedUntilTimestamp
        }

        fun getRemainingCooldownSeconds(): Long {
            val remainingMs = rateLimitedUntilTimestamp - System.currentTimeMillis()
            return if (remainingMs > 0) (remainingMs / 1000L) else 0L
        }

        fun markRateLimited(cooldownSeconds: Long = 180L) {
            rateLimitedUntilTimestamp = System.currentTimeMillis() + (cooldownSeconds * 1000L)
        }

        fun resetRateLimit() {
            rateLimitedUntilTimestamp = 0L
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun evaluateMarketForTrading(
        symbol: String,
        currentPrice: Double,
        candles: List<CandleStick>,
        indicators: TechnicalIndicators,
        currentPositionHeld: Boolean = false,
        positionEntryPrice: Double = 0.0,
        unrealizedPnlPct: Double = 0.0,
        customApiKey: String = "",
        forceManual: Boolean = false
    ): GeminiTraderDecision = withContext(Dispatchers.IO) {
        val apiKey = if (customApiKey.isNotBlank()) customApiKey.trim() else BuildConfig.GEMINI_API_KEY
        val isKeyValid = apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY")

        // 1. Check rate limit: If manual click and cooldown is low, allow user to test API
        if (isCurrentlyRateLimited()) {
            val remainingSec = getRemainingCooldownSeconds()
            if (!forceManual || remainingSec > 15L) {
                return@withContext generateHeuristicTraderDecision(
                    symbol = symbol,
                    currentPrice = currentPrice,
                    candles = candles,
                    indicators = indicators,
                    currentPositionHeld = currentPositionHeld,
                    positionEntryPrice = positionEntryPrice,
                    unrealizedPnlPct = unrealizedPnlPct,
                    isLiveGemini = false,
                    notePrefix = "[Engine Kuantitatif - Gemini Limit 429 (${remainingSec}s)]",
                    isRateLimited = true,
                    rateLimitRemainingSeconds = remainingSec,
                    forceManual = forceManual
                )
            }
        }

        if (!isKeyValid) {
            return@withContext generateHeuristicTraderDecision(
                symbol = symbol,
                currentPrice = currentPrice,
                candles = candles,
                indicators = indicators,
                currentPositionHeld = currentPositionHeld,
                positionEntryPrice = positionEntryPrice,
                unrealizedPnlPct = unrealizedPnlPct,
                isLiveGemini = false,
                notePrefix = "[Engine Kuantitatif - Masukkan Kunci Gemini di 'Akun & API']",
                forceManual = forceManual
            )
        }

        val prompt = buildTraderPrompt(
            symbol = symbol,
            currentPrice = currentPrice,
            candles = candles,
            indicators = indicators,
            currentPositionHeld = currentPositionHeld,
            positionEntryPrice = positionEntryPrice,
            unrealizedPnlPct = unrealizedPnlPct,
            forceManual = forceManual
        )

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
                put("temperature", if (forceManual) 0.10 else 0.15)
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
                val request = Request.Builder().url(url).post(requestBody).build()

                val response = httpClient.newCall(request).execute()
                val responseString = response.body?.string()

                if (response.code == 429) {
                    markRateLimited(45L)
                    return@withContext generateHeuristicTraderDecision(
                        symbol = symbol,
                        currentPrice = currentPrice,
                        candles = candles,
                        indicators = indicators,
                        currentPositionHeld = currentPositionHeld,
                        positionEntryPrice = positionEntryPrice,
                        unrealizedPnlPct = unrealizedPnlPct,
                        isLiveGemini = false,
                        notePrefix = "[Rate Limit 429 - Fallback Algo]",
                        isRateLimited = true,
                        rateLimitRemainingSeconds = 45L,
                        forceManual = forceManual
                    )
                }

                if (response.isSuccessful && !responseString.isNullOrBlank()) {
                    val decision = parseTraderResponse(responseString, currentPrice, indicators, forceManual)
                    if (decision != null) {
                        return@withContext decision
                    }
                }
            } catch (e: Exception) {
                val is429Exception = e.message?.contains("429") == true || e.message?.contains("RESOURCE_EXHAUSTED") == true
                if (is429Exception) {
                    markRateLimited(45L)
                    return@withContext generateHeuristicTraderDecision(
                        symbol = symbol,
                        currentPrice = currentPrice,
                        candles = candles,
                        indicators = indicators,
                        currentPositionHeld = currentPositionHeld,
                        positionEntryPrice = positionEntryPrice,
                        unrealizedPnlPct = unrealizedPnlPct,
                        isLiveGemini = false,
                        notePrefix = "[Rate Limit 429 - Fallback Algo]",
                        isRateLimited = true,
                        rateLimitRemainingSeconds = 45L,
                        forceManual = forceManual
                    )
                }
            }
        }

        // All models failed or timed out: fall back to algorithmic engine
        generateHeuristicTraderDecision(
            symbol = symbol,
            currentPrice = currentPrice,
            candles = candles,
            indicators = indicators,
            currentPositionHeld = currentPositionHeld,
            positionEntryPrice = positionEntryPrice,
            unrealizedPnlPct = unrealizedPnlPct,
            isLiveGemini = false,
            notePrefix = "[Fallback Engine Kuantitatif]",
            forceManual = forceManual
        )
    }

    private fun buildTraderPrompt(
        symbol: String,
        currentPrice: Double,
        candles: List<CandleStick>,
        indicators: TechnicalIndicators,
        currentPositionHeld: Boolean,
        positionEntryPrice: Double,
        unrealizedPnlPct: Double,
        forceManual: Boolean = false
    ): String {
        val last10 = candles.takeLast(10)
        val candleSummaries = last10.joinToString("\n") { c ->
            "- O: ${c.open}, H: ${c.high}, L: ${c.low}, C: ${c.close}, Vol: ${c.volume}"
        }

        val ema9 = indicators.ema9.lastOrNull() ?: currentPrice
        val ema21 = indicators.ema21.lastOrNull() ?: currentPrice
        val sma50 = indicators.sma50.lastOrNull() ?: currentPrice
        val rsi = String.format(Locale.US, "%.1f", indicators.currentRsi ?: 50.0)
        val macd = indicators.currentMacd?.macd ?: 0.0
        val macdSignal = indicators.currentMacd?.signal ?: 0.0
        val macdHist = indicators.currentMacd?.histogram ?: 0.0
        val atr = indicators.currentAtr ?: (currentPrice * 0.015)
        val bbLower = indicators.currentBollinger?.lower ?: (currentPrice * 0.98)
        val bbUpper = indicators.currentBollinger?.upper ?: (currentPrice * 1.02)
        val stochK = indicators.currentStochastic?.k ?: 50.0
        val stochD = indicators.currentStochastic?.d ?: 50.0

        val positionContext = if (currentPositionHeld) {
            """
            STATUS POSISI SAAT INI:
            - Sedang memegang posisi LONG pada simbol $symbol
            - Harga Masuk (Entry): $positionEntryPrice
            - Harga Saat Ini: $currentPrice
            - Floating PnL: ${String.format(Locale.US, "%+.2f%%", unrealizedPnlPct)}
            Pertanyaan: Apakah saat ini saatnya TAKE PROFIT / CUT LOSS (action: SELL), atau tetap pertahankan (action: NEUTRAL)?
            """.trimIndent()
        } else {
            """
            STATUS POSISI SAAT INI:
            - Tidak ada posisi terbuka (CASH / USDT STANDBY).
            - Mencari setup entri berprobabilitas tinggi (Risk/Reward minimal 1:2.0).
            ${if (forceManual) "INSTRUKSI PENTING: Pengguna secara manual meminta eksekusi entri sekarang. Jika kondisi grafik wajar/konstruktif (tidak dalam flash crash), prioritaskan setup aksi 'BUY' dengan confidence >= 80% dan shouldExecute: true!" else "Evaluasi apakah ada momentum atau setup BUY yang terkonfirmasi."}
            """.trimIndent()
        }

        return """
            Anda adalah Gemini AI Trader, algoritma eksekusi trading profesional kuantitatif untuk aset $symbol.
            Tugas Anda adalah mengevaluasi data teknikal secara objektif dan mengambil keputusan eksekusi trading.

            $positionContext

            DATA INDIKATOR TEKNIKAL REAL-TIME:
            - Simbol: $symbol
            - Harga Terkini: $currentPrice
            - EMA 9: $ema9 | EMA 21: $ema21 | SMA 50: $sma50
            - RSI 14: $rsi
            - MACD Line: $macd | Signal: $macdSignal | Hist: $macdHist
            - Bollinger Bands: Lower=$bbLower | Upper=$bbUpper
            - Stochastic (14,3,3): %K=$stochK | %D=$stochD
            - ATR (14): $atr
            - Support 1 (S1): ${indicators.supportLevel1} | Resistance 1 (R1): ${indicators.resistanceLevel1}
            - Pivot Point: ${indicators.pivotPoint}
            - Pola Candlestick Terdeteksi: ${indicators.detectedPatterns.joinToString(", ").ifEmpty { "Netral" }}

            10 CANDLE TERAKHIR:
            $candleSummaries

            ATURAN EKSEKUSI:
            1. Jika momentum atau konfluensi teknikal positif (misal EMA mengarah naik, RSI 40-65, atau pantulan support), berikan action "BUY" dengan confidence >= 78% dan shouldExecute: true.
            2. Jika sudah ada posisi dan target tercapai atau tanda reversal, berikan action "SELL".
            3. Jika pasar saat ini belum ideal tapi ada potensi setup, berikan patokan trigger (triggerMinPrice dan triggerMaxPrice) agar bot lokal memantau tanpa perlu memanggil API Gemini terus-menerus.
            4. Tentukan Stop Loss (SL) dan Take Profit (TP) yang logis berdasarkan ATR dan level Support/Resistance.

            KEMBALIKAN HANYA JSON VALID BERIKUT TANPA TEKS LAIN ATAU MARKDOWN:
            {
              "action": "BUY" | "SELL" | "NEUTRAL",
              "confidence": 85,
              "shouldExecute": true,
              "suggestedEntry": $currentPrice,
              "suggestedStopLoss": 0.0,
              "suggestedTakeProfit": 0.0,
              "tradeThesis": "Alasan kuantitatif mengapa order harus dieksekusi atau ditahan",
              "riskWarning": "Mitigasi risiko dan anjuran money management",
              "triggerMinPrice": $currentPrice,
              "triggerMaxPrice": $currentPrice,
              "triggerCondition": "BREAKOUT_OR_PULLBACK",
              "planValidityMinutes": 20
            }
        """.trimIndent()
    }

    private fun parseTraderResponse(
        rawJson: String,
        currentPrice: Double,
        indicators: TechnicalIndicators,
        forceManual: Boolean = false
    ): GeminiTraderDecision? {
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
            val actionStr = json.optString("action", "NEUTRAL").uppercase(Locale.US)
            val action = when {
                actionStr.contains("BUY") -> SignalAction.BUY
                actionStr.contains("SELL") -> SignalAction.SELL
                else -> SignalAction.NEUTRAL
            }

            val rawConfidence = json.optInt("confidence", 75)
            val confidence = (rawConfidence / 100.0).coerceIn(0.1, 0.99)
            val shouldExecute = json.optBoolean("shouldExecute", (action != SignalAction.NEUTRAL && confidence >= 0.70) || (forceManual && action == SignalAction.BUY))

            val atr = indicators.currentAtr ?: (currentPrice * 0.015)
            val defaultSl = if (action == SignalAction.BUY) currentPrice - (1.5 * atr) else currentPrice + (1.5 * atr)
            val defaultTp = if (action == SignalAction.BUY) currentPrice + (3.0 * atr) else currentPrice - (3.0 * atr)

            val sl = json.optDouble("suggestedStopLoss", defaultSl).let { if (it > 0) it else defaultSl }
            val tp = json.optDouble("suggestedTakeProfit", defaultTp).let { if (it > 0) it else defaultTp }
            val entry = json.optDouble("suggestedEntry", currentPrice).let { if (it > 0) it else currentPrice }

            val thesis = json.optString("tradeThesis", "Gemini AI Trader: Analisis momentum dan konfluensi pasar selesai.")
            val risk = json.optString("riskWarning", "Batas risiko terkendali dengan Trailing Stop otomatis.")

            val trigMin = json.optDouble("triggerMinPrice", if (action == SignalAction.BUY) currentPrice * 0.995 else 0.0)
            val trigMax = json.optDouble("triggerMaxPrice", if (action == SignalAction.BUY) currentPrice * 1.005 else 0.0)
            val trigCond = json.optString("triggerCondition", if (shouldExecute) "IMMEDIATE" else "PRICE_RETEST_OR_BREAKOUT")
            val validityMin = json.optInt("planValidityMinutes", 20).coerceIn(5, 60)

            GeminiTraderDecision(
                action = action,
                confidence = confidence,
                shouldExecute = shouldExecute,
                suggestedEntry = entry,
                suggestedStopLoss = sl,
                suggestedTakeProfit = tp,
                tradeThesis = thesis,
                riskWarning = risk,
                isLiveGeminiResponse = true,
                triggerMinPrice = if (trigMin > 0) trigMin else currentPrice,
                triggerMaxPrice = if (trigMax > 0) trigMax else currentPrice,
                triggerCondition = trigCond,
                planValidityMinutes = validityMin
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun generateHeuristicTraderDecision(
        symbol: String,
        currentPrice: Double,
        candles: List<CandleStick>,
        indicators: TechnicalIndicators,
        currentPositionHeld: Boolean,
        positionEntryPrice: Double,
        unrealizedPnlPct: Double,
        isLiveGemini: Boolean,
        notePrefix: String,
        isRateLimited: Boolean = false,
        rateLimitRemainingSeconds: Long = 0L,
        forceManual: Boolean = false
    ): GeminiTraderDecision {
        val rsi = indicators.currentRsi ?: 50.0
        val ema9 = indicators.ema9.lastOrNull() ?: currentPrice
        val ema21 = indicators.ema21.lastOrNull() ?: currentPrice
        val sma50 = indicators.sma50.lastOrNull() ?: currentPrice
        val atr = indicators.currentAtr ?: (currentPrice * 0.015)
        val macdHist = indicators.currentMacd?.histogram ?: 0.0
        val stochK = indicators.currentStochastic?.k ?: 50.0
        val stochD = indicators.currentStochastic?.d ?: 50.0
        val s1 = indicators.supportLevel1
        val r1 = indicators.resistanceLevel1

        if (currentPositionHeld) {
            // Monitor open position
            return when {
                unrealizedPnlPct >= 3.0 -> {
                    GeminiTraderDecision(
                        action = SignalAction.SELL,
                        confidence = 0.90,
                        shouldExecute = true,
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice * 0.99,
                        suggestedTakeProfit = currentPrice,
                        tradeThesis = "$notePrefix Target profit +${String.format(Locale.US, "%.2f%%", unrealizedPnlPct)} tercapai. Amankan keuntungan ke USDT.",
                        riskWarning = "Realisasikan profit ke saldo USDT.",
                        isLiveGeminiResponse = isLiveGemini,
                        isRateLimited = isRateLimited,
                        rateLimitRemainingSeconds = rateLimitRemainingSeconds
                    )
                }
                unrealizedPnlPct <= -1.8 -> {
                    GeminiTraderDecision(
                        action = SignalAction.SELL,
                        confidence = 0.88,
                        shouldExecute = true,
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice,
                        suggestedTakeProfit = currentPrice,
                        tradeThesis = "$notePrefix Stop Loss proteksi risiko aktif (${String.format(Locale.US, "%.2f%%", unrealizedPnlPct)}). Disiplin pemotongan risiko.",
                        riskWarning = "Potong kerugian sedini mungkin.",
                        isLiveGeminiResponse = isLiveGemini,
                        isRateLimited = isRateLimited,
                        rateLimitRemainingSeconds = rateLimitRemainingSeconds
                    )
                }
                ema9 < ema21 && rsi > 68.0 -> {
                    GeminiTraderDecision(
                        action = SignalAction.SELL,
                        confidence = 0.82,
                        shouldExecute = true,
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice * 1.01,
                        suggestedTakeProfit = currentPrice * 0.98,
                        tradeThesis = "$notePrefix Death Cross EMA & RSI jenuh beli terdeteksi. Tutup posisi untuk memitigasi penurunan.",
                        riskWarning = "Peluang koreksi harga tinggi.",
                        isLiveGeminiResponse = isLiveGemini,
                        isRateLimited = isRateLimited,
                        rateLimitRemainingSeconds = rateLimitRemainingSeconds
                    )
                }
                else -> {
                    GeminiTraderDecision(
                        action = SignalAction.NEUTRAL,
                        confidence = 0.70,
                        shouldExecute = false,
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = positionEntryPrice * 0.982,
                        suggestedTakeProfit = positionEntryPrice * 1.042,
                        tradeThesis = "$notePrefix Posisi berjalan (${String.format(Locale.US, "%+.2f%%", unrealizedPnlPct)}). Trailing stop aktif mengunci potensi profit.",
                        riskWarning = "Trailing stop aktif mengunci keuntungan.",
                        isLiveGeminiResponse = isLiveGemini,
                        isRateLimited = isRateLimited,
                        rateLimitRemainingSeconds = rateLimitRemainingSeconds
                    )
                }
            }
        }

        // Check if user explicitly asked for immediate entry execution
        if (forceManual) {
            val sl = currentPrice - (1.5 * atr)
            val tp = currentPrice + (3.0 * atr)
            val reasonPrefix = if (isRateLimited) "⚡ [Eksekusi Engine Kuantitatif - Gemini Limit 429]" else "⚡ [Eksekusi Cepat Permintaan Trader]"
            return GeminiTraderDecision(
                action = SignalAction.BUY,
                confidence = 0.88,
                shouldExecute = true,
                suggestedEntry = currentPrice,
                suggestedStopLoss = sl,
                suggestedTakeProfit = tp,
                tradeThesis = "$reasonPrefix Order BUY dieksekusi berdasarkan kalkulasi indikator real-time (EMA 9: ${String.format(Locale.US, "%,.2f", ema9)}, RSI: ${String.format(Locale.US, "%.1f", rsi)}).",
                riskWarning = "Trailing Stop $${String.format(Locale.US, "%,.2f", sl)} aktif membatasi risiko maksimal.",
                isLiveGeminiResponse = false,
                isRateLimited = isRateLimited,
                rateLimitRemainingSeconds = rateLimitRemainingSeconds
            )
        }

        // Evaluate for BUY opportunity via robust technical scoring
        val isGoldenCross = ema9 >= ema21
        val isAboveTrend = currentPrice >= sma50
        val isRsiHealthy = rsi in 38.0..66.0
        val isMacdPositive = macdHist >= 0.0
        val isNearSupport = (s1 > 0 && currentPrice in (s1 * 0.99)..(s1 * 1.025))
        val isStochOversoldBounce = stochK in 18.0..55.0 && stochK >= stochD

        var bullishScore = 0
        if (isGoldenCross) bullishScore += 2
        if (isAboveTrend) bullishScore += 2
        if (isRsiHealthy) bullishScore += 2
        if (isMacdPositive) bullishScore += 1
        if (isNearSupport || isStochOversoldBounce) bullishScore += 2

        // Lower threshold to 4 for responsive automated execution
        return if (bullishScore >= 4) {
            val sl = currentPrice - (1.4 * atr)
            val tp = max(if (r1 > currentPrice) r1 else 0.0, currentPrice + (2.8 * atr))
            GeminiTraderDecision(
                action = SignalAction.BUY,
                confidence = 0.85,
                shouldExecute = true,
                suggestedEntry = currentPrice,
                suggestedStopLoss = sl,
                suggestedTakeProfit = tp,
                tradeThesis = "$notePrefix Konfluensi Bullish Terkonfirmasi (Skor $bullishScore/9): EMA 9>=21, RSI ${String.format(Locale.US, "%.1f", rsi)}, MACD positif. Eksekusi posisi BUY.",
                riskWarning = "Stop loss diatur di bawah struktur terdekat ($${String.format(Locale.US, "%,.2f", sl)}).",
                isLiveGeminiResponse = isLiveGemini,
                isRateLimited = isRateLimited,
                rateLimitRemainingSeconds = rateLimitRemainingSeconds,
                triggerMinPrice = currentPrice * 0.998,
                triggerMaxPrice = currentPrice * 1.002,
                triggerCondition = "IMMEDIATE"
            )
        } else {
            val pullbackTarget = if (s1 > 0) s1 else currentPrice * 0.992
            GeminiTraderDecision(
                action = SignalAction.NEUTRAL,
                confidence = 0.60,
                shouldExecute = false,
                suggestedEntry = currentPrice,
                suggestedStopLoss = currentPrice * 0.985,
                suggestedTakeProfit = currentPrice * 1.035,
                tradeThesis = "$notePrefix Menunggu konfirmasi konfluensi tren yang lebih kokoh (skor setup $bullishScore/9). Tidak ada eksekusi spekulatif sembarangan.",
                riskWarning = "Disiplin modal: hanya masuk ketika peluang risiko berbanding imbalan (R:R) ideal.",
                isLiveGeminiResponse = isLiveGemini,
                isRateLimited = isRateLimited,
                rateLimitRemainingSeconds = rateLimitRemainingSeconds,
                triggerMinPrice = pullbackTarget * 0.998,
                triggerMaxPrice = pullbackTarget * 1.005,
                triggerCondition = "PULLBACK_RETEST_SUPPORT",
                planValidityMinutes = 20
            )
        }
    }
}
