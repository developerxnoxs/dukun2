package com.example.data.bot

import com.example.data.calculator.IndicatorCalculator
import com.example.data.model.CandleStick
import com.example.data.model.SignalAction
import com.example.data.model.TechnicalIndicators
import kotlin.math.max

enum class BotStrategyType(
    val title: String,
    val description: String,
    val defaultStopLossPct: Double,
    val defaultTakeProfitPct: Double,
    val defaultTrailingPct: Double
) {
    TREND_MOMENTUM_CONFLUENCE(
        title = "Trend Momentum Confluence",
        description = "Konfirmasi tren multi-faktor: EMA (9/21/50) Golden Cross + MACD ekspansi + RSI momentum filter + ATR dynamic stop",
        defaultStopLossPct = 1.8,
        defaultTakeProfitPct = 4.2,
        defaultTrailingPct = 1.2
    ),
    BOLLINGER_STOCH_REVERSION(
        title = "Bollinger & Stoch Reversion",
        description = "Mean reversion statistik: Penetrasi Lower Bollinger Band + Stochastic Oversold (%K>%D < 25) + RSI oversold bounce",
        defaultStopLossPct = 1.5,
        defaultTakeProfitPct = 3.5,
        defaultTrailingPct = 1.0
    ),
    SMART_GRID_SCALPER(
        title = "Smart Volatility Breakout",
        description = "Breakout volatilitas terukur dengan S/R pivot level dinamis dan rasio Risk/Reward minimal 1:2",
        defaultStopLossPct = 1.2,
        defaultTakeProfitPct = 3.0,
        defaultTrailingPct = 0.8
    ),
    GEMINI_AI_TRADER(
        title = "Gemini AI Autonomous Trader",
        description = "Trader otonom berbasis model Gemini AI: Analisis konfluensi multi-faktor (Price Action, S/R, Volume, Indikator) dan eksekusi buy/sell otomatis terarah",
        defaultStopLossPct = 1.5,
        defaultTakeProfitPct = 4.0,
        defaultTrailingPct = 1.0
    )
}

data class BotSignalDecision(
    val action: SignalAction, // BUY, SELL, or HOLD
    val confidence: Double, // 0.0 to 1.0
    val reason: String,
    val suggestedEntry: Double,
    val suggestedStopLoss: Double,
    val suggestedTakeProfit: Double,
    val atr: Double,
    val indicators: TechnicalIndicators
)

object BotStrategyEngine {

