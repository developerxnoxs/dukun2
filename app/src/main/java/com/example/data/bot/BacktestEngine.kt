package com.example.data.bot

import com.example.data.model.CandleStick
import com.example.data.model.SignalAction
import kotlin.math.max
import kotlin.math.min

data class BacktestTrade(
    val entryTime: Long,
    val exitTime: Long,
    val symbol: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val pnlUsdt: Double,
    val pnlPercent: Double,
    val exitReason: String, // TP_HIT, SL_HIT, TRAILING_STOP, SIGNAL_EXIT, END_OF_DATA
    val feePaid: Double
)

data class BacktestReport(
    val symbol: String,
    val strategy: BotStrategyType,
    val initialBalance: Double,
    val finalBalance: Double,
    val totalPnlUsdt: Double,
    val totalPnlPercent: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdownPercent: Double,
    val candleCount: Int,
    val trades: List<BacktestTrade>
)

object BacktestEngine {

    /**
     * Executes chronological simulation on real MEXC historical candles without lookahead bias.
     */
    fun runBacktest(
        symbol: String,
        candles: List<CandleStick>,
        strategy: BotStrategyType,
        initialCapital: Double = 1000.0,
        stopLossPct: Double = 0.0,
        takeProfitPct: Double = 0.0,
        trailingPct: Double = 0.0,
        allocationPct: Double = 25.0, // % of equity per trade
        feeRatePct: Double = 0.1, // MEXC spot maker/taker fee (0.1%)
        slippagePct: Double = 0.05 // realistic market slippage
    ): BacktestReport {
        if (candles.size < 35) {
            return BacktestReport(
                symbol = symbol,
                strategy = strategy,
                initialBalance = initialCapital,
                finalBalance = initialCapital,
                totalPnlUsdt = 0.0,
                totalPnlPercent = 0.0,
                totalTrades = 0,
                winningTrades = 0,
                losingTrades = 0,
                winRate = 0.0,
                profitFactor = 0.0,
                maxDrawdownPercent = 0.0,
                candleCount = candles.size,
                trades = emptyList()
            )
        }

        val effectiveSlPct = if (stopLossPct > 0) stopLossPct else strategy.defaultStopLossPct
        val effectiveTpPct = if (takeProfitPct > 0) takeProfitPct else strategy.defaultTakeProfitPct
        val effectiveTrailingPct = if (trailingPct > 0) trailingPct else strategy.defaultTrailingPct

        var equity = initialCapital
        var peakEquity = initialCapital
        var maxDrawdown = 0.0

        val trades = mutableListOf<BacktestTrade>()

        var inPosition = false
        var entryPrice = 0.0
        var entryTime = 0L
        var quantity = 0.0
        var allocatedCapital = 0.0
        var stopLossPrice = 0.0
        var takeProfitPrice = 0.0
        var highestPriceSinceEntry = 0.0
        var trailingActive = false
        var trailingStopPrice = 0.0

        val minWarmup = 30
        for (i in minWarmup until candles.size) {
            val historicalSlice = candles.subList(0, i + 1)
            val currentCandle = candles[i]
            val currentClose = currentCandle.close
            val currentHigh = currentCandle.high
            val currentLow = currentCandle.low

            if (inPosition) {
                highestPriceSinceEntry = max(highestPriceSinceEntry, currentHigh)

                // Check trailing stop activation (activated when profit >= 1.5%)
                val profitFromHighPct = ((highestPriceSinceEntry - entryPrice) / entryPrice) * 100.0
                if (profitFromHighPct >= 1.5) {
                    trailingActive = true
                    val dynamicTrailing = highestPriceSinceEntry * (1.0 - effectiveTrailingPct / 100.0)
                    trailingStopPrice = max(trailingStopPrice, dynamicTrailing)
                }

                var exitPrice: Double? = null
                var exitReason = ""

                // 1. Check Take Profit
                if (currentHigh >= takeProfitPrice) {
                    exitPrice = takeProfitPrice * (1.0 - slippagePct / 100.0)
                    exitReason = "TP_HIT (+${"%.2f".format(effectiveTpPct)}%)"
                }
                // 2. Check Trailing Stop
                else if (trailingActive && currentLow <= trailingStopPrice) {
                    exitPrice = trailingStopPrice * (1.0 - slippagePct / 100.0)
                    exitReason = "TRAILING_STOP (Profit Terkunci)"
                }
                // 3. Check Stop Loss
                else if (currentLow <= stopLossPrice) {
                    exitPrice = stopLossPrice * (1.0 - slippagePct / 100.0)
                    exitReason = "SL_HIT (-${"%.2f".format(effectiveSlPct)}%)"
                } else {
                    // 4. Check Strategy SELL Signal
                    val signal = BotStrategyEngine.evaluate(
                        historicalSlice,
                        strategy,
                        effectiveSlPct,
                        effectiveTpPct
                    )
                    if (signal.action == SignalAction.SELL) {
                        exitPrice = currentClose * (1.0 - slippagePct / 100.0)
                        exitReason = "SIGNAL_EXIT (${signal.reason.take(30)}...)"
                    }
                }

                // If exited
                if (exitPrice != null) {
                    val grossProceeds = quantity * exitPrice
                    val entryFee = allocatedCapital * (feeRatePct / 100.0)
                    val exitFee = grossProceeds * (feeRatePct / 100.0)
                    val totalFee = entryFee + exitFee

                    val netPnl = (grossProceeds - allocatedCapital) - totalFee
                    val pnlPct = (netPnl / allocatedCapital) * 100.0

                    equity += netPnl
                    peakEquity = max(peakEquity, equity)
                    val dd = if (peakEquity > 0) ((peakEquity - equity) / peakEquity) * 100.0 else 0.0
                    maxDrawdown = max(maxDrawdown, dd)

                    trades.add(
                        BacktestTrade(
                            entryTime = entryTime,
                            exitTime = currentCandle.timestamp,
                            symbol = symbol,
                            entryPrice = entryPrice,
                            exitPrice = exitPrice,
                            quantity = quantity,
                            pnlUsdt = netPnl,
                            pnlPercent = pnlPct,
                            exitReason = exitReason,
                            feePaid = totalFee
                        )
                    )

                    inPosition = false
                }
            } else {
                // Not in position: check for BUY signal
                val signal = BotStrategyEngine.evaluate(
                    historicalSlice,
                    strategy,
                    effectiveSlPct,
                    effectiveTpPct
                )

                if (signal.action == SignalAction.BUY && equity > 10.0) {
                    val tradeAlloc = equity * (allocationPct / 100.0)
                    allocatedCapital = tradeAlloc
                    entryPrice = currentClose * (1.0 + slippagePct / 100.0)
                    entryTime = currentCandle.timestamp
                    quantity = tradeAlloc / entryPrice

                    stopLossPrice = min(entryPrice * (1.0 - effectiveSlPct / 100.0), signal.suggestedStopLoss)
                    takeProfitPrice = max(entryPrice * (1.0 + effectiveTpPct / 100.0), signal.suggestedTakeProfit)
                    highestPriceSinceEntry = entryPrice
                    trailingActive = false
                    trailingStopPrice = stopLossPrice
                    inPosition = true
                }
            }
        }

        // If still in position at the end of data, close at final candle
        if (inPosition && candles.isNotEmpty()) {
            val lastCandle = candles.last()
            val exitPrice = lastCandle.close
            val grossProceeds = quantity * exitPrice
            val totalFee = (allocatedCapital + grossProceeds) * (feeRatePct / 100.0)
            val netPnl = (grossProceeds - allocatedCapital) - totalFee
            val pnlPct = (netPnl / allocatedCapital) * 100.0

            equity += netPnl
            trades.add(
                BacktestTrade(
                    entryTime = entryTime,
                    exitTime = lastCandle.timestamp,
                    symbol = symbol,
                    entryPrice = entryPrice,
                    exitPrice = exitPrice,
                    quantity = quantity,
                    pnlUsdt = netPnl,
                    pnlPercent = pnlPct,
                    exitReason = "END_OF_DATA",
                    feePaid = totalFee
                )
            )
        }

        val totalTrades = trades.size
        val winTrades = trades.count { it.pnlUsdt > 0 }
        val lossTrades = trades.count { it.pnlUsdt <= 0 }
        val winRate = if (totalTrades > 0) (winTrades.toDouble() / totalTrades) * 100.0 else 0.0

        val grossProfit = trades.filter { it.pnlUsdt > 0 }.sumOf { it.pnlUsdt }
        val grossLoss = kotlin.math.abs(trades.filter { it.pnlUsdt < 0 }.sumOf { it.pnlUsdt })
        val profitFactor = if (grossLoss > 0.0) grossProfit / grossLoss else if (grossProfit > 0) 99.9 else 0.0

        val totalPnlUsdt = equity - initialCapital
        val totalPnlPercent = ((equity - initialCapital) / initialCapital) * 100.0

        return BacktestReport(
            symbol = symbol,
            strategy = strategy,
            initialBalance = initialCapital,
            finalBalance = equity,
            totalPnlUsdt = totalPnlUsdt,
            totalPnlPercent = totalPnlPercent,
            totalTrades = totalTrades,
            winningTrades = winTrades,
            losingTrades = lossTrades,
            winRate = winRate,
            profitFactor = profitFactor,
            maxDrawdownPercent = maxDrawdown,
            candleCount = candles.size,
            trades = trades
        )
    }
}
