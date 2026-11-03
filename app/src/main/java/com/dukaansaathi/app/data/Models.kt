package com.dukaansaathi.app.data

data class Item(
    val id: Long,
    var name: String,
    var quantity: Int,
    var unit: String,
    var lowStockAt: Int,
    var price: Double
) {
    fun isLowStock(): Boolean = quantity <= lowStockAt
}

data class CreditEntry(
    val id: Long,
    var customer: String,
    var amount: Double,
    var note: String,
    var type: String, // "GIVEN" (udhaar diya) or "RECEIVED" (payment mila)
    var timestamp: Long
)

data class Sale(
    val id: Long,
    var itemName: String,
    var qty: Int,
    var total: Double,
    var timestamp: Long
)
