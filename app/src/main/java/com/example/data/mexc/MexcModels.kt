package com.example.data.mexc

data class MexcServerTime(
    val serverTime: Long
)

data class MexcBalance(
    val asset: String,
    val free: Double,
    val locked: Double
) {
    val total: Double get() = free + locked
}

data class MexcAccountInfo(
    val makerCommission: Int = 10,
    val takerCommission: Int = 10,
    val canTrade: Boolean = true,
    val canWithdraw: Boolean = false,
    val canDeposit: Boolean = false,
    val updateTime: Long = 0L,
    val balances: List<MexcBalance> = emptyList()
) {
    fun getUsdtFree(): Double = balances.find { it.asset.equals("USDT", ignoreCase = true) }?.free ?: 0.0
    fun getAssetFree(asset: String): Double = balances.find { it.asset.equals(asset, ignoreCase = true) }?.free ?: 0.0
}

data class MexcOrderResponse(
    val symbol: String,
    val orderId: String,
    val orderListId: Long = -1,
    val clientOrderId: String? = null,
    val transactTime: Long = System.currentTimeMillis(),
    val price: Double = 0.0,
    val origQty: Double = 0.0,
    val executedQty: Double = 0.0,
    val status: String = "FILLED",
    val timeInForce: String = "GTC",
    val type: String = "MARKET",
    val side: String = "BUY",
    val isTestOrder: Boolean = false
)

data class MexcTickerPrice(
    val symbol: String,
    val price: Double
)

data class MexcApiError(
    val code: Int,
    override val message: String
) : Exception("MEXC API Error ($code): $message")
