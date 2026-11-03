package com.dukaansaathi.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class StoreRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("dukaansaathi", Context.MODE_PRIVATE)

    var items: MutableList<Item> = loadItems()
        private set
    var credits: MutableList<CreditEntry> = loadCredits()
        private set
    var sales: MutableList<Sale> = loadSales()
        private set

    private fun nextId(key: String): Long {
        val id = prefs.getLong(key, 1L)
        prefs.edit().putLong(key, id + 1).apply()
        return id
    }

    // ---------- Items ----------
    fun addItem(name: String, quantity: Int, unit: String, lowStockAt: Int, price: Double): Item {
        val item = Item(nextId("id_item"), name, quantity, unit, lowStockAt, price)
        items.add(item)
        saveItems()
        return item
    }

    fun updateItem(item: Item) { saveItems() }

    fun deleteItem(id: Long) {
        items.removeAll { it.id == id }
        saveItems()
    }

    fun findItem(id: Long): Item? = items.firstOrNull { it.id == id }

    // ---------- Credit ----------
    fun addCredit(customer: String, amount: Double, note: String, type: String): CreditEntry {
        val entry = CreditEntry(nextId("id_credit"), customer.trim(), amount, note.trim(), type, System.currentTimeMillis())
        credits.add(entry)
        saveCredits()
        return entry
    }

    fun outstandingByCustomer(): Map<String, Double> {
        val map = linkedMapOf<String, Double>()
        for (c in credits) {
            val key = c.customer.ifBlank { "Unknown" }
            val delta = if (c.type == "GIVEN") c.amount else -c.amount
            map[key] = (map[key] ?: 0.0) + delta
        }
        return map.filterValues { it > 0.005 }
    }

    fun totalOutstanding(): Double = outstandingByCustomer().values.sum()

    // ---------- Sales ----------
    fun addSale(itemName: String, qty: Int, total: Double): Sale {
        val sale = Sale(nextId("id_sale"), itemName, qty, total, System.currentTimeMillis())
        sales.add(sale)
        saveSales()
        return sale
    }

    fun todaySalesTotal(): Double {
        val start = startOfToday()
        return sales.filter { it.timestamp >= start }.sumOf { it.total }
    }

    private fun startOfToday(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun lowStockItems(): List<Item> = items.filter { it.isLowStock() }

    // ---------- Persistence (org.json, no external deps) ----------
    private fun saveItems() {
        val arr = JSONArray()
        for (i in items) {
            arr.put(JSONObject().apply {
                put("id", i.id); put("name", i.name); put("quantity", i.quantity)
                put("unit", i.unit); put("lowStockAt", i.lowStockAt); put("price", i.price)
            })
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    private fun loadItems(): MutableList<Item> {
        val arr = JSONArray(prefs.getString("items", "[]")!!)
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            Item(o.getLong("id"), o.getString("name"), o.getInt("quantity"),
                o.getString("unit"), o.getInt("lowStockAt"), o.getDouble("price"))
        }.toMutableList()
    }

    private fun saveCredits() {
        val arr = JSONArray()
        for (c in credits) {
            arr.put(JSONObject().apply {
                put("id", c.id); put("customer", c.customer); put("amount", c.amount)
                put("note", c.note); put("type", c.type); put("timestamp", c.timestamp)
            })
        }
        prefs.edit().putString("credits", arr.toString()).apply()
    }

    private fun loadCredits(): MutableList<CreditEntry> {
        val arr = JSONArray(prefs.getString("credits", "[]")!!)
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            CreditEntry(o.getLong("id"), o.getString("customer"), o.getDouble("amount"),
                o.getString("note"), o.getString("type"), o.getLong("timestamp"))
        }.toMutableList()
    }

    private fun saveSales() {
        val arr = JSONArray()
        for (s in sales) {
            arr.put(JSONObject().apply {
                put("id", s.id); put("itemName", s.itemName); put("qty", s.qty)
                put("total", s.total); put("timestamp", s.timestamp)
            })
        }
        prefs.edit().putString("sales", arr.toString()).apply()
    }

    private fun loadSales(): MutableList<Sale> {
        val arr = JSONArray(prefs.getString("sales", "[]")!!)
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            Sale(o.getLong("id"), o.getString("itemName"), o.getInt("qty"),
                o.getDouble("total"), o.getLong("timestamp"))
        }.toMutableList()
    }

    fun exportLedger(): String {
        val sb = StringBuilder()
        val fmt = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        sb.append("=== DukaanSaathi Ledger ===\n\n")
        sb.append("-- Today's sales: Rs. ").append(String.format("%.2f", todaySalesTotal())).append(" --\n\n")
        sb.append("INVENTORY\n")
        for (i in items) sb.append("• ${i.name} — ${i.quantity} ${i.unit} @ Rs.${i.price} ${if (i.isLowStock()) "[LOW STOCK]" else ""}\n")
        sb.append("\nCREDIT BOOK (udhaar)\n")
        for (c in credits.sortedByDescending { it.timestamp }) {
            sb.append("• ${fmt.format(java.util.Date(c.timestamp))} — ${c.customer}: ")
                .append(if (c.type == "GIVEN") "+" else "-").append("Rs.").append(String.format("%.2f", c.amount))
                .append(if (c.note.isNotBlank()) " (${c.note})" else "").append("\n")
        }
        sb.append("\nTotal outstanding: Rs. ").append(String.format("%.2f", totalOutstanding())).append("\n")
        return sb.toString()
    }
}

object Repos {
    lateinit var store: StoreRepository
        private set
    fun init(context: Context) {
        if (!::store.isInitialized) store = StoreRepository(context)
    }
}
