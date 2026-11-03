package com.dukaansaathi.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dukaansaathi.app.data.Repos
import com.dukaansaathi.app.databinding.ActivityMainBinding
import com.dukaansaathi.app.ui.CreditFragment
import com.dukaansaathi.app.ui.DashboardFragment
import com.dukaansaathi.app.ui.InventoryFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repos.init(applicationContext)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> show(DashboardFragment())
                R.id.nav_inventory -> show(InventoryFragment())
                R.id.nav_credit -> show(CreditFragment())
            }
            true
        }
        if (savedInstanceState == null) show(DashboardFragment())
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