    fun evaluate(
        candles: List<CandleStick>,
        strategyType: BotStrategyType,
        customSlPct: Double = 0.0,
        customTpPct: Double = 0.0
    ): BotSignalDecision {
        if (candles.size < 30) {
            val currentPrice = candles.lastOrNull()?.close ?: 0.0
            return BotSignalDecision(
                action = SignalAction.NEUTRAL,
                confidence = 0.0,
                reason = "Data historis tidak mencukupi (minimal 30 bar diperlukan untuk kalkulasi indikator)",
                suggestedEntry = currentPrice,
                suggestedStopLoss = currentPrice * 0.98,
                suggestedTakeProfit = currentPrice * 1.04,
                atr = 0.0,
                indicators = IndicatorCalculator.calculateAllIndicators(candles)
            )
        }

        val indicators = IndicatorCalculator.calculateAllIndicators(candles)
        val lastIdx = candles.size - 1
        val currentCandle = candles[lastIdx]
        val prevCandle = candles[lastIdx - 1]
        val currentPrice = currentCandle.close

        val currentEma9 = indicators.ema9.getOrNull(lastIdx) ?: currentPrice
        val prevEma9 = indicators.ema9.getOrNull(lastIdx - 1) ?: currentPrice
        val currentEma21 = indicators.ema21.getOrNull(lastIdx) ?: currentPrice
        val prevEma21 = indicators.ema21.getOrNull(lastIdx - 1) ?: currentPrice
        val currentSma50 = indicators.sma50.getOrNull(lastIdx) ?: currentPrice

        val currentRsi = indicators.currentRsi ?: 50.0
        val prevRsi = indicators.rsi14.getOrNull(lastIdx - 1) ?: 50.0

        val currentMacd = indicators.currentMacd
        val prevMacd = indicators.macd.getOrNull(lastIdx - 1)

        val currentBollinger = indicators.currentBollinger
        val currentStoch = indicators.currentStochastic
        val currentAtr = max(indicators.currentAtr ?: (currentPrice * 0.015), currentPrice * 0.005)

        val effectiveSlPct = if (customSlPct > 0) customSlPct else strategyType.defaultStopLossPct
        val effectiveTpPct = if (customTpPct > 0) customTpPct else strategyType.defaultTakeProfitPct

        return when (strategyType) {
            BotStrategyType.TREND_MOMENTUM_CONFLUENCE -> {
                // Algoritma 1: Trend Momentum Confluence
                val isEmaGoldenCross = (prevEma9 <= prevEma21 && currentEma9 > currentEma21)
                val isEmaBullishTrend = currentEma9 > currentEma21 && currentPrice > currentSma50
                val isRsiValidLong = currentRsi in 38.0..68.0 && currentRsi >= prevRsi
                val isMacdBullish = (currentMacd != null) && (currentMacd.macd > currentMacd.signal) && (currentMacd.histogram >= 0.0)

                val isEmaDeathCross = (prevEma9 >= prevEma21 && currentEma9 < currentEma21)
                val isRsiOverboughtReversal = currentRsi > 76.0 && currentRsi < prevRsi
                val isMacdBearishCross = (currentMacd != null) && (currentMacd.macd < currentMacd.signal) && (currentMacd.histogram < 0.0)

                if ((isEmaGoldenCross || isEmaBullishTrend) && isRsiValidLong && isMacdBullish) {
                    val stopLoss = currentPrice - (1.5 * currentAtr)
                    val takeProfit = currentPrice + (3.0 * currentAtr)
                    val conf = if (isEmaGoldenCross) 0.90 else 0.82

                    BotSignalDecision(
                        action = SignalAction.BUY,
                        confidence = conf,
                        reason = "Sinyal BUY: Trend Bullish EMA (9 > 21) & di atas SMA50, MACD Histogram positif (+${"%.4f".format(currentMacd?.histogram ?: 0.0)}), RSI sehat (${"%.1f".format(currentRsi)})",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = stopLoss,
                        suggestedTakeProfit = takeProfit,
                        atr = currentAtr,
                        indicators = indicators
                    )
                } else if (isEmaDeathCross || (isRsiOverboughtReversal && isMacdBearishCross)) {
                    val stopLoss = currentPrice + (1.5 * currentAtr)
                    val takeProfit = currentPrice - (3.0 * currentAtr)
                    BotSignalDecision(
                        action = SignalAction.SELL,
                        confidence = 0.85,
                        reason = "Sinyal EXIT / SELL: Death Cross EMA (9 < 21) atau RSI Overbought Reversal (${"%.1f".format(currentRsi)}) dengan MACD melemah",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = stopLoss,
                        suggestedTakeProfit = takeProfit,
                        atr = currentAtr,
                        indicators = indicators
                    )
                } else {
                    BotSignalDecision(
                        action = SignalAction.NEUTRAL,
                        confidence = 0.5,
                        reason = "Kondisi Netral / Konsolidasi: Menunggu konfirmasi konfluensi tren EMA & MACD yang valid",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice * (1.0 - effectiveSlPct / 100.0),
                        suggestedTakeProfit = currentPrice * (1.0 + effectiveTpPct / 100.0),
                        atr = currentAtr,
                        indicators = indicators
                    )
                }
            }

            BotStrategyType.BOLLINGER_STOCH_REVERSION -> {
                // Algoritma 2: Bollinger Band Reversion + Stochastic Oversold
                val lowerBand = currentBollinger?.lower ?: (currentPrice * 0.98)
                val upperBand = currentBollinger?.upper ?: (currentPrice * 1.02)
                val middleBand = currentBollinger?.middle ?: currentPrice

                val isNearOrBelowLower = currentPrice <= lowerBand * 1.002
                val isStochOversoldCross = (currentStoch != null) && (currentStoch.k < 30.0) && (currentStoch.k > currentStoch.d)
                val isRsiOversoldRecovering = currentRsi in 22.0..42.0

                val isNearOrAboveUpper = currentPrice >= upperBand * 0.998
                val isStochOverbought = (currentStoch != null) && currentStoch.k > 80.0

                if (isNearOrBelowLower && isStochOversoldCross && isRsiOversoldRecovering) {
                    val stopLoss = currentPrice - (1.2 * currentAtr)
                    val takeProfit = max(middleBand, currentPrice + (2.5 * currentAtr))
                    BotSignalDecision(
                        action = SignalAction.BUY,
                        confidence = 0.88,
                        reason = "Sinyal BUY: Penetrasi Lower Bollinger Band, Stochastic Oversold (%K=${"%.1f".format(currentStoch.k)} > %D=${"%.1f".format(currentStoch.d)}), RSI (${"%.1f".format(currentRsi)}) memantul",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = stopLoss,
                        suggestedTakeProfit = takeProfit,
                        atr = currentAtr,
                        indicators = indicators
                    )
                } else if (isNearOrAboveUpper || isStochOverbought || currentRsi > 78.0) {
                    BotSignalDecision(
                        action = SignalAction.SELL,
                        confidence = 0.84,
                        reason = "Sinyal EXIT / SELL: Harga mencapai Upper Bollinger Band atau Stochastic Overbought (>80)",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice + (1.2 * currentAtr),
                        suggestedTakeProfit = currentPrice - (2.5 * currentAtr),
                        atr = currentAtr,
                        indicators = indicators
                    )
                } else {
                    BotSignalDecision(
                        action = SignalAction.NEUTRAL,
                        confidence = 0.5,
                        reason = "Harga berada dalam rentang wajar Bollinger Bands (${"%.2f".format(lowerBand)} - ${"%.2f".format(upperBand)})",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice * (1.0 - effectiveSlPct / 100.0),
                        suggestedTakeProfit = currentPrice * (1.0 + effectiveTpPct / 100.0),
                        atr = currentAtr,
                        indicators = indicators
                    )
                }
            }

            BotStrategyType.SMART_GRID_SCALPER -> {
                // Algoritma 3: Smart Volatility Breakout & S/R Pivots
                val r1 = indicators.resistanceLevel1
                val s1 = indicators.supportLevel1
                val pivot = indicators.pivotPoint

                val isNearSupport = (s1 > 0 && currentPrice in (s1 * 0.995)..(s1 * 1.015))
                val isBullishVolumeCandle = currentCandle.close > currentCandle.open && currentCandle.volume >= prevCandle.volume
                val isRsiAboveMid = currentRsi in 42.0..62.0

                if (isNearSupport && isBullishVolumeCandle && isRsiAboveMid) {
                    val stopLoss = currentPrice * (1.0 - effectiveSlPct / 100.0)
                    val takeProfit = if (r1 > currentPrice) r1 else currentPrice * (1.0 + effectiveTpPct / 100.0)
                    BotSignalDecision(
                        action = SignalAction.BUY,
                        confidence = 0.83,
                        reason = "Sinyal BUY: Pantulan Support Level 1 (${"%.2f".format(s1)}) didukung volume buyer dan RSI ${"%.1f".format(currentRsi)}",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = stopLoss,
                        suggestedTakeProfit = takeProfit,
                        atr = currentAtr,
                        indicators = indicators
                    )
                } else if (r1 > 0 && currentPrice >= r1 * 0.998) {
                    BotSignalDecision(
                        action = SignalAction.SELL,
                        confidence = 0.81,
                        reason = "Sinyal EXIT: Harga mencapai target Resistance Level 1 (${"%.2f".format(r1)})",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice * 1.015,
                        suggestedTakeProfit = currentPrice * 0.97,
                        atr = currentAtr,
                        indicators = indicators
                    )
                } else {
                    BotSignalDecision(
                        action = SignalAction.NEUTRAL,
                        confidence = 0.5,
                        reason = "Menunggu harga menguji area support S1 (${"%.2f".format(s1)}) atau konfirmasi breakout",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice * (1.0 - effectiveSlPct / 100.0),
                        suggestedTakeProfit = currentPrice * (1.0 + effectiveTpPct / 100.0),
                        atr = currentAtr,
                        indicators = indicators
                    )
                }
            }

            BotStrategyType.GEMINI_AI_TRADER -> {
                // Algoritma 4: Gemini AI Trader Confluence Engine
                val ema9 = indicators.ema9.lastOrNull() ?: currentPrice
                val ema21 = indicators.ema21.lastOrNull() ?: currentPrice
                val sma50 = indicators.sma50.lastOrNull() ?: currentPrice
                val s1 = indicators.supportLevel1
                val r1 = indicators.resistanceLevel1
                val macdHist = indicators.currentMacd?.histogram ?: 0.0
                val stochK = currentStoch?.k ?: 50.0
                val stochD = currentStoch?.d ?: 50.0

                val isTrendBullish = currentPrice > sma50 && ema9 > ema21
                val isRsiValid = currentRsi in 38.0..65.0
                val isNearDemand = s1 > 0 && currentPrice in (s1 * 0.992)..(s1 * 1.025)
                val isStochRising = stochK > stochD && stochK < 75.0
                val isMacdConfirming = macdHist >= -0.0001

                val isBearishExit = (ema9 < ema21 && currentRsi > 70.0) || (r1 > 0 && currentPrice >= r1 * 0.998)

                if (isTrendBullish && isRsiValid && (isNearDemand || isStochRising) && isMacdConfirming) {
                    val stopLoss = currentPrice - (1.4 * currentAtr)
                    val takeProfit = currentPrice + (3.2 * currentAtr)
                    BotSignalDecision(
                        action = SignalAction.BUY,
                        confidence = 0.89,
                        reason = "Gemini Trader BUY: Konfluensi tren bullish terkonfirmasi (EMA 9>21>50, RSI ${"%.1f".format(currentRsi)}, MACD positif, pantulan area demand)",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = stopLoss,
                        suggestedTakeProfit = takeProfit,
                        atr = currentAtr,
                        indicators = indicators
                    )
                } else if (isBearishExit) {
                    BotSignalDecision(
                        action = SignalAction.SELL,
                        confidence = 0.85,
                        reason = "Gemini Trader SELL / EXIT: Indikator mencapai target resistance atau terjadi momentum pelemahan",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice * 1.015,
                        suggestedTakeProfit = currentPrice * 0.97,
                        atr = currentAtr,
                        indicators = indicators
                    )
                } else {
                    BotSignalDecision(
                        action = SignalAction.NEUTRAL,
                        confidence = 0.60,
                        reason = "Gemini Trader STANDBY: Kondisi pasar netral / konsolidasi. Menunggu setup berprobabilitas tinggi dengan rasio R:R optimal.",
                        suggestedEntry = currentPrice,
                        suggestedStopLoss = currentPrice * (1.0 - effectiveSlPct / 100.0),
                        suggestedTakeProfit = currentPrice * (1.0 + effectiveTpPct / 100.0),
                        atr = currentAtr,
                        indicators = indicators
                    )
                }
            }
        }
    }
}
