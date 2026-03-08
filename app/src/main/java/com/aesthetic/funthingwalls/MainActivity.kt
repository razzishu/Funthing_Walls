package com.aesthetic.funthingwalls

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aesthetic.funthingwalls.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Load the "Collections" screen by default when the app opens
        if (savedInstanceState == null) {
            loadFragment(CollectionsFragment())
        }

        // 2. Listen for taps on the Bottom Navigation Bar
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_collections -> {
                    loadFragment(CollectionsFragment())
                    true
                }
                R.id.nav_funthing -> {
                    loadFragment(FunthingFragment())
                    true
                }
                R.id.nav_local -> {
                    loadFragment(LocalFragment())
                    true
                }
                R.id.nav_live -> {
                    loadFragment(LiveFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
        // Put this right below binding = ActivityMainBinding.inflate(...)
        binding.bottomNavigation.itemIconTintList = null
    }

    // 3. The function that swaps the "Rooms" out
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}