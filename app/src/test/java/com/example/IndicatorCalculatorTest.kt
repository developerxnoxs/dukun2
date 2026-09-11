package com.example

import com.example.data.calculator.IndicatorCalculator
import com.example.data.model.CandleStick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorCalculatorTest {

    @Test
    fun testIndicatorCalculation() {
        val candles = mutableListOf<CandleStick>()
        var price = 100.0
        val now = System.currentTimeMillis()

        for (i in 0 until 40) {
            price += if (i % 2 == 0) 1.5 else -0.8
            candles.add(
                CandleStick(
                    timestamp = now - (40 - i) * 60000L,
                    open = price - 0.5,
                    high = price + 1.0,
                    low = price - 1.0,
                    close = price,
                    volume = 1000.0
                )
            )
        }

        val indicators = IndicatorCalculator.calculateAllIndicators(candles)

        // Verify indicators computed
        assertEquals(40, indicators.ema9.size)
        assertEquals(40, indicators.ema21.size)
        assertEquals(40, indicators.bollingerBands.size)
        assertEquals(40, indicators.rsi14.size)
        assertEquals(40, indicators.macd.size)

        assertNotNull(indicators.currentRsi)
        assertTrue(indicators.currentRsi!! in 0.0..100.0)
        assertTrue(indicators.supportLevel1 < indicators.resistanceLevel1)
    }
}
