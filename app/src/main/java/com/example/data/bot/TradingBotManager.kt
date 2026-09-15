package com.example.data.bot

import android.content.Context
import com.example.data.calculator.IndicatorCalculator
import com.example.data.gemini.GeminiTraderClient
import com.example.data.gemini.GeminiTraderDecision
import com.example.data.local.AppDatabase
import com.example.data.local.BotTradeEntity
import com.example.data.mexc.MexcApiClient
import com.example.data.model.CandleStick
import com.example.data.model.SignalAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class ActivePosition(
    val symbol: String,
    val entryPrice: Double,
    val currentPrice: Double,
    val quantity: Double,
    val allocatedUsdt: Double,
    val stopLossPrice: Double,
    val takeProfitPrice: Double,
    val trailingStopPrice: Double,
    val highestPrice: Double,
    val unrealizedPnl: Double,
    val unrealizedPnlPct: Double,
    val entryTime: Long,
    val orderId: String,
    val isSandbox: Boolean
)

data class BotUiState(
    val isRunning: Boolean = false,
    val isSandbox: Boolean = true,
    val apiKey: String = "",
    val secretKey: String = "",
    val customGeminiApiKey: String = "",
    val isGeminiRateLimited: Boolean = false,
    val geminiRateLimitCooldownSec: Long = 0L,
    val strategy: BotStrategyType = BotStrategyType.GEMINI_AI_TRADER,
    val sandboxBalance: Double = 1000.0,
    val realUsdtBalance: Double = 0.0,
    val activePosition: ActivePosition? = null,
    val tradeAllocationPct: Double = 20.0,
    val customSlPct: Double = 1.8,
    val customTpPct: Double = 4.2,
    val customTrailingPct: Double = 1.2,
    val cooldownSeconds: Int = 180,
    val lastSignalReason: String = "Menunggu evaluasi Gemini AI Trader",
    val terminalLogs: List<String> = emptyList(),
    val totalRealizedPnl: Double = 0.0,
    val totalTradesCount: Int = 0,
    val winTradesCount: Int = 0,
    val isConnectingMexc: Boolean = false,
    val mexcConnectionStatus: String = "Belum Terhubung",
    val isGeminiThinking: Boolean = false,
    val lastGeminiThesis: String = "",
    val lastGeminiConfidence: Double = 0.0,
    val isLiveGeminiResponse: Boolean = false,
    val activeBlueprint: GeminiTradeBlueprint? = null
)

