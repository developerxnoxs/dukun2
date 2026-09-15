package com.example.data.calculator

import com.example.data.model.BollingerBandPoint
import com.example.data.model.CandleStick
import com.example.data.model.MacdPoint
import com.example.data.model.StochasticPoint
import com.example.data.model.TechnicalIndicators
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object IndicatorCalculator {

    fun calculateAllIndicators(candles: List<CandleStick>): TechnicalIndicators {
        if (candles.isEmpty()) {
            return TechnicalIndicators(
                ema9 = emptyList(),
                ema21 = emptyList(),
                sma20 = emptyList(),
                sma50 = emptyList(),
                rsi14 = emptyList(),
                bollingerBands = emptyList(),
                macd = emptyList(),
                stochastic = emptyList(),
                atr14 = emptyList(),
                currentRsi = null,
                currentMacd = null,
                currentBollinger = null,
                currentStochastic = null,
                currentAtr = null,
                supportLevel1 = 0.0,
                supportLevel2 = 0.0,
                resistanceLevel1 = 0.0,
                resistanceLevel2 = 0.0,
                pivotPoint = 0.0,
                detectedPatterns = emptyList()
            )
        }

        val closes = candles.map { it.close }
        val ema9 = calculateEMA(closes, 9)
        val ema21 = calculateEMA(closes, 21)
        val sma20 = calculateSMA(closes, 20)
        val sma50 = calculateSMA(closes, 50)
        val rsi14 = calculateRSI(closes, 14)
        val bollinger = calculateBollingerBands(closes, 20, 2.0)
        val macdList = calculateMACD(closes, 12, 26, 9)
        val stochList = calculateStochastic(candles, 14, 3)
        val atrList = calculateATR(candles, 14)

        val lastIndex = candles.size - 1
        val currentRsi = rsi14.getOrNull(lastIndex)
        val currentMacd = macdList.getOrNull(lastIndex)
        val currentBollinger = bollinger.getOrNull(lastIndex)
        val currentStochastic = stochList.getOrNull(lastIndex)
        val currentAtr = atrList.getOrNull(lastIndex)

        // Calculate Pivot Points using recent high, low, close
        val lookback = min(30, candles.size)
        val recentCandles = candles.takeLast(lookback)
        val highest = recentCandles.maxOf { it.high }
        val lowest = recentCandles.minOf { it.low }
        val lastClose = candles.last().close

        val pivot = (highest + lowest + lastClose) / 3.0
        val r1 = (2 * pivot) - lowest
        val s1 = (2 * pivot) - highest
        val r2 = pivot + (highest - lowest)
        val s2 = pivot - (highest - lowest)

        val patterns = detectCandlePatterns(candles)

        return TechnicalIndicators(
            ema9 = ema9,
            ema21 = ema21,
            sma20 = sma20,
            sma50 = sma50,
            rsi14 = rsi14,
            bollingerBands = bollinger,
            macd = macdList,
            stochastic = stochList,
            atr14 = atrList,
            currentRsi = currentRsi,
            currentMacd = currentMacd,
            currentBollinger = currentBollinger,
            currentStochastic = currentStochastic,
            currentAtr = currentAtr,
            supportLevel1 = s1,
            supportLevel2 = s2,
            resistanceLevel1 = r1,
            resistanceLevel2 = r2,
            pivotPoint = pivot,
            detectedPatterns = patterns
        )
    }

    fun calculateSMA(data: List<Double>, period: Int): List<Double?> {
        val result = ArrayList<Double?>(data.size)
        var rollingSum = 0.0
        for (i in data.indices) {
            rollingSum += data[i]
            if (i >= period) {
                rollingSum -= data[i - period]
            }
            if (i >= period - 1) {
                result.add(rollingSum / period)
            } else {
                result.add(null)
            }
        }
        return result
    }

    fun calculateEMA(data: List<Double>, period: Int): List<Double?> {
        val result = ArrayList<Double?>(data.size)
        if (data.size < period) {
            return List(data.size) { null }
        }
        val multiplier = 2.0 / (period + 1.0)
        var initialSum = 0.0
        for (i in 0 until period) {
            initialSum += data[i]
            result.add(null)
        }
        var prevEma = initialSum / period
        result[period - 1] = prevEma

        for (i in period until data.size) {
            val currentEma = (data[i] - prevEma) * multiplier + prevEma
            result.add(currentEma)
            prevEma = currentEma
        }
        return result
    }

    fun calculateRSI(data: List<Double>, period: Int = 14): List<Double?> {
        val result = ArrayList<Double?>(data.size)
        if (data.size <= period) {
            return List(data.size) { null }
        }

        var gainSum = 0.0
        var lossSum = 0.0

        for (i in 0 until period) {
            result.add(null)
        }

        for (i in 1..period) {
            val diff = data[i] - data[i - 1]
            if (diff >= 0) gainSum += diff else lossSum += abs(diff)
        }

        var avgGain = gainSum / period
        var avgLoss = lossSum / period

        val firstRs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        val firstRsi = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + firstRs))
        result.add(firstRsi)

        for (i in (period + 1) until data.size) {
            val diff = data[i] - data[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) abs(diff) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            val rsi = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))
            result.add(rsi)
        }

        return result
    }

    fun calculateBollingerBands(
        data: List<Double>,
        period: Int = 20,
        multiplier: Double = 2.0
    ): List<BollingerBandPoint?> {
        val sma = calculateSMA(data, period)
        val result = ArrayList<BollingerBandPoint?>(data.size)

        for (i in data.indices) {
            val middle = sma[i]
            if (middle == null || i < period - 1) {
                result.add(null)
            } else {
                var sumSqDiff = 0.0
                for (j in (i - period + 1)..i) {
                    val diff = data[j] - middle
                    sumSqDiff += diff * diff
                }
                val stdDev = sqrt(sumSqDiff / period)
                result.add(
                    BollingerBandPoint(
                        upper = middle + (multiplier * stdDev),
                        middle = middle,
                        lower = middle - (multiplier * stdDev)
                    )
                )
            }
        }
        return result
    }

    fun calculateMACD(
        data: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): List<MacdPoint?> {
        val fastEma = calculateEMA(data, fastPeriod)
        val slowEma = calculateEMA(data, slowPeriod)

        val macdLine = ArrayList<Double?>()
        val validMacdValues = ArrayList<Double>()

        for (i in data.indices) {
            val fast = fastEma[i]
            val slow = slowEma[i]
            if (fast != null && slow != null) {
                val diff = fast - slow
                macdLine.add(diff)
                validMacdValues.add(diff)
            } else {
                macdLine.add(null)
            }
        }

        val signalEma = calculateEMA(validMacdValues, signalPeriod)
        val result = ArrayList<MacdPoint?>()
        var validIdx = 0

        for (i in data.indices) {
            val m = macdLine[i]
            if (m != null) {
                val s = signalEma.getOrNull(validIdx)
                if (s != null) {
                    result.add(MacdPoint(macd = m, signal = s, histogram = m - s))
                } else {
                    result.add(null)
                }
                validIdx++
            } else {
                result.add(null)
            }
        }

        return result
    }

    fun detectCandlePatterns(candles: List<CandleStick>): List<String> {
        val patterns = mutableListOf<String>()
        if (candles.size < 3) return patterns

        val c = candles.last()
        val prev = candles[candles.size - 2]
        val prev2 = candles[candles.size - 3]

        val body = c.bodyHeight
        val range = c.high - c.low
        val isDoji = range > 0 && (body / range) < 0.1

        if (isDoji) {
            if (c.lowerWick > body * 3 && c.upperWick < body) {
                patterns.add("Dragonfly Doji (Sinyal Reversal Bullish)")
            } else if (c.upperWick > body * 3 && c.lowerWick < body) {
                patterns.add("Gravestone Doji (Sinyal Reversal Bearish)")
            } else {
                patterns.add("Doji (Indikasi Keraguan Pasar / Konsolidasi)")
            }
        }

        // Hammer / Hanging Man
        if (c.lowerWick > (body * 2.0) && c.upperWick < (body * 0.5) && range > 0) {
            if (prev.close < prev.open) {
                patterns.add("Hammer (Potensi Reversal Bullish di Dasar)")
            } else {
                patterns.add("Hanging Man (Peringatan Koreksi Bearish)")
            }
        }

        // Inverted Hammer / Shooting Star
        if (c.upperWick > (body * 2.0) && c.lowerWick < (body * 0.5) && range > 0) {
            if (prev.close > prev.open) {
                patterns.add("Shooting Star (Sinyal Penolakan Harga Atas)")
            } else {
                patterns.add("Inverted Hammer (Akumulasi Pembeli di Bawah)")
            }
        }

        // Engulfing
        if (!prev.isBullish && c.isBullish && c.open <= prev.close && c.close >= prev.open) {
            patterns.add("Bullish Engulfing (Tekanan Beli Kuat Mengambil Alih)")
        } else if (prev.isBullish && !c.isBullish && c.open >= prev.close && c.close <= prev.open) {
            patterns.add("Bearish Engulfing (Tekanan Jual Kuat Mematahkan Tren)")
        }

        // Morning Star / Evening Star
        val prevBody = prev.bodyHeight
        if (!prev2.isBullish && prevBody < prev2.bodyHeight * 0.4 && c.isBullish && c.close > (prev2.open + prev2.close) / 2) {
            patterns.add("Morning Star (Formasi Konfirmasi Reversal Naik 3 Lilin)")
        } else if (prev2.isBullish && prevBody < prev2.bodyHeight * 0.4 && !c.isBullish && c.close < (prev2.open + prev2.close) / 2) {
            patterns.add("Evening Star (Formasi Konfirmasi Penurunan 3 Lilin)")
        }

        // Marubozu
        if (range > 0 && (body / range) > 0.88) {
            if (c.isBullish) patterns.add("Bullish Marubozu (Momentum Pembelian Sangat Dominan)")
            else patterns.add("Bearish Marubozu (Momentum Penjualan Sangat Dominan)")
        }

        return patterns
    }

    /**
     * Fast/Slow Stochastic Oscillator (%K, %D)
     * %K = ((Close - Lowest Low) / (Highest High - Lowest Low)) * 100
     * %D = 3-period SMA of %K
     */
    fun calculateStochastic(
        candles: List<CandleStick>,
        kPeriod: Int = 14,
        dPeriod: Int = 3
    ): List<StochasticPoint?> {
        val kValues = ArrayList<Double?>()
        for (i in candles.indices) {
            if (i < kPeriod - 1) {
                kValues.add(null)
            } else {
                val window = candles.subList(i - kPeriod + 1, i + 1)
                val highestHigh = window.maxOf { it.high }
                val lowestLow = window.minOf { it.low }
                val currentClose = candles[i].close

                val k = if (highestHigh == lowestLow) {
                    50.0
                } else {
                    ((currentClose - lowestLow) / (highestHigh - lowestLow)) * 100.0
                }
                kValues.add(k.coerceIn(0.0, 100.0))
            }
        }

        val result = ArrayList<StochasticPoint?>()
        for (i in candles.indices) {
            val k = kValues[i]
            if (k == null || i < (kPeriod - 1 + dPeriod - 1)) {
                result.add(null)
            } else {
                val dWindow = kValues.subList(i - dPeriod + 1, i + 1).filterNotNull()
                val d = if (dWindow.isNotEmpty()) dWindow.average() else k
                result.add(StochasticPoint(k = k, d = d))
            }
        }
        return result
    }

    /**
     * Average True Range (ATR 14) for volatility measurement and dynamic stop-loss/take-profit calculation
     */
    fun calculateATR(candles: List<CandleStick>, period: Int = 14): List<Double?> {
        if (candles.isEmpty()) return emptyList()

        val trList = ArrayList<Double>()
        for (i in candles.indices) {
            if (i == 0) {
                trList.add(candles[i].high - candles[i].low)
            } else {
                val high = candles[i].high
                val low = candles[i].low
                val prevClose = candles[i - 1].close
                val tr = max(high - low, max(abs(high - prevClose), abs(low - prevClose)))
                trList.add(tr)
            }
        }

        val atr = ArrayList<Double?>()
        var runningAtr = 0.0
        for (i in candles.indices) {
            if (i < period - 1) {
                atr.add(null)
            } else if (i == period - 1) {
                runningAtr = trList.take(period).average()
                atr.add(runningAtr)
            } else {
                runningAtr = ((runningAtr * (period - 1)) + trList[i]) / period
                atr.add(runningAtr)
            }
        }
        return atr
    }
}
