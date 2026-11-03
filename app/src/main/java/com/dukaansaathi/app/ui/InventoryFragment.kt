package com.dukaansaathi.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dukaansaathi.app.R
import com.dukaansaathi.app.data.Item
import com.dukaansaathi.app.data.Repos
import com.dukaansaathi.app.databinding.FragmentInventoryBinding
import com.dukaansaathi.app.databinding.ItemInventoryBinding
import com.dukaansaathi.app.util.VoiceCapableFragment

class InventoryFragment : VoiceCapableFragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ItemAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ItemAdapter { showItemActions(it) }
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter
        binding.btnAddItem.setOnClickListener { showAddDialog() }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.submit(Repos.store.items.sortedWith(compareBy<Item> { it.isLowStock().not() }.thenBy { it.name }))
    }

    private fun showAddDialog(editing: Item? = null) {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val nameRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val etName = EditText(ctx).apply {
            hint = "Item name (e.g. Sugar)"
            setText(editing?.name ?: "")
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnMic = ImageButton(ctx).apply {
            setImageResource(R.drawable.ic_mic)
            setOnClickListener { askVoice(getString(R.string.voice_prompt_item)) { etName.setText(it) } }
        }
        nameRow.addView(etName)
        nameRow.addView(btnMic)
        val etQty = EditText(ctx).apply { hint = "Quantity"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(editing?.quantity?.toString() ?: "") }
        val etUnit = EditText(ctx).apply { hint = "Unit (kg / pcs / litre)"; setText(editing?.unit ?: "pcs") }
        val etLow = EditText(ctx).apply { hint = "Low stock alert at"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(editing?.lowStockAt?.toString() ?: "5") }
        val etPrice = EditText(ctx).apply { hint = "Price per unit (Rs.)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; setText(editing?.price?.toString() ?: "") }
        layout.addView(nameRow); layout.addView(etQty); layout.addView(etUnit); layout.addView(etLow); layout.addView(etPrice)

        AlertDialog.Builder(ctx)
            .setTitle(if (editing == null) "Add stock item" else "Edit item")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val qty = etQty.text.toString().toIntOrNull() ?: 0
                val low = etLow.text.toString().toIntOrNull() ?: 5
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                if (name.isBlank()) { toast("Enter item name"); return@setPositiveButton }
                if (editing == null) {
                    Repos.store.addItem(name, qty, etUnit.text.toString().trim().ifBlank { "pcs" }, low, price)
                } else {
                    editing.name = name; editing.quantity = qty
                    editing.unit = etUnit.text.toString().trim().ifBlank { "pcs" }
                    editing.lowStockAt = low; editing.price = price
                    Repos.store.updateItem(editing)
                }
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showItemActions(item: Item) {
        val options = arrayOf("Sell 1 ${item.unit}", "Sell custom quantity", "Edit item", "Delete item")
        AlertDialog.Builder(requireContext())
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sell(item, 1)
                    1 -> showSellDialog(item)
                    2 -> showAddDialog(item)
                    3 -> { Repos.store.deleteItem(item.id); refresh() }
                }
            }
            .show()
    }

    private fun showSellDialog(item: Item) {
        val et = EditText(requireContext()).apply {
            hint = "Quantity to sell"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Sell ${item.name}")
            .setView(et)
            .setPositiveButton("Sell") { _, _ ->
                val qty = et.text.toString().toIntOrNull() ?: 0
                if (qty > 0) sell(item, qty) else toast("Enter valid quantity")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sell(item: Item, qty: Int) {
        if (qty > item.quantity) { toast("Not enough stock!"); return }
        item.quantity -= qty
        Repos.store.updateItem(item)
        Repos.store.addSale(item.name, qty, qty * item.price)
        toast("Sold $qty ${item.unit} of ${item.name}")
        refresh()
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ItemAdapter(private val onClick: (Item) -> Unit) :
        RecyclerView.Adapter<ItemAdapter.VH>() {

        private var data: List<Item> = emptyList()
        fun submit(list: List<Item>) { data = list; notifyDataSetChanged() }

        inner class VH(val b: ItemInventoryBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = data[position]
            holder.b.tvItemName.text = item.name
            holder.b.tvItemQty.text = "${item.quantity} ${item.unit} in stock"
            holder.b.tvItemPrice.text = "Rs. ${item.price}/${item.unit}"
            holder.b.tvLowBadge.visibility = if (item.isLowStock()) View.VISIBLE else View.GONE
            holder.b.root.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = data.size
    }
}
