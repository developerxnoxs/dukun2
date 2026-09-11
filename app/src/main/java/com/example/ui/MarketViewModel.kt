package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.calculator.IndicatorCalculator
import com.example.data.fetcher.MarketDataFetcher
import com.example.data.gemini.ChartImageRenderer
import com.example.data.gemini.GeminiAnalystClient
import com.example.data.model.AlertType
import com.example.data.model.AiAnalysisResult
import com.example.data.model.CandleStick
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
    val isLiveFeedActive: Boolean = true
)

class MarketViewModel(application: Application) : AndroidViewModel(application) {

    private val fetcher = MarketDataFetcher()
    private val geminiClient = GeminiAnalystClient()
    private val notificationManager = SignalNotificationManager(application)

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private var priceTickerJob: Job? = null

    init {
        loadMarketData()
        fetchTradingViewRatings()
        startRealPriceTicker()
    }

    fun selectAsset(asset: MarketAsset) {
        if (_uiState.value.selectedAsset.symbol == asset.symbol) return
        _uiState.update { it.copy(selectedAsset = asset, aiAnalysisResult = null, latestChartBitmap = null) }
        loadMarketData()
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
                        if (it.symbol == asset.symbol) updatedAsset else it
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
                        val selected = updated.firstOrNull { it.symbol == state.selectedAsset.symbol } ?: state.selectedAsset
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

            // 2. Call Gemini API Multimodal Vision
            val result = geminiClient.analyzeChartWithGemini(
                asset = state.selectedAsset,
                timeframe = state.selectedTimeframe,
                candles = candles,
                indicators = state.indicators,
                chartBitmap = chartBitmap
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

    private fun startRealPriceTicker() {
        priceTickerJob?.cancel()
        priceTickerJob = viewModelScope.launch {
            var cycleCount = 0
            while (isActive) {
                delay(3500) // Poll real live price from server every 3.5 seconds
                cycleCount++

                val state = _uiState.value
                val asset = state.selectedAsset
                val candles = state.candles

                try {
                    // 1. Fetch REAL live price from exchange
                    val realPrice = fetcher.fetchLiveRealPrice(asset)

                    if (candles.isNotEmpty()) {
                        val last = candles.last()
                        val updatedCandle = last.copy(
                            close = realPrice,
                            high = max(last.high, realPrice),
                            low = min(last.low, realPrice)
                        )

                        val updatedCandles = candles.dropLast(1) + updatedCandle
                        val updatedAsset = asset.copy(
                            currentPrice = realPrice,
                            lastSyncTime = System.currentTimeMillis()
                        )
                        val updatedIndicators = IndicatorCalculator.calculateAllIndicators(updatedCandles)

                        _uiState.update { s ->
                            s.copy(
                                candles = updatedCandles,
                                selectedAsset = updatedAsset,
                                indicators = updatedIndicators,
                                isLiveFeedActive = true,
                                assets = s.assets.map { if (it.symbol == asset.symbol) updatedAsset else it }
                            )
                        }

                        checkAlerts(realPrice, updatedIndicators)
                    }

                    // 2. Every ~14 seconds, refresh TradingView ratings in background
                    if (cycleCount % 4 == 0) {
                        val ratings = fetcher.fetchTradingViewScanner(state.assets)
                        if (ratings.isNotEmpty()) {
                            _uiState.update { s ->
                                val updatedList = s.assets.map { a ->
                                    val r = ratings[a.tvSymbol]
                                    if (r != null) a.copy(tvRating = r) else a
                                }
                                val curSelected = updatedList.firstOrNull { it.symbol == s.selectedAsset.symbol } ?: s.selectedAsset
                                s.copy(assets = updatedList, selectedAsset = curSelected)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // In case of momentary network glitch, do NOT fabricate data
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        priceTickerJob?.cancel()
    }
}
