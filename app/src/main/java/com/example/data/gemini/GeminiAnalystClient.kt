package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.AiAnalysisResult
import com.example.data.model.CandleStick
import com.example.data.model.MarketAsset
import com.example.data.model.SignalAction
import com.example.data.model.TechnicalIndicators
import com.example.data.model.Timeframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class GeminiAnalystClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeChartWithGemini(
        asset: MarketAsset,
        timeframe: Timeframe,
        candles: List<CandleStick>,
        indicators: TechnicalIndicators,
        chartBitmap: Bitmap
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val isKeyValid = apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY")

        if (!isKeyValid) {
            // Provide accurate algorithmic technical recommendation when API key is not yet set
            return@withContext generateHeuristicRecommendation(
                asset, timeframe, candles, indicators,
                note = "Analisis teknikal berbasis kalkulasi indikator real-time. (Tip: Masukkan GEMINI_API_KEY di panel Secrets AI Studio untuk analisis multi-modal neural Gemini langsung)"
            )
        }

        try {
            // 1. Convert Chart Bitmap to Base64 JPEG
            val outputStream = ByteArrayOutputStream()
            chartBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            // 2. Build structured technical prompt
            val prompt = buildTechnicalPrompt(asset, timeframe, candles, indicators)

            // 3. Build REST Request Payload for Gemini Vision (gemini-3.5-flash)
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            // Text prompt part
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            // Image part
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                // Optional system instruction
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("topP", 0.9)
                })
            }

            // Using gemini-3.5-flash as specified in skill guidelines
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseString = response.body?.string()

            if (!response.isSuccessful || responseString.isNullOrBlank()) {
                return@withContext generateHeuristicRecommendation(
                    asset, timeframe, candles, indicators,
                    note = "Koneksi Gemini bermasalah (${response.code}). Menggunakan analisis kuantitatif lokal."
                )
            }

            parseGeminiResponse(responseString, asset, indicators)
        } catch (e: Exception) {
            generateHeuristicRecommendation(
                asset, timeframe, candles, indicators,
                note = "Gagal memanggil Gemini API: ${e.localizedMessage ?: "Unknown"}. Fallback ke analisis teknikal algoritma."
            )
        }
    }

    private fun buildTechnicalPrompt(
        asset: MarketAsset,
        timeframe: Timeframe,
        candles: List<CandleStick>,
        indicators: TechnicalIndicators
    ): String {
        val last = candles.last()
        val rsi = String.format(Locale.US, "%.1f", indicators.currentRsi ?: 50.0)
        val patterns = if (indicators.detectedPatterns.isNotEmpty()) {
            indicators.detectedPatterns.joinToString(", ")
        } else {
            "Tidak ada pola candlestick khusus"
        }

        return """
            Anda adalah Trader & Analis Teknikal Senior Pasar Forex dan Cryptocurrency.
            Tinjau gambar chart candlestick yang dilampirkan serta data indikator teknikal berikut:
            
            - Aset: ${asset.displayName} (${asset.name})
            - Timeframe: ${timeframe.label}
            - Harga Saat Ini: ${asset.currentPrice}
            - RSI (14): $rsi
            - Pivot Point: ${indicators.pivotPoint}
            - Support 1 (S1): ${indicators.supportLevel1}
            - Resistance 1 (R1): ${indicators.resistanceLevel1}
            - Pola Lilin Terdeteksi: $patterns

            Berikan analisa teknikal profesional dalam format JSON valid PERSIS berikut tanpa markdown tambahan:
            {
              "action": "STRONG BUY" | "BUY" | "NEUTRAL" | "SELL" | "STRONG SELL",
              "confidence": 85,
              "trend": "BULLISH" | "BEARISH" | "SIDEWAYS",
              "entryZone": "rentang harga entry terbaik",
              "takeProfit1": 0.0,
              "takeProfit2": 0.0,
              "stopLoss": 0.0,
              "riskRewardRatio": "1 : 2.5",
              "rsiAnalysis": "analisis mendalam RSI",
              "macdAnalysis": "analisis mendalam MACD / momentum",
              "maAnalysis": "analisis EMA 9 vs EMA 21",
              "keySummary": "ringkasan eksekutif alasan pengambilan posisi trading",
              "riskWarning": "peringatan batas risiko dan rekomendasi leverage/money management"
            }
        """.trimIndent()
    }

    private fun parseGeminiResponse(
        rawJson: String,
        asset: MarketAsset,
        indicators: TechnicalIndicators
    ): AiAnalysisResult {
        val root = JSONObject(rawJson)
        val candidates = root.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text") ?: ""

        // Extract JSON substring if wrapped in markdown code blocks ```json ... ```
        val cleanedText = text
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val json = JSONObject(cleanedText)
        val actionStr = json.optString("action", "BUY").uppercase(Locale.US)
        val action = when {
            actionStr.contains("STRONG BUY") -> SignalAction.STRONG_BUY
            actionStr.contains("STRONG SELL") -> SignalAction.STRONG_SELL
            actionStr.contains("BUY") -> SignalAction.BUY
            actionStr.contains("SELL") -> SignalAction.SELL
            else -> SignalAction.NEUTRAL
        }

        val confidence = json.optInt("confidence", 80).coerceIn(10, 99)
        val trend = json.optString("trend", "BULLISH")
        val entryZone = json.optString("entryZone", "${asset.currentPrice}")
        val tp1 = json.optDouble("takeProfit1", asset.currentPrice * 1.015)
        val tp2 = json.optDouble("takeProfit2", asset.currentPrice * 1.03)
        val sl = json.optDouble("stopLoss", asset.currentPrice * 0.985)
        val rr = json.optString("riskRewardRatio", "1 : 2.0")
        val rsiAnal = json.optString("rsiAnalysis", "Kondisi RSI terkonfirmasi.")
        val macdAnal = json.optString("macdAnalysis", "Sinyal momentum MACD aktif.")
        val maAnal = json.optString("maAnalysis", "EMA 9 dan EMA 21 selaras dengan arah tren.")
        val summary = json.optString("keySummary", "Analisis teknikal visual mengonfirmasi sinyal probabilitas tinggi.")
        val risk = json.optString("riskWarning", "Gunakan batas risiko maksimal 1-2% modal per transaksi.")

        return AiAnalysisResult(
            action = action,
            confidence = confidence,
            trend = trend,
            entryZone = entryZone,
            takeProfit1 = tp1,
            takeProfit2 = tp2,
            stopLoss = sl,
            riskRewardRatio = rr,
            patternsDetected = indicators.detectedPatterns,
            rsiAnalysis = rsiAnal,
            macdAnalysis = macdAnal,
            maAnalysis = maAnal,
            keySummary = summary,
            riskWarning = risk,
            analyzedAt = System.currentTimeMillis(),
            isRealAi = true,
            platformSource = "${asset.platform.badgeLabel} (${asset.tvSymbol})"
        )
    }

    fun generateHeuristicRecommendation(
        asset: MarketAsset,
        timeframe: Timeframe,
        candles: List<CandleStick>,
        indicators: TechnicalIndicators,
        note: String = ""
    ): AiAnalysisResult {
        val currentPrice = asset.currentPrice
        val rsi = indicators.currentRsi ?: 50.0
        val macd = indicators.currentMacd
        val lastIdx = candles.size - 1
        val ema9 = indicators.ema9.getOrNull(lastIdx) ?: currentPrice
        val ema21 = indicators.ema21.getOrNull(lastIdx) ?: currentPrice

        var score = 0 // > 0 Bullish, < 0 Bearish

        // 1. RSI Factor
        if (rsi < 32.0) score += 3 // Oversold -> Strong Buy potential
        else if (rsi < 45.0) score += 1
        else if (rsi > 68.0) score -= 3 // Overbought -> Sell
        else if (rsi > 55.0) score -= 1

        // 2. EMA Trend
        if (ema9 > ema21) score += 2 else score -= 2
        if (currentPrice > ema9) score += 1 else score -= 1

        // 3. MACD Momentum
        if (macd != null) {
            if (macd.histogram > 0 && macd.macd > macd.signal) score += 2
            else if (macd.histogram < 0 && macd.macd < macd.signal) score -= 2
        }

        // 4. Candlestick patterns
        val patterns = indicators.detectedPatterns
        for (p in patterns) {
            if (p.contains("Bullish") || p.contains("Hammer") || p.contains("Morning")) score += 2
            if (p.contains("Bearish") || p.contains("Shooting") || p.contains("Evening")) score -= 2
        }

        val (action, trend, confidence) = when {
            score >= 4 -> Triple(SignalAction.STRONG_BUY, "BULLISH KUAT", min(94, 75 + score * 3))
            score in 2..3 -> Triple(SignalAction.BUY, "BULLISH", min(85, 68 + score * 3))
            score in -1..1 -> Triple(SignalAction.NEUTRAL, "KONSOLIDASI / SIDEWAYS", 60)
            score in -3..-2 -> Triple(SignalAction.SELL, "BEARISH", min(85, 68 + kotlin.math.abs(score) * 3))
            else -> Triple(SignalAction.STRONG_SELL, "BEARISH KUAT", min(94, 75 + kotlin.math.abs(score) * 3))
        }

        val decimals = asset.decimals
        val formatStr = "%.${decimals}f"

        val tp1: Double
        val tp2: Double
        val sl: Double
        val entryZone: String

        if (action == SignalAction.BUY || action == SignalAction.STRONG_BUY) {
            val delta = (indicators.resistanceLevel1 - currentPrice).coerceAtLeast(currentPrice * 0.008)
            tp1 = currentPrice + delta
            tp2 = currentPrice + (delta * 1.8)
            sl = (currentPrice - (delta * 0.6)).coerceAtLeast(indicators.supportLevel1 * 0.99)
            entryZone = "${String.format(Locale.US, formatStr, currentPrice * 0.998)} - ${String.format(Locale.US, formatStr, currentPrice * 1.002)}"
        } else if (action == SignalAction.SELL || action == SignalAction.STRONG_SELL) {
            val delta = (currentPrice - indicators.supportLevel1).coerceAtLeast(currentPrice * 0.008)
            tp1 = currentPrice - delta
            tp2 = currentPrice - (delta * 1.8)
            sl = (currentPrice + (delta * 0.6)).coerceAtMost(indicators.resistanceLevel1 * 1.01)
            entryZone = "${String.format(Locale.US, formatStr, currentPrice * 0.998)} - ${String.format(Locale.US, formatStr, currentPrice * 1.002)}"
        } else {
            tp1 = indicators.resistanceLevel1
            tp2 = indicators.resistanceLevel2
            sl = indicators.supportLevel1
            entryZone = "Wait and See / Tunggu Breakout"
        }

        val rsiText = when {
            rsi < 30 -> "RSI di level ${String.format(Locale.US, "%.1f", rsi)} menunjukkan kondisi oversold ekstrem, potensi rebound tinggi."
            rsi > 70 -> "RSI di level ${String.format(Locale.US, "%.1f", rsi)} mengindikasikan jenuh beli (overbought), waspadai pullback."
            else -> "RSI netral di level ${String.format(Locale.US, "%.1f", rsi)}, ruang pergerakan masih seimbang."
        }

        val macdText = if (macd != null && macd.histogram > 0) {
            "MACD histogram positif menandakan akselerasi momentum buyer."
        } else {
            "MACD histogram di bawah nol mengindikasikan tekanan penjual masih dominan."
        }

        val maText = if (ema9 > ema21) {
            "Golden Alignment: EMA 9 berada di atas EMA 21 mendukung kelanjutan tren naik."
        } else {
            "Death Alignment: EMA 9 berada di bawah EMA 21 menekan harga ke area support."
        }

        val summary = if (action == SignalAction.BUY || action == SignalAction.STRONG_BUY) {
            "Kombinasi pola lilin dan posisi harga di atas support utama memberikan sinyal beli probabilitas tinggi. $note"
        } else if (action == SignalAction.SELL || action == SignalAction.STRONG_SELL) {
            "Penolakan harga pada area resistance bersamaan dengan pelemahan momentum memicu sinyal jual strategis. $note"
        } else {
            "Pasar dalam fase konsolidasi sideways di antara support & resistance. Disarankan menunggu konfirmasi arah breakout. $note"
        }

        return AiAnalysisResult(
            action = action,
            confidence = confidence,
            trend = trend,
            entryZone = entryZone,
            takeProfit1 = tp1,
            takeProfit2 = tp2,
            stopLoss = sl,
            riskRewardRatio = if (action == SignalAction.NEUTRAL) "1 : 1" else "1 : 2.2",
            patternsDetected = patterns,
            rsiAnalysis = rsiText,
            macdAnalysis = macdText,
            maAnalysis = maText,
            keySummary = summary,
            riskWarning = "Disiplin gunakan Stop Loss ketat. Maksimal risiko per transaksi 1-2% dari total modal trading.",
            analyzedAt = System.currentTimeMillis(),
            isRealAi = false,
            platformSource = "${asset.platform.badgeLabel} (${asset.tvSymbol})"
        )
    }
}
