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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dukaansaathi.app.R
import com.dukaansaathi.app.data.CreditEntry
import com.dukaansaathi.app.data.Repos
import com.dukaansaathi.app.databinding.FragmentCreditBinding
import com.dukaansaathi.app.databinding.ItemCreditBinding
import com.dukaansaathi.app.util.VoiceCapableFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreditFragment : VoiceCapableFragment() {

    private var _binding: FragmentCreditBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CreditAdapter
    private val dateFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = CreditAdapter()
        binding.rvCredits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCredits.adapter = adapter
        binding.btnGiven.setOnClickListener { showEntryDialog("GIVEN") }
        binding.btnReceived.setOnClickListener { showEntryDialog("RECEIVED") }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val store = Repos.store
        val outstanding = store.outstandingByCustomer()
        binding.tvOutstandingSummary.text = if (outstanding.isEmpty()) {
            "No pending udhaar. Total outstanding: Rs. 0"
        } else {
            "Total outstanding: Rs. " + String.format("%.2f", store.totalOutstanding()) +
                "\n" + outstanding.entries.joinToString("\n") { "${it.key}: Rs. ${String.format("%.2f", it.value)}" }
        }
        adapter.submit(store.credits.sortedByDescending { it.timestamp })
    }

    private fun showEntryDialog(type: String) {
        val ctx = requireContext()
        val title = if (type == "GIVEN") "Udhar diya (credit given)" else "Payment received"
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 0) }

        val nameRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val etCustomer = EditText(ctx).apply {
            hint = "Customer name"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnMicName = ImageButton(ctx).apply {
            setImageResource(R.drawable.ic_mic)
            setOnClickListener { askVoice(getString(R.string.voice_prompt_customer)) { etCustomer.setText(it) } }
        }
        nameRow.addView(etCustomer); nameRow.addView(btnMicName)

        val etAmount = EditText(ctx).apply {
            hint = "Amount (Rs.)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val noteRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val etNote = EditText(ctx).apply {
            hint = "Note (optional)"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnMicNote = ImageButton(ctx).apply {
            setImageResource(R.drawable.ic_mic)
            setOnClickListener { askVoice(getString(R.string.voice_prompt_note)) { etNote.setText(it) } }
        }
        noteRow.addView(etNote); noteRow.addView(btnMicNote)

        layout.addView(nameRow); layout.addView(etAmount); layout.addView(noteRow)

        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val customer = etCustomer.text.toString().trim()
                val amount = etAmount.text.toString().toDoubleOrNull()
                if (customer.isBlank()) { toast("Enter customer name"); return@setPositiveButton }
                if (amount == null || amount <= 0) { toast("Enter valid amount"); return@setPositiveButton }
                Repos.store.addCredit(customer, amount, etNote.text.toString(), type)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class CreditAdapter : RecyclerView.Adapter<CreditAdapter.VH>() {
        private var data: List<CreditEntry> = emptyList()
        fun submit(list: List<CreditEntry>) { data = list; notifyDataSetChanged() }

        inner class VH(val b: ItemCreditBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemCreditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val c = data[position]
            val given = c.type == "GIVEN"
            holder.b.tvCustomer.text = c.customer + if (given) "  (udhaar)" else "  (paid)"
            holder.b.tvNote.text = c.note
            holder.b.tvDate.text = dateFmt.format(Date(c.timestamp))
            holder.b.tvAmount.text = (if (given) "+" else "-") + " Rs. " + String.format("%.2f", c.amount)
            val color = ContextCompat.getColor(requireContext(), if (given) R.color.low_stock else R.color.primary)
            holder.b.tvAmount.setTextColor(color)
        }

        override fun getItemCount(): Int = data.size
    }
}
