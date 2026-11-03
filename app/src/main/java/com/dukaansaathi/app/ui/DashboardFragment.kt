package com.dukaansaathi.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.dukaansaathi.app.data.Repos
import com.dukaansaathi.app.databinding.FragmentDashboardBinding
import java.io.File

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val store = Repos.store
        binding.tvTodaySales.text = "Rs. " + String.format("%.2f", store.todaySalesTotal())
        binding.tvLowStock.text = store.lowStockItems().size.toString()
        binding.tvOutstanding.text = "Rs. " + String.format("%.2f", store.totalOutstanding())
        binding.tvItemCount.text = store.items.size.toString()

        val low = store.lowStockItems()
        binding.tvLowStockList.text = if (low.isEmpty()) {
            "All items are in stock."
        } else {
            "Low stock: " + low.joinToString(", ") { "${it.name} (${it.quantity} ${it.unit})" }
        }

        binding.btnExport.setOnClickListener { shareLedger() }
    }

    private fun shareLedger() {
        val dir = File(requireContext().filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "dukaan_ledger.txt")
        file.writeText(Repos.store.exportLedger())
        val uri = FileProvider.getUriForFile(requireContext(), "com.dukaansaathi.app.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DukaanSaathi ledger")
            putExtra(Intent.EXTRA_TEXT, "My shop ledger from DukaanSaathi")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share ledger"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
