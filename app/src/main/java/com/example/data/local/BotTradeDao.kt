package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BotTradeDao {

    @Query("SELECT * FROM bot_trades ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<BotTradeEntity>>

    @Query("SELECT * FROM bot_trades WHERE isSandbox = :isSandbox ORDER BY timestamp DESC")
    fun getTradesByMode(isSandbox: Boolean): Flow<List<BotTradeEntity>>

    @Query("SELECT * FROM bot_trades ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTrades(limit: Int = 50): List<BotTradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: BotTradeEntity): Long

    @Query("DELETE FROM bot_trades WHERE isSandbox = :isSandbox")
    suspend fun clearTradesByMode(isSandbox: Boolean)

    @Query("DELETE FROM bot_trades")
    suspend fun clearAllTrades()
}
