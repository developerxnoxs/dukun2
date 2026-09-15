package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bot_trades")
data class BotTradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val symbol: String,
    val side: String, // BUY / SELL
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val pnlUsdt: Double,
    val pnlPercent: Double,
    val exitReason: String, // TP_HIT, SL_HIT, TRAILING_STOP, SIGNAL_REVERSAL, MANUAL_CLOSE
    val isSandbox: Boolean = true,
    val strategyName: String = "Confluence",
    val orderId: String = ""
)
