package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.bot.BacktestEngine
import com.example.data.bot.BacktestReport
import com.example.data.bot.BotStrategyType
import com.example.data.bot.BotUiState
import com.example.data.bot.TradingBotManager
import com.example.data.calculator.IndicatorCalculator
import com.example.data.fetcher.MarketDataFetcher
import com.example.data.gemini.ChartImageRenderer
import com.example.data.gemini.GeminiAnalystClient
import com.example.data.mexc.Mexc24hTicker
import com.example.data.mexc.MexcApiClient
import com.example.data.model.AlertType
import com.example.data.model.AiAnalysisResult
import com.example.data.model.AssetType
import com.example.data.model.CandleStick
import com.example.data.model.ExchangePlatform
import com.example.data.model.LiveSignal
import com.example.data.model.MarketAsset
import com.example.data.model.PriceAlert
import com.example.data.model.SignalAction
import com.example.data.model.TechnicalIndicators
import com.example.data.model.Timeframe
import com.example.service.notification.SignalNotificationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class MarketUiState(
    val assets: List<MarketAsset> = MarketDataFetcher.DEFAULT_ASSETS,
    val selectedAsset: MarketAsset = MarketDataFetcher.DEFAULT_ASSETS.first(),
    val selectedTimeframe: Timeframe = Timeframe.H1,
    val candles: List<CandleStick> = emptyList(),
    val indicators: TechnicalIndicators = IndicatorCalculator.calculateAllIndicators(emptyList()),
    val isChartLoading: Boolean = false,
    val isAiAnalyzing: Boolean = false,
    val aiAnalysisResult: AiAnalysisResult? = null,
    val latestChartBitmap: Bitmap? = null,
    val activeAlerts: List<PriceAlert> = emptyList(),
    val triggeredSignals: List<LiveSignal> = emptyList(),
    val errorMessage: String? = null,
    val isLiveFeedActive: Boolean = true,
    val searchResults: List<com.example.data.model.SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val isAutoRefreshEnabled: Boolean = true,
    val autoRefreshIntervalSeconds: Int = 5,
    val refreshCountdown: Int = 5,
    val isRefreshingPrice: Boolean = false,
    val lastRefreshedTimeMillis: Long = System.currentTimeMillis()
)

class MarketViewModel(application: Application) : AndroidViewModel(application) {

    private val fetcher = MarketDataFetcher()
    private val geminiClient = GeminiAnalystClient()
    private val notificationManager = SignalNotificationManager(application)
    val mexcClient = MexcApiClient()
    val tradingBotManager = TradingBotManager(application, mexcClient, viewModelScope)
    val botState: StateFlow<BotUiState> = tradingBotManager.botState

    private val _backtestReport = MutableStateFlow<BacktestReport?>(null)
    val backtestReport: StateFlow<BacktestReport?> = _backtestReport.asStateFlow()

