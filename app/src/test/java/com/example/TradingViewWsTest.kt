package com.example

import com.example.data.fetcher.XnoxsTradingViewFetcher
import com.example.data.model.Timeframe
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradingViewWsTest {

    private val fetcher = XnoxsTradingViewFetcher()

    @Test
    fun testBtcTradingViewWs() = runBlocking {
        println("[TEST XNOXS] Fetching BINANCE:BTCUSDT...")
        val result = fetcher.getHistoricalData("BINANCE:BTCUSDT", Timeframe.M15, barsCount = 15)
        println("[TEST XNOXS] Candles count: ${result.candles.size}")
        assertFalse("Candles should not be empty", result.candles.isEmpty())
        val last = result.candles.last()
        println("[TEST XNOXS] Last BTC candle: close=${last.close}, open=${last.open}, high=${last.high}, low=${last.low}")
        assertTrue("BTC Price should be > 1000", last.close > 1000.0)
    }

    @Test
    fun testForexTradingViewWs() = runBlocking {
        println("[TEST XNOXS] Fetching OANDA:EURUSD...")
        val result = fetcher.getHistoricalData("OANDA:EURUSD", Timeframe.H1, barsCount = 15)
        println("[TEST XNOXS] EURUSD Candles count: ${result.candles.size}")
        assertFalse("Candles should not be empty", result.candles.isEmpty())
        val last = result.candles.last()
        println("[TEST XNOXS] Last EURUSD candle: close=${last.close}, open=${last.open}")
        assertTrue("EURUSD Price should be ~1.0-1.3", last.close in 0.8..1.8)
    }

    @Test
    fun testGoldTradingViewWs() = runBlocking {
        println("[TEST XNOXS] Fetching OANDA:XAUUSD...")
        val result = fetcher.getHistoricalData("OANDA:XAUUSD", Timeframe.H4, barsCount = 15)
        println("[TEST XNOXS] Gold Candles count: ${result.candles.size}")
        assertFalse("Candles should not be empty", result.candles.isEmpty())
        val last = result.candles.last()
        println("[TEST XNOXS] Last Gold candle: close=${last.close}")
        assertTrue("Gold price should be > 1000", last.close > 1000.0)
    }

    @Test
    fun test1MinuteAnd5MinuteTimeframes() = runBlocking {
        println("[TEST 1M & 5M] Testing M1 and M5 timeframes...")
        val m1Result = fetcher.getHistoricalData("BINANCE:BTCUSDT", Timeframe.M1, barsCount = 10)
        println("[TEST 1M] Candles: ${m1Result.candles.size}")
        assertFalse("M1 candles should not be empty", m1Result.candles.isEmpty())

        val m5Result = fetcher.getHistoricalData("BINANCE:ETHUSDT", Timeframe.M5, barsCount = 10)
        println("[TEST 5M] Candles: ${m5Result.candles.size}")
        assertFalse("M5 candles should not be empty", m5Result.candles.isEmpty())
    }
}

