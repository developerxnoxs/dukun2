package com.example.data.bot

import com.example.data.model.SignalAction
import com.example.data.model.TechnicalIndicators

/**
 * Trade Blueprint produced by Gemini AI analysis.
 * Acts as a local trigger anchor so the bot doesn't spam the Gemini API.
 */
data class GeminiTradeBlueprint(
    val symbol: String,
    val initialAnalysisPrice: Double,
    val triggerAction: SignalAction, // BUY, SELL, or STANDBY/NEUTRAL
    val triggerCondition: String, // e.g., "PRICE_PULLBACK_SUPPORT", "PRICE_BREAKOUT", "IMMEDIATE"
    val triggerMinPrice: Double, // lower bound for trigger
    val triggerMaxPrice: Double, // upper bound for trigger
    val triggerRsiMin: Double = 30.0,
    val triggerRsiMax: Double = 70.0,
    val suggestedStopLoss: Double,
    val suggestedTakeProfit: Double,
    val confidence: Double,
    val thesis: String,
    val validityTtlMinutes: Int = 20, // valid for 20 minutes locally without API calls
    val createdAtTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if this plan has expired locally.
     */
    fun isExpired(): Boolean {
        val ageMs = System.currentTimeMillis() - createdAtTimestamp
        return ageMs > (validityTtlMinutes * 60 * 1000L)
    }

    /**
     * Sisa waktu berlaku rencana dalam menit/detik.
     */
    fun getRemainingSeconds(): Long {
        val totalMs = validityTtlMinutes * 60 * 1000L
        val ageMs = System.currentTimeMillis() - createdAtTimestamp
        return if (totalMs > ageMs) (totalMs - ageMs) / 1000L else 0L
    }

    /**
     * Evaluasi apakah kondisi harga dan teknikal live saat ini menyentuh level pemicu.
     */
    fun isTriggerConditionMet(currentPrice: Double, indicators: TechnicalIndicators): Boolean {
        if (isExpired()) return false
        if (triggerAction == SignalAction.NEUTRAL) return false

        // Evaluasi range harga pemicu
        val isPriceInRange = if (triggerMinPrice > 0 && triggerMaxPrice > 0) {
            currentPrice in (triggerMinPrice * 0.998)..(triggerMaxPrice * 1.002)
        } else if (triggerMinPrice > 0) {
            currentPrice >= triggerMinPrice
        } else if (triggerMaxPrice > 0) {
            currentPrice <= triggerMaxPrice
        } else {
            true
        }

        // Evaluasi filter RSI
        val currentRsi = indicators.currentRsi ?: 50.0
        val isRsiValid = currentRsi in triggerRsiMin..triggerRsiMax

        return isPriceInRange && isRsiValid
    }
}