    private val _isBacktesting = MutableStateFlow(false)
    val isBacktesting: StateFlow<Boolean> = _isBacktesting.asStateFlow()

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private var priceTickerJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadMarketData()
        fetchTradingViewRatings()
        startRealPriceTicker()
    }

    fun searchSymbols(query: String, filter: String? = null) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = fetcher.searchTradingViewSymbols(query, filter)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun clearSearchResults() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
    }

    fun addCustomAsset(item: com.example.data.model.SearchResultItem) {
        val newAsset = fetcher.createMarketAssetFromSearch(item)
        _uiState.update { state ->
            val existing = state.assets.firstOrNull { it.tvSymbol == newAsset.tvSymbol }
            if (existing != null) {
                state.copy(selectedAsset = existing, aiAnalysisResult = null, latestChartBitmap = null)
            } else {
                state.copy(
                    assets = listOf(newAsset) + state.assets,
                    selectedAsset = newAsset,
                    aiAnalysisResult = null,
                    latestChartBitmap = null
                )
            }
        }
        loadMarketData()
        fetchTradingViewRatings()
    }

    fun addDirectSymbol(rawSymbol: String, exchange: String = "BINANCE", type: com.example.data.model.AssetType = com.example.data.model.AssetType.CRYPTO) {
        val cleanSymbol = rawSymbol.trim().uppercase()
        if (cleanSymbol.isBlank()) return
        val prefix = exchange.trim().uppercase()
        val tvSymbol = if (cleanSymbol.contains(":")) cleanSymbol else "$prefix:$cleanSymbol"
        val symbolOnly = tvSymbol.substringAfter(":")
        val platform = com.example.data.model.ExchangePlatform.fromExchange(tvSymbol.substringBefore(":"))

        val newAsset = MarketAsset(
            symbol = symbolOnly,
            tvSymbol = tvSymbol,
            displayName = symbolOnly,
            name = "$cleanSymbol ($prefix)",
            type = type,
            platform = platform,
            currentPrice = 0.0,
            change24h = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            decimals = if (type == com.example.data.model.AssetType.FOREX && !symbolOnly.contains("JPY")) 4 else 2
        )

        _uiState.update { state ->
            val exists = state.assets.firstOrNull { it.tvSymbol == newAsset.tvSymbol }
            if (exists != null) {
                state.copy(selectedAsset = exists, aiAnalysisResult = null, latestChartBitmap = null)
            } else {
                state.copy(
                    assets = listOf(newAsset) + state.assets,
                    selectedAsset = newAsset,
                    aiAnalysisResult = null,
                    latestChartBitmap = null
                )
            }
        }
        loadMarketData()
        fetchTradingViewRatings()
    }

    fun selectAsset(asset: MarketAsset) {
        if (_uiState.value.selectedAsset.tvSymbol == asset.tvSymbol) return
        _uiState.update { it.copy(selectedAsset = asset, aiAnalysisResult = null, latestChartBitmap = null) }
        loadMarketData()
    }

    /**
     * Select a coin from the MEXC Screener / AI Selector:
     * - Configures the autonomous TradingBotManager to focus on this symbol
     * - Updates the active chart, indicators, and live market feed to track this coin
     */
    fun selectMexcCoin(ticker: Mexc24hTicker, customTpPct: Double? = null, customSlPct: Double? = null) {
        tradingBotManager.selectCoinForTrading(ticker.symbol, customTpPct, customSlPct)
        val existing = _uiState.value.assets.firstOrNull { it.symbol.equals(ticker.symbol, ignoreCase = true) }
        if (existing != null) {
            selectAsset(existing)
        } else {
            val mexcAsset = MarketAsset(
                symbol = ticker.symbol,
                tvSymbol = "MEXC:${ticker.symbol}",
                displayName = "${ticker.baseAsset}/USDT",
                name = "${ticker.baseAsset} Token",
                type = AssetType.CRYPTO,
                platform = ExchangePlatform.MEXC,
                currentPrice = if (ticker.lastPrice > 0) ticker.lastPrice else 1.0,
                change24h = ticker.priceChangePercent,
                high24h = ticker.highPrice,
                low24h = ticker.lowPrice,
                volume24h = ticker.volume,
                decimals = if (ticker.lastPrice < 0.01) 6 else if (ticker.lastPrice < 1.0) 4 else 2
            )
            _uiState.update {
                it.copy(
                    assets = listOf(mexcAsset) + it.assets,
                    selectedAsset = mexcAsset,
                    aiAnalysisResult = null,
                    latestChartBitmap = null
                )
            }
            loadMarketData()
        }
    }

    fun selectMexcCoinBySymbol(symbol: String) {
        val cleanSym = symbol.uppercase(Locale.US)
        val existing = _uiState.value.assets.firstOrNull { it.symbol.equals(cleanSym, ignoreCase = true) }
        if (existing != null) {
            tradingBotManager.selectCoinForTrading(cleanSym)
            selectAsset(existing)
        } else {
            viewModelScope.launch {
                val res = MexcApiClient().getSingle24hTicker(cleanSym)
                if (res.isSuccess) {
                    selectMexcCoin(res.getOrThrow())
                } else {
                    tradingBotManager.selectCoinForTrading(cleanSym)
                    val base = cleanSym.removeSuffix("USDT")
                    val mexcAsset = MarketAsset(
                        symbol = cleanSym,
                        tvSymbol = "MEXC:$cleanSym",
                        displayName = "$base/USDT",
                        name = "$base Token",
                        type = AssetType.CRYPTO,
                        platform = ExchangePlatform.MEXC,
                        currentPrice = _uiState.value.selectedAsset.currentPrice,
                        change24h = 0.0,
                        high24h = 0.0,
                        low24h = 0.0,
                        volume24h = 0.0
                    )
                    _uiState.update {
                        it.copy(
                            assets = listOf(mexcAsset) + it.assets,
                            selectedAsset = mexcAsset,
                            aiAnalysisResult = null,
                            latestChartBitmap = null
                        )
                    }
                    loadMarketData()
                }
            }
        }
    }

    fun selectTimeframe(timeframe: Timeframe) {
        if (_uiState.value.selectedTimeframe == timeframe) return
        _uiState.update { it.copy(selectedTimeframe = timeframe, aiAnalysisResult = null, latestChartBitmap = null) }
        loadMarketData()
    }

    fun loadMarketData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChartLoading = true, errorMessage = null) }
            val asset = _uiState.value.selectedAsset
            val tf = _uiState.value.selectedTimeframe

            try {
                // Fetch 100% genuine real-time candlestick data directly from exchange/broker
                val candles = fetcher.fetchCandles(asset, tf, limit = 85)
                val indicators = IndicatorCalculator.calculateAllIndicators(candles)

                val lastClose = candles.lastOrNull()?.close ?: asset.currentPrice
                val updatedAsset = asset.copy(currentPrice = lastClose, lastSyncTime = System.currentTimeMillis())

                _uiState.update { state ->
                    val updatedAssets = state.assets.map {
                        if (it.tvSymbol == asset.tvSymbol) updatedAsset else it
                    }
                    state.copy(
                        assets = updatedAssets,
                        selectedAsset = updatedAsset,
                        candles = candles,
                        indicators = indicators,
                        isChartLoading = false,
                        errorMessage = null,
                        isLiveFeedActive = true
                    )
                }

                // Check active alerts
                checkAlerts(updatedAsset.currentPrice, indicators)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isChartLoading = false,
                        isLiveFeedActive = false,
                        errorMessage = "Koneksi ke server ${asset.platform.title} terputus: ${e.localizedMessage ?: "Unknown error"}. Silakan periksa jaringan dan klik Refresh."
                    )
                }
            }
        }
    }

    private fun fetchTradingViewRatings() {
        viewModelScope.launch {
            try {
                val currentAssets = _uiState.value.assets
                val ratings = fetcher.fetchTradingViewScanner(currentAssets)
                if (ratings.isNotEmpty()) {
                    _uiState.update { state ->
                        val updated = state.assets.map { asset ->
                            val r = ratings[asset.tvSymbol]
                            if (r != null) asset.copy(tvRating = r) else asset
                        }
                        val selected = updated.firstOrNull { it.tvSymbol == state.selectedAsset.tvSymbol } ?: state.selectedAsset
                        state.copy(assets = updated, selectedAsset = selected)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun requestAiAnalysis() {
        val state = _uiState.value
        val candles = state.candles
        if (candles.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiAnalyzing = true) }

            // 1. Render high-resolution chart bitmap with indicators
            val chartBitmap = ChartImageRenderer.renderChartBitmap(
                asset = state.selectedAsset,
                timeframe = state.selectedTimeframe,
                candles = candles,
                indicators = state.indicators
            )

            // 2. Call Gemini API Multimodal Vision with custom API Key fallback
            val customApiKey = botState.value.customGeminiApiKey
            val result = geminiClient.analyzeChartWithGemini(
                asset = state.selectedAsset,
                timeframe = state.selectedTimeframe,
                candles = candles,
                indicators = state.indicators,
                chartBitmap = chartBitmap,
                customApiKey = customApiKey
            )

            // 3. Post live signal notification if it's an actionable signal
            val signal = LiveSignal(
                symbol = state.selectedAsset.displayName,
                platform = state.selectedAsset.platform.badgeLabel,
                action = result.action,
                price = state.selectedAsset.currentPrice,
                title = "${result.action.label} pada ${state.selectedAsset.displayName} [${state.selectedAsset.platform.badgeLabel}]",
                message = "${result.keySummary} (Keyakinan: ${result.confidence}%)",
                tvRating = state.selectedAsset.tvRating?.action?.label
            )

            notificationManager.showSignalNotification(signal)

            _uiState.update { current ->
                current.copy(
                    isAiAnalyzing = false,
                    aiAnalysisResult = result,
                    latestChartBitmap = chartBitmap,
                    triggeredSignals = listOf(signal) + current.triggeredSignals
                )
            }
        }
    }

    fun addAlert(alert: PriceAlert) {
        _uiState.update { it.copy(activeAlerts = it.activeAlerts + alert) }
    }

    fun deleteAlert(alertId: String) {
        _uiState.update { current ->
            current.copy(activeAlerts = current.activeAlerts.filterNot { it.id == alertId })
        }
    }

    private fun checkAlerts(currentPrice: Double, indicators: TechnicalIndicators) {
        val currentAlerts = _uiState.value.activeAlerts
        val triggered = mutableListOf<PriceAlert>()
        val newSignals = mutableListOf<LiveSignal>()

        for (alert in currentAlerts) {
            if (alert.symbol != _uiState.value.selectedAsset.symbol) continue

            val isConditionMet = when (alert.type) {
                AlertType.PRICE_ABOVE -> currentPrice >= alert.targetValue
                AlertType.PRICE_BELOW -> currentPrice <= alert.targetValue
                AlertType.RSI_OVERBOUGHT -> (indicators.currentRsi ?: 50.0) >= 70.0
                AlertType.RSI_OVERSOLD -> (indicators.currentRsi ?: 50.0) <= 30.0
                AlertType.MACD_BULLISH_CROSS -> (indicators.currentMacd?.histogram ?: 0.0) > 0.0
                AlertType.MACD_BEARISH_CROSS -> (indicators.currentMacd?.histogram ?: 0.0) < 0.0
            }

            if (isConditionMet) {
                triggered.add(alert)
                notificationManager.showPriceAlertNotification(
                    symbol = alert.symbol,
                    currentPrice = currentPrice,
                    conditionText = "${alert.type.name} target ${alert.targetValue}"
                )

                newSignals.add(
                    LiveSignal(
                        symbol = alert.symbol,
                        platform = _uiState.value.selectedAsset.platform.badgeLabel,
                        action = if (alert.type == AlertType.PRICE_ABOVE) SignalAction.BUY else SignalAction.SELL,
                        price = currentPrice,
                        title = "Target Alert Tersentuh!",
                        message = "${alert.symbol} menyentuh target ${alert.targetValue} (${alert.note})",
                        tvRating = _uiState.value.selectedAsset.tvRating?.action?.label
                    )
                )
            }
        }

        if (triggered.isNotEmpty()) {
            _uiState.update { current ->
                current.copy(
                    activeAlerts = current.activeAlerts.filterNot { triggered.contains(it) },
                    triggeredSignals = newSignals + current.triggeredSignals
                )
            }
        }
    }

    private var isRefreshingInProgress = false
    private var refreshCycleCounter = 0

    fun toggleAutoRefresh() {
        val newState = !_uiState.value.isAutoRefreshEnabled
        _uiState.update { 
            it.copy(
                isAutoRefreshEnabled = newState,
                refreshCountdown = if (newState) it.autoRefreshIntervalSeconds else it.refreshCountdown
            ) 
        }
    }

    fun setAutoRefreshInterval(seconds: Int) {
        val validSec = seconds.coerceIn(3, 60)
        _uiState.update { 
            it.copy(
                autoRefreshIntervalSeconds = validSec,
                refreshCountdown = validSec
            ) 
        }
    }

    fun triggerImmediateRefresh() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    refreshCountdown = it.autoRefreshIntervalSeconds,
                    isRefreshingPrice = true
                ) 
            }
            performAutoRefresh(fullCandleSync = true)
        }
    }

    private suspend fun performAutoRefresh(fullCandleSync: Boolean = false) {
        if (isRefreshingInProgress) return
        isRefreshingInProgress = true
        _uiState.update { it.copy(isRefreshingPrice = true) }

        try {
            val state = _uiState.value
            val asset = state.selectedAsset
            val tf = state.selectedTimeframe
            val currentCandles = state.candles

            // If initial candles are missing or periodic full sync is needed, fetch fresh candles in background
            if (currentCandles.isEmpty() || fullCandleSync || refreshCycleCounter % 4 == 0) {
                try {
                    val freshCandles = fetcher.fetchCandles(asset, tf, limit = 85)
                    if (freshCandles.isNotEmpty()) {
                        val indicators = IndicatorCalculator.calculateAllIndicators(freshCandles)
                        val lastClose = freshCandles.last().close
                        val updatedAsset = asset.copy(
                            currentPrice = lastClose,
                            lastSyncTime = System.currentTimeMillis()
                        )
                        _uiState.update { s ->
                            val updatedAssets = s.assets.map { if (it.tvSymbol == asset.tvSymbol) updatedAsset else it }
                            s.copy(
                                candles = freshCandles,
                                indicators = indicators,
                                selectedAsset = updatedAsset,
                                assets = updatedAssets,
                                isLiveFeedActive = true,
                                errorMessage = null
                            )
                        }
                        checkAlerts(lastClose, indicators)
                    }
                } catch (e: Exception) {
                    println("[AutoRefresh] Silent candle fetch error: ${e.message}")
                }
            }

            // Always update real-time price & 24h stats
            val priceUpdate = fetcher.fetchLiveRealPriceDetails(asset)
            val realPrice = priceUpdate.price

            if (realPrice > 0.0) {
                _uiState.update { s ->
                    val nowCandles = s.candles
                    val tfMinutes = s.selectedTimeframe.minutes
                    val tfDurationMs = tfMinutes * 60 * 1000L
                    val now = System.currentTimeMillis()

                    val updatedCandles = if (nowCandles.isNotEmpty()) {
                        val last = nowCandles.last()
                        // If the current candle time boundary has elapsed, append a new candle
                        if (tfDurationMs > 0 && (now - last.timestamp) >= tfDurationMs) {
                            val newCandle = CandleStick(
                                timestamp = last.timestamp + tfDurationMs,
                                open = last.close,
                                high = max(last.close, realPrice),
                                low = min(last.close, realPrice),
                                close = realPrice,
                                volume = 1.0
                            )
                            nowCandles + newCandle
                        } else {
                            val updatedCandle = last.copy(
                                close = realPrice,
                                high = max(last.high, realPrice),
                                low = min(last.low, realPrice)
                            )
                            nowCandles.dropLast(1) + updatedCandle
                        }
                    } else nowCandles

                    val updatedAsset = s.selectedAsset.copy(
                        currentPrice = realPrice,
                        change24h = priceUpdate.change24h ?: s.selectedAsset.change24h,
                        high24h = priceUpdate.high24h ?: max(s.selectedAsset.high24h, realPrice),
                        low24h = priceUpdate.low24h ?: min(s.selectedAsset.low24h, realPrice),
                        lastSyncTime = System.currentTimeMillis()
                    )

                    val newIndicators = if (updatedCandles.isNotEmpty()) {
                        IndicatorCalculator.calculateAllIndicators(updatedCandles)
                    } else s.indicators

                    val updatedAssets = s.assets.map { if (it.tvSymbol == asset.tvSymbol) updatedAsset else it }

                    s.copy(
                        candles = updatedCandles,
                        selectedAsset = updatedAsset,
                        assets = updatedAssets,
                        indicators = newIndicators,
                        isLiveFeedActive = true,
                        errorMessage = null,
                        lastRefreshedTimeMillis = System.currentTimeMillis()
                    )
                }
                checkAlerts(realPrice, _uiState.value.indicators)
                // Evaluate Trading Bot on each market tick
                tradingBotManager.evaluateMarketTick(
                    symbol = asset.symbol,
                    currentPrice = realPrice,
                    candles = _uiState.value.candles
                )
            }

            // Periodically refresh ratings from TradingView scanner
            if (refreshCycleCounter % 3 == 0) {
                try {
                    val ratings = fetcher.fetchTradingViewScanner(_uiState.value.assets)
                    if (ratings.isNotEmpty()) {
                        _uiState.update { s ->
                            val updatedList = s.assets.map { a ->
                                val r = ratings[a.tvSymbol]
                                if (r != null) a.copy(tvRating = r) else a
                            }
                            val curSelected = updatedList.firstOrNull { it.tvSymbol == s.selectedAsset.tvSymbol } ?: s.selectedAsset
                            s.copy(assets = updatedList, selectedAsset = curSelected)
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            println("[AutoRefresh] error: ${e.message}")
        } finally {
            _uiState.update { 
                it.copy(
                    isRefreshingPrice = false,
                    lastRefreshedTimeMillis = System.currentTimeMillis()
                ) 
            }
            isRefreshingInProgress = false
        }
    }

    private fun startRealPriceTicker() {
        priceTickerJob?.cancel()
        priceTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val state = _uiState.value
                if (!state.isAutoRefreshEnabled) {
                    continue
                }

                val currentCountdown = state.refreshCountdown
                if (currentCountdown > 1) {
                    _uiState.update { it.copy(refreshCountdown = currentCountdown - 1) }
                } else {
                    val interval = state.autoRefreshIntervalSeconds
                    _uiState.update { it.copy(refreshCountdown = interval) }
                    refreshCycleCounter++
                    launch {
                        performAutoRefresh(fullCandleSync = (refreshCycleCounter % 4 == 0))
                    }
                }
            }
        }
    }

    fun runMexcBacktest(symbol: String, interval: String = "15m", limit: Int = 250) {
        viewModelScope.launch {
            _isBacktesting.value = true
            try {
                // Fetch real historical candles from MEXC API
                val cleanSymbol = symbol.replace("/", "").replace(":", "").uppercase()
                val mexcKlinesResult = mexcClient.getKlines(cleanSymbol, interval, limit)
                val testCandles = if (mexcKlinesResult.isSuccess && mexcKlinesResult.getOrThrow().isNotEmpty()) {
                    mexcKlinesResult.getOrThrow()
                } else {
                    _uiState.value.candles
                }

                val currentBot = botState.value
                val report = BacktestEngine.runBacktest(
                    symbol = symbol,
                    candles = testCandles,
                    strategy = currentBot.strategy,
                    initialCapital = 1000.0,
                    stopLossPct = currentBot.customSlPct,
                    takeProfitPct = currentBot.customTpPct,
                    trailingPct = currentBot.customTrailingPct,
                    allocationPct = currentBot.tradeAllocationPct
                )
                _backtestReport.value = report
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isBacktesting.value = false
            }
        }
    }

    /**
     * Executes immediate on-demand market analysis and order placement using Gemini AI Trader.
     */
    fun executeGeminiTraderNow() {
        val state = _uiState.value
        val candles = state.candles
        if (candles.isEmpty()) return
        val currentPrice = state.selectedAsset.currentPrice
        val symbol = state.selectedAsset.symbol.replace("/", "").replace(":", "").uppercase()
        tradingBotManager.requestGeminiTraderExecution(
            symbol = symbol,
            currentPrice = currentPrice,
            candles = candles
        )
    }

    /**
     * Executes instant market buy with risk management trailing stop.
     */
    fun executeInstantBuyNow() {
        val state = _uiState.value
        val candles = state.candles
        if (candles.isEmpty()) return
        val currentPrice = state.selectedAsset.currentPrice
        val symbol = state.selectedAsset.symbol.replace("/", "").replace(":", "").uppercase()
        tradingBotManager.executeInstantManualBuy(
            symbol = symbol,
            currentPrice = currentPrice,
            candles = candles
        )
    }

    /**
     * Updates custom Gemini API key and clears quota limit lock.
     */
    fun updateGeminiApiKey(geminiKey: String) {
        tradingBotManager.setGeminiApiKey(geminiKey)
    }

    override fun onCleared() {
        super.onCleared()
        priceTickerJob?.cancel()
    }
}