class TradingBotManager(
    private val context: Context,
    private val mexcApiClient: MexcApiClient = MexcApiClient(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val geminiTraderClient: GeminiTraderClient = GeminiTraderClient()
) {
    private val prefs = context.getSharedPreferences("mexc_autobot_prefs", Context.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(context)
    private val tradeDao = db.botTradeDao()

    private val _botState = MutableStateFlow(
        BotUiState(
            apiKey = prefs.getString("api_key", "") ?: "",
            secretKey = prefs.getString("secret_key", "") ?: "",
            customGeminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
            isSandbox = prefs.getBoolean("is_sandbox", true),
            sandboxBalance = prefs.getFloat("sandbox_balance", 1000f).toDouble(),
            tradeAllocationPct = prefs.getFloat("alloc_pct", 20f).toDouble(),
            customSlPct = prefs.getFloat("sl_pct", 1.8f).toDouble(),
            customTpPct = prefs.getFloat("tp_pct", 4.2f).toDouble(),
            strategy = try {
                BotStrategyType.valueOf(prefs.getString("strategy", BotStrategyType.GEMINI_AI_TRADER.name) ?: BotStrategyType.GEMINI_AI_TRADER.name)
            } catch (e: Exception) {
                BotStrategyType.GEMINI_AI_TRADER
            }
        )
    )
    val botState: StateFlow<BotUiState> = _botState.asStateFlow()

    private var lastTradeTime = 0L
    private var lastGeminiEvaluationTime = 0L

    init {
        logTerminal("🤖 Trading Bot Initialized (Executor: Gemini AI Trader & MEXC Engine)")
        loadStatsFromDb()
    }

    private fun loadStatsFromDb() {
        scope.launch {
            tradeDao.getAllTrades().collect { list ->
                val totalPnl = list.sumOf { it.pnlUsdt }
                val totalCount = list.size
                val winCount = list.count { it.pnlUsdt > 0 }
                _botState.update {
                    it.copy(
                        totalRealizedPnl = totalPnl,
                        totalTradesCount = totalCount,
                        winTradesCount = winCount
                    )
                }
            }
        }
    }

    fun setCredentials(key: String, secret: String) {
        prefs.edit().putString("api_key", key).putString("secret_key", secret).apply()
        _botState.update { it.copy(apiKey = key, secretKey = secret) }
        logTerminal("🔑 Kredensial MEXC diperbarui")
    }

    fun setGeminiApiKey(geminiKey: String) {
        prefs.edit().putString("gemini_api_key", geminiKey.trim()).apply()
        _botState.update {
            it.copy(
                customGeminiApiKey = geminiKey.trim(),
                isGeminiRateLimited = false,
                geminiRateLimitCooldownSec = 0L
            )
        }
        GeminiTraderClient.resetRateLimit()
        logTerminal("🤖 Kunci Gemini API diperbarui dan status batas kuota di-reset")
    }

    fun executeInstantManualBuy(
        symbol: String,
        currentPrice: Double,
        candles: List<CandleStick>
    ) {
        val position = _botState.value.activePosition
        if (position != null) {
            logTerminal("ℹ️ Posisi pada $symbol sudah terbuka ($${String.format(Locale.US, "%,.2f", position.entryPrice)}).")
            return
        }
        val indicators = IndicatorCalculator.calculateAllIndicators(candles)
        val atr = max(indicators.currentAtr ?: (currentPrice * 0.015), currentPrice * 0.005)
        val sl = currentPrice * (1.0 - _botState.value.customSlPct / 100.0)
        val tp = currentPrice * (1.0 + _botState.value.customTpPct / 100.0)
        val decision = BotSignalDecision(
            action = SignalAction.BUY,
            confidence = 0.95,
            reason = "⚡ Order Beli Pasar Instan Pengguna dengan Trailing Stop Otomatis",
            suggestedEntry = currentPrice,
            suggestedStopLoss = sl,
            suggestedTakeProfit = tp,
            atr = atr,
            indicators = indicators
        )
        logTerminal("⚡ [EKSEKUSI INSTAN] Membuka posisi pasar langsung pada $symbol...")
        openPositionInternal(symbol, currentPrice, decision)
    }

    fun setSandboxMode(isSandbox: Boolean) {
        prefs.edit().putBoolean("is_sandbox", isSandbox).apply()
        _botState.update { it.copy(isSandbox = isSandbox) }
        val modeName = if (isSandbox) "SANDBOX (Demo / Order Test)" else "REAL TRADING (Live Matching Engine)"
        logTerminal("⚙️ Mode diubah ke: $modeName")
    }

    fun setStrategy(strategy: BotStrategyType) {
        prefs.edit().putString("strategy", strategy.name).apply()
        _botState.update {
            it.copy(
                strategy = strategy,
                customSlPct = strategy.defaultStopLossPct,
                customTpPct = strategy.defaultTakeProfitPct,
                customTrailingPct = strategy.defaultTrailingPct
            )
        }
        logTerminal("📐 Strategi aktif: ${strategy.title}")
    }

    fun updateRiskParameters(allocPct: Double, slPct: Double, tpPct: Double, trailingPct: Double) {
        prefs.edit()
            .putFloat("alloc_pct", allocPct.toFloat())
            .putFloat("sl_pct", slPct.toFloat())
            .putFloat("tp_pct", tpPct.toFloat())
            .apply()
        _botState.update {
            it.copy(
                tradeAllocationPct = allocPct,
                customSlPct = slPct,
                customTpPct = tpPct,
                customTrailingPct = trailingPct
            )
        }
        logTerminal("🛡️ Parameter risiko diperbarui: Alloc ${allocPct}%, SL ${slPct}%, TP ${tpPct}%")
    }

    fun startBot() {
        _botState.update { it.copy(isRunning = true) }
        val mode = if (_botState.value.isSandbox) "SANDBOX DEMO" else "REAL LIVE TRADING"
        logTerminal("🚀 BOT AKTIF: Beroperasi pada mode $mode dengan strategi ${_botState.value.strategy.title}")
    }

    fun stopBot() {
        _botState.update { it.copy(isRunning = false) }
        logTerminal("⏸️ BOT DIHENTIKAN: Pengawasan dan eksekusi otomatis dinonaktifkan")
    }

    fun resetSandboxBalance(amount: Double = 1000.0) {
        prefs.edit().putFloat("sandbox_balance", amount.toFloat()).apply()
        _botState.update { it.copy(sandboxBalance = amount) }
        logTerminal("💰 Saldo Sandbox direset menjadi: $amount USDT")
    }

    fun testMexcConnection() {
        scope.launch {
            _botState.update { it.copy(isConnectingMexc = true, mexcConnectionStatus = "Menghubungkan...") }
            logTerminal("🌐 Menguji koneksi MEXC API...")

            val pingRes = mexcApiClient.ping()
            if (pingRes.isFailure) {
                _botState.update {
                    it.copy(
                        isConnectingMexc = false,
                        mexcConnectionStatus = "Gagal Terhubung: ${pingRes.exceptionOrNull()?.message}"
                    )
                }
                logTerminal("❌ Ping MEXC gagal: ${pingRes.exceptionOrNull()?.message}")
                return@launch
            }

            val timeRes = mexcApiClient.getServerTime()
            val serverTime = timeRes.getOrNull() ?: 0L
            val localTime = System.currentTimeMillis()
            val drift = kotlin.math.abs(localTime - serverTime)
            logTerminal("⏱️ MEXC Server Time: $serverTime (Drift: ${drift}ms)")

            val key = _botState.value.apiKey
            val secret = _botState.value.secretKey

            if (key.isNotBlank() && secret.isNotBlank()) {
                val accRes = mexcApiClient.getAccountInfo(key, secret)
                if (accRes.isSuccess) {
                    val acc = accRes.getOrThrow()
                    val usdtFree = acc.getUsdtFree()
                    _botState.update {
                        it.copy(
                            isConnectingMexc = false,
                            realUsdtBalance = usdtFree,
                            mexcConnectionStatus = "Terhubung (Saldo: ${"%.2f".format(usdtFree)} USDT)"
                        )
                    }
                    logTerminal("✅ MEXC API Terautentikasi! Saldo Riil: $usdtFree USDT, CanTrade: ${acc.canTrade}")
                } else {
                    val err = accRes.exceptionOrNull()?.message ?: "Gagal autentikasi"
                    _botState.update {
                        it.copy(
                            isConnectingMexc = false,
                            mexcConnectionStatus = "Autentikasi Gagal: $err"
                        )
                    }
                    logTerminal("⚠️ Autentikasi MEXC gagal: $err")
                }
            } else {
                _botState.update {
                    it.copy(
                        isConnectingMexc = false,
                        mexcConnectionStatus = "Koneksi Publik MEXC OK (Kunci API belum diisi)"
                    )
                }
                logTerminal("ℹ️ MEXC Online. Masukkan API Key & Secret untuk akses akun/trading.")
            }
        }
    }

    /**
     * Executes a fast test order using MEXC's official POST /api/v3/order/test
     */
    fun sendQuickMexcTestOrder(symbol: String, quoteAmount: Double = 20.0) {
        scope.launch {
            val key = _botState.value.apiKey
            val secret = _botState.value.secretKey
            if (key.isBlank() || secret.isBlank()) {
                logTerminal("⚠️ Masukkan API Key & Secret Key untuk melakukan Test Order MEXC")
                return@launch
            }
            logTerminal("🧪 Mengirim Test Order (POST /api/v3/order/test) untuk $symbol sebesar $quoteAmount USDT...")
            val result = mexcApiClient.sendTestOrder(
                apiKey = key,
                secretKey = secret,
                symbol = symbol,
                side = "BUY",
                type = "MARKET",
                quoteOrderQty = quoteAmount
            )
            if (result.isSuccess) {
                logTerminal("✅ Test Order Validasi MEXC SUKSES! Parameter, signature HMAC & filter bursa valid.")
            } else {
                logTerminal("❌ Test Order Ditolak MEXC: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Main algorithmic heartbeat. Runs on each price tick / refresh.
     */
    fun evaluateMarketTick(
        symbol: String,
        currentPrice: Double,
        candles: List<CandleStick>
    ) {
        val state = _botState.value
        val position = state.activePosition

        // 1. If currently in an active position: monitor PnL, SL, TP, Trailing Stop
        if (position != null && position.symbol.equals(symbol, ignoreCase = true)) {
            val unrealizedPnl = (currentPrice - position.entryPrice) * position.quantity
            val unrealizedPnlPct = ((currentPrice - position.entryPrice) / position.entryPrice) * 100.0
            val newHighest = max(position.highestPrice, currentPrice)

            // Trailing Stop logic
            val profitFromHighPct = ((newHighest - position.entryPrice) / position.entryPrice) * 100.0
            var newTrailingStop = position.trailingStopPrice
            if (profitFromHighPct >= 1.5) {
                val calculatedTrailing = newHighest * (1.0 - state.customTrailingPct / 100.0)
                newTrailingStop = max(newTrailingStop, calculatedTrailing)
            }

            // Check Exits
            var shouldClose = false
            var closeReason = ""

            if (currentPrice >= position.takeProfitPrice) {
                shouldClose = true
                closeReason = "TAKE_PROFIT_HIT (+${"%.2f".format(unrealizedPnlPct)}%)"
            } else if (currentPrice <= newTrailingStop && profitFromHighPct >= 1.5) {
                shouldClose = true
                closeReason = "TRAILING_STOP_HIT (Terkunci +${"%.2f".format(unrealizedPnlPct)}%)"
            } else if (currentPrice <= position.stopLossPrice) {
                shouldClose = true
                closeReason = "STOP_LOSS_HIT (-${"%.2f".format(unrealizedPnlPct)}%)"
            } else {
                // Check strategy sell signal
                val decision = BotStrategyEngine.evaluate(
                    candles,
                    state.strategy,
                    state.customSlPct,
                    state.customTpPct
                )
                if (decision.action == SignalAction.SELL) {
                    shouldClose = true
                    closeReason = "STRATEGY_SELL_SIGNAL (${decision.reason.take(25)}...)"
                }
            }

            if (shouldClose) {
                closePositionInternal(position, currentPrice, closeReason)
            } else {
                // Update position mark price
                _botState.update {
                    it.copy(
                        activePosition = position.copy(
                            currentPrice = currentPrice,
                            highestPrice = newHighest,
                            trailingStopPrice = newTrailingStop,
                            unrealizedPnl = unrealizedPnl,
                            unrealizedPnlPct = unrealizedPnlPct
                        )
                    )
                }

                // If Gemini AI Trader is active, also let Gemini monitor position exit periodically
                if (state.strategy == BotStrategyType.GEMINI_AI_TRADER) {
                    val now = System.currentTimeMillis()
                    if (now - lastGeminiEvaluationTime >= 60_000L && !_botState.value.isGeminiThinking) {
                        lastGeminiEvaluationTime = now
                        triggerGeminiTraderEvaluation(symbol, currentPrice, candles, position, forceManual = false)
                    }
                }
            }
            return
        }

        // 2. If NOT in position and Bot is RUNNING: Evaluate BUY signals
        if (state.isRunning && position == null && candles.size >= 30) {
            val now = System.currentTimeMillis()
            if (now - lastTradeTime < (state.cooldownSeconds * 1000L)) {
                return // In anti-whipsaw cooldown
            }

            if (state.strategy == BotStrategyType.GEMINI_AI_TRADER) {
                val isRateLimited = GeminiTraderClient.isCurrentlyRateLimited()
                if (isRateLimited) {
                    val remainingSec = GeminiTraderClient.getRemainingCooldownSeconds()
                    _botState.update {
                        it.copy(
                            isGeminiRateLimited = true,
                            geminiRateLimitCooldownSec = remainingSec,
                            lastSignalReason = "⚠️ Kuota Gemini Limit (429). Eksekusi aktif via Engine Kuantitatif Lokal (${remainingSec}s)"
                        )
                    }
                    // Execute using local quantitative engine so trading never stops
                    val decision = BotStrategyEngine.evaluate(
                        candles,
                        BotStrategyType.TREND_MOMENTUM_CONFLUENCE,
                        state.customSlPct,
                        state.customTpPct
                    )
                    if (decision.action == SignalAction.BUY && decision.confidence >= 0.75) {
                        logTerminal("⚡ [ALGO BUY - Fallback Gemini 429] Sinyal Terkonfirmasi: ${decision.reason}")
                        openPositionInternal(symbol, currentPrice, decision)
                    }
                    return
                } else {
                    if (_botState.value.isGeminiRateLimited) {
                        _botState.update { it.copy(isGeminiRateLimited = false, geminiRateLimitCooldownSec = 0L) }
                    }
                }

                // ==========================================================
                // ARSITEKTUR PATOKAN ANALISIS GEMINI (MILESTONE TRIGGER)
                // Memeriksa Blueprint Lokal sebelum memanggil Gemini API
                // ==========================================================
                val activePlan = state.activeBlueprint
                val indicators = IndicatorCalculator.calculateAllIndicators(candles)

                if (activePlan != null && !activePlan.isExpired()) {
                    // Blueprint Gemini masih aktif! Pantau harga lokal secara gratis tanpa API call
                    val conditionMet = activePlan.isTriggerConditionMet(currentPrice, indicators)
                    val remainingSec = activePlan.getRemainingSeconds()

                    if (conditionMet) {
                        logTerminal("🎯 [Pemicu Patokan Terpenuhi!] Harga ($currentPrice) menyentuh level rencana Gemini (${activePlan.triggerMinPrice} - ${activePlan.triggerMaxPrice}). Memanggil Gemini untuk konfirmasi final & eksekusi!")
                        _botState.update {
                            it.copy(
                                activeBlueprint = null, // Hapus blueprint setelah dipicu
                                lastSignalReason = "🎯 Target pemicu tercapai: Meminta konfirmasi eksekusi order!"
                            )
                        }
                        lastGeminiEvaluationTime = now
                        triggerGeminiTraderEvaluation(symbol, currentPrice, candles, null, forceManual = true)
                        return
                    } else {
                        // Belum menyentuh trigger, tetap diam dan hemat API 100%
                        _botState.update {
                            it.copy(
                                lastSignalReason = "🎯 Memantau Patokan Gemini: Target $${String.format(Locale.US, "%,.2f", activePlan.triggerMinPrice)} - $${String.format(Locale.US, "%,.2f", activePlan.triggerMaxPrice)} (Berlaku ${remainingSec}s)"
                            )
                        }
                        return
                    }
                }

                // Jika belum ada blueprint atau blueprint sudah kadaluwarsa:
                // Lakukan evaluasi awal Gemini hanya saat setup teknikal matang atau jeda waktu cukup
                val elapsed = now - lastGeminiEvaluationTime
                val isPrimed = isTechnicalSetupPrimed(candles, currentPrice)
                val shouldAnalyzeNewPlan = (elapsed >= 90_000L && isPrimed) || elapsed >= 180_000L

                if (shouldAnalyzeNewPlan && !_botState.value.isGeminiThinking) {
                    lastGeminiEvaluationTime = now
                    triggerGeminiTraderEvaluation(symbol, currentPrice, candles, null, forceManual = false)
                }
            } else {
                val decision = BotStrategyEngine.evaluate(
                    candles,
                    state.strategy,
                    state.customSlPct,
                    state.customTpPct
                )
                _botState.update { it.copy(lastSignalReason = decision.reason) }

                if (decision.action == SignalAction.BUY && decision.confidence >= 0.75) {
                    openPositionInternal(symbol, currentPrice, decision)
                }
            }
        }
    }

    private fun isTechnicalSetupPrimed(candles: List<CandleStick>, currentPrice: Double): Boolean {
        if (candles.size < 20) return false
        val indicators = IndicatorCalculator.calculateAllIndicators(candles)
        val rsi = indicators.currentRsi ?: 50.0
        val lastIdx = candles.size - 1
        val ema9 = indicators.ema9.getOrNull(lastIdx) ?: currentPrice
        val ema21 = indicators.ema21.getOrNull(lastIdx) ?: currentPrice
        val macdHist = indicators.currentMacd?.histogram ?: 0.0

        val isEmaConverging = kotlin.math.abs(ema9 - ema21) / currentPrice < 0.005 || ema9 >= ema21
        val isRsiActionable = rsi in 35.0..65.0
        val isMacdImproving = macdHist >= 0.0

        return (isEmaConverging && isRsiActionable) || isMacdImproving
    }

    /**
     * Trigger on-demand or autonomous Gemini AI analysis and trade execution.
     */
    fun requestGeminiTraderExecution(
        symbol: String,
        currentPrice: Double,
        candles: List<CandleStick>
    ) {
        val position = _botState.value.activePosition
        triggerGeminiTraderEvaluation(symbol, currentPrice, candles, position, forceManual = true)
    }

    private fun triggerGeminiTraderEvaluation(
        symbol: String,
        currentPrice: Double,
        candles: List<CandleStick>,
        position: ActivePosition?,
        forceManual: Boolean = false
    ) {
        if (candles.size < 15) {
            logTerminal("⚠️ [Gemini Trader] Data candle belum cukup untuk analisis (minimal 15 bar)")
            return
        }

        scope.launch {
            _botState.update { it.copy(isGeminiThinking = true) }
            val intentLabel = if (forceManual) "Permintaan Eksekusi Cepat" else "Evaluasi Rutin"
            logTerminal("🧠 [Gemini Trader - $intentLabel] Menganalisis aksi harga dan indikator $symbol...")

            val indicators = IndicatorCalculator.calculateAllIndicators(candles)
            val pnlPct = if (position != null) {
                ((currentPrice - position.entryPrice) / position.entryPrice) * 100.0
            } else 0.0

            val decision = geminiTraderClient.evaluateMarketForTrading(
                symbol = symbol,
                currentPrice = currentPrice,
                candles = candles,
                indicators = indicators,
                currentPositionHeld = position != null,
                positionEntryPrice = position?.entryPrice ?: 0.0,
                unrealizedPnlPct = pnlPct,
                customApiKey = _botState.value.customGeminiApiKey,
                forceManual = forceManual
            )

            if (decision.isRateLimited) {
                _botState.update {
                    it.copy(
                        isGeminiRateLimited = true,
                        geminiRateLimitCooldownSec = decision.rateLimitRemainingSeconds
                    )
                }
                logTerminal("⚠️ [Gemini Rate Limit 429] Kuota API limit. Beralih ke Eksekusi Engine Kuantitatif Lokal.")
            } else {
                _botState.update {
                    it.copy(
                        isGeminiRateLimited = false,
                        geminiRateLimitCooldownSec = 0L
                    )
                }
            }

            val sourceTag = when {
                decision.isLiveGeminiResponse -> "Gemini Live AI"
                decision.isRateLimited -> "Engine Kuantitatif (429 Fallback)"
                else -> "Engine Kuantitatif Lokal"
            }
            logTerminal("🤖 [Gemini Trader - $sourceTag] Rekomendasi: ${decision.action.label} (Confidence: ${"%.0f".format(decision.confidence * 100)}%)")
            logTerminal("💡 Thesis: ${decision.tradeThesis}")

            _botState.update {
                it.copy(
                    isGeminiThinking = false,
                    lastGeminiThesis = decision.tradeThesis,
                    lastGeminiConfidence = decision.confidence,
                    isLiveGeminiResponse = decision.isLiveGeminiResponse,
                    lastSignalReason = "[$sourceTag] ${decision.tradeThesis}"
                )
            }

            // Execute trading if conditions met
            if (decision.action == SignalAction.BUY && decision.shouldExecute) {
                if (position == null) {
                    val botSignal = BotSignalDecision(
                        action = SignalAction.BUY,
                        confidence = decision.confidence,
                        reason = "[$sourceTag] ${decision.tradeThesis}",
                        suggestedEntry = decision.suggestedEntry,
                        suggestedStopLoss = decision.suggestedStopLoss,
                        suggestedTakeProfit = decision.suggestedTakeProfit,
                        atr = indicators.currentAtr ?: (currentPrice * 0.015),
                        indicators = indicators
                    )
                    // Reset blueprint
                    _botState.update { it.copy(activeBlueprint = null) }
                    openPositionInternal(symbol, currentPrice, botSignal)
                } else {
                    logTerminal("ℹ️ [Gemini Trader] Sinyal BUY terkonfirmasi namun posisi sudah aktif.")
                }
            } else if (decision.action == SignalAction.SELL && decision.shouldExecute && position != null) {
                _botState.update { it.copy(activeBlueprint = null) }
                closePositionInternal(position, currentPrice, "EXIT: ${decision.tradeThesis.take(28)}...")
            } else {
                // Belum waktunya eksekusi langsung, simpan Rencana Patokan (Blueprint)
                if (position == null) {
                    val blueprint = GeminiTradeBlueprint(
                        symbol = symbol,
                        initialAnalysisPrice = currentPrice,
                        triggerAction = SignalAction.BUY,
                        triggerCondition = decision.triggerCondition,
                        triggerMinPrice = decision.triggerMinPrice,
                        triggerMaxPrice = decision.triggerMaxPrice,
                        triggerRsiMin = decision.triggerRsiMin,
                        triggerRsiMax = decision.triggerRsiMax,
                        suggestedStopLoss = decision.suggestedStopLoss,
                        suggestedTakeProfit = decision.suggestedTakeProfit,
                        confidence = decision.confidence,
                        thesis = decision.tradeThesis,
                        validityTtlMinutes = decision.planValidityMinutes
                    )
                    _botState.update { it.copy(activeBlueprint = blueprint) }
                    logTerminal("📋 [Patokan Rencana Terpasang] Menunggu level pemicu $${String.format(Locale.US, "%,.2f", blueprint.triggerMinPrice)} - $${String.format(Locale.US, "%,.2f", blueprint.triggerMaxPrice)} (Berlaku ${blueprint.validityTtlMinutes}m, Zero-API-Cost)")
                }
                if (forceManual) {
                    logTerminal("🛡️ [Gemini Trader] Posisi STANDBY: Patokan telah dipasang, bot akan mengeksekusi seketika pemicu tersentuh.")
                }
            }
        }
    }

    private fun openPositionInternal(
        symbol: String,
        currentPrice: Double,
        decision: BotSignalDecision
    ) {
        val state = _botState.value
        val isSandbox = state.isSandbox
        val allocPct = state.tradeAllocationPct

        val capital = if (isSandbox) state.sandboxBalance else state.realUsdtBalance
        val tradeUsdt = max(15.0, capital * (allocPct / 100.0))

        if (capital < 10.0) {
            logTerminal("⚠️ Saldo tidak mencukupi untuk membuka posisi (${"%.2f".format(capital)} USDT)")
            return
        }

        val qty = tradeUsdt / currentPrice
        val slPrice = min(currentPrice * (1.0 - state.customSlPct / 100.0), decision.suggestedStopLoss)
        val tpPrice = max(currentPrice * (1.0 + state.customTpPct / 100.0), decision.suggestedTakeProfit)

        scope.launch {
            if (isSandbox) {
                // Sandbox Mode
                // If API keys are filled, also test with MEXC POST /api/v3/order/test
                if (state.apiKey.isNotBlank() && state.secretKey.isNotBlank()) {
                    val testRes = mexcApiClient.sendTestOrder(
                        apiKey = state.apiKey,
                        secretKey = state.secretKey,
                        symbol = symbol,
                        side = "BUY",
                        type = "MARKET",
                        quoteOrderQty = tradeUsdt
                    )
                    if (testRes.isSuccess) {
                        logTerminal("✅ [Sandbox] MEXC /order/test tervalidasi di server MEXC")
                    } else {
                        logTerminal("⚠️ [Sandbox] MEXC /order/test respon: ${testRes.exceptionOrNull()?.message}")
                    }
                }

                val newBalance = state.sandboxBalance - tradeUsdt
                prefs.edit().putFloat("sandbox_balance", newBalance.toFloat()).apply()

                val newPos = ActivePosition(
                    symbol = symbol,
                    entryPrice = currentPrice,
                    currentPrice = currentPrice,
                    quantity = qty,
                    allocatedUsdt = tradeUsdt,
                    stopLossPrice = slPrice,
                    takeProfitPrice = tpPrice,
                    trailingStopPrice = slPrice,
                    highestPrice = currentPrice,
                    unrealizedPnl = 0.0,
                    unrealizedPnlPct = 0.0,
                    entryTime = System.currentTimeMillis(),
                    orderId = "SANDBOX-${System.currentTimeMillis()}",
                    isSandbox = true
                )

                lastTradeTime = System.currentTimeMillis()
                _botState.update {
                    it.copy(
                        sandboxBalance = newBalance,
                        activePosition = newPos
                    )
                }

                logTerminal("🟢 [SANDBOX BUY] $symbol | Harga: $currentPrice | Alokasi: ${"%.2f".format(tradeUsdt)} USDT | SL: ${"%.2f".format(slPrice)} | TP: ${"%.2f".format(tpPrice)}")
            } else {
                // Real Live Trading Mode
                if (state.apiKey.isBlank() || state.secretKey.isBlank()) {
                    logTerminal("❌ Real Trading Gagal: API Key atau Secret Key belum diisi!")
                    return@launch
                }

                logTerminal("⚡ [REAL BUY] Mengirim order live ke matching engine MEXC...")
                val orderRes = mexcApiClient.sendRealOrder(
                    apiKey = state.apiKey,
                    secretKey = state.secretKey,
                    symbol = symbol,
                    side = "BUY",
                    type = "MARKET",
                    quoteOrderQty = tradeUsdt
                )

                if (orderRes.isSuccess) {
                    val order = orderRes.getOrThrow()
                    val actualQty = if (order.executedQty > 0) order.executedQty else qty
                    val actualPrice = if (order.price > 0) order.price else currentPrice

                    val newPos = ActivePosition(
                        symbol = symbol,
                        entryPrice = actualPrice,
                        currentPrice = actualPrice,
                        quantity = actualQty,
                        allocatedUsdt = tradeUsdt,
                        stopLossPrice = slPrice,
                        takeProfitPrice = tpPrice,
                        trailingStopPrice = slPrice,
                        highestPrice = actualPrice,
                        unrealizedPnl = 0.0,
                        unrealizedPnlPct = 0.0,
                        entryTime = System.currentTimeMillis(),
                        orderId = order.orderId,
                        isSandbox = false
                    )

                    lastTradeTime = System.currentTimeMillis()
                    _botState.update { it.copy(activePosition = newPos) }
                    logTerminal("✅ [REAL ORDER TERISI] Order ID: ${order.orderId} | Qty: $actualQty | Harga: $actualPrice")
                } else {
                    logTerminal("❌ [REAL ORDER GAGAL] ${orderRes.exceptionOrNull()?.message}")
                }
            }
        }
    }

    private fun closePositionInternal(
        position: ActivePosition,
        exitPrice: Double,
        reason: String
    ) {
        val state = _botState.value
        val isSandbox = position.isSandbox
        val feeRate = 0.001 // 0.1% spot fee
        val grossReturn = position.quantity * exitPrice
        val totalFee = (position.allocatedUsdt + grossReturn) * feeRate
        val netPnl = (grossReturn - position.allocatedUsdt) - totalFee
        val pnlPct = (netPnl / position.allocatedUsdt) * 100.0

        scope.launch {
            if (!isSandbox) {
                // If real trading, submit MARKET SELL order to MEXC
                if (state.apiKey.isNotBlank() && state.secretKey.isNotBlank()) {
                    logTerminal("⚡ [REAL SELL] Menutup posisi di MEXC Spot...")
                    val sellRes = mexcApiClient.sendRealOrder(
                        apiKey = state.apiKey,
                        secretKey = state.secretKey,
                        symbol = position.symbol,
                        side = "SELL",
                        type = "MARKET",
                        quantity = position.quantity
                    )
                    if (sellRes.isSuccess) {
                        logTerminal("✅ [REAL SELL TERISI] Order ID: ${sellRes.getOrThrow().orderId}")
                    } else {
                        logTerminal("⚠️ Real Sell Warning: ${sellRes.exceptionOrNull()?.message}")
                    }
                }
            } else {
                // Refund sandbox capital + net profit
                val updatedSandboxBalance = state.sandboxBalance + position.allocatedUsdt + netPnl
                prefs.edit().putFloat("sandbox_balance", updatedSandboxBalance.toFloat()).apply()
                _botState.update { it.copy(sandboxBalance = updatedSandboxBalance) }
            }

            // Record to Room Database
            val tradeRecord = BotTradeEntity(
                symbol = position.symbol,
                side = "BUY->SELL",
                entryPrice = position.entryPrice,
                exitPrice = exitPrice,
                quantity = position.quantity,
                pnlUsdt = netPnl,
                pnlPercent = pnlPct,
                exitReason = reason,
                isSandbox = isSandbox,
                strategyName = state.strategy.title,
                orderId = position.orderId
            )
            tradeDao.insertTrade(tradeRecord)

            _botState.update { it.copy(activePosition = null) }
            lastTradeTime = System.currentTimeMillis()

            val pnlSign = if (netPnl >= 0) "+${"%.2f".format(netPnl)}" else "%.2f".format(netPnl)
            val pnlEmoji = if (netPnl >= 0) "🟢" else "🔴"
            logTerminal("$pnlEmoji [POSISI DITUTUP] ${position.symbol} | PnL: $pnlSign USDT (${"%.2f".format(pnlPct)}%) | Alasan: $reason")
        }
    }

    fun manualClosePosition(reason: String = "MANUAL_CLOSE") {
        val pos = _botState.value.activePosition ?: return
        closePositionInternal(pos, pos.currentPrice, reason)
    }

    fun clearTradeHistory() {
        scope.launch {
            tradeDao.clearTradesByMode(_botState.value.isSandbox)
            logTerminal("🗑️ Riwayat trading dihapus untuk mode saat ini")
        }
    }

    private fun logTerminal(msg: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$timeStr] $msg"
        _botState.update {
            val list = listOf(entry) + it.terminalLogs
            it.copy(terminalLogs = list.take(100))
        }
    }
}
