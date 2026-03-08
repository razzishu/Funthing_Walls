package com.aesthetic.funthingwalls

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aesthetic.funthingwalls.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Check if the user already saved an API key
        val sharedPreferences = getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
        val savedKey = sharedPreferences.getString("API_KEY", null)

        if (savedKey != null && savedKey.isNotEmpty()) {
            // Key exists! Skip this screen and go to the Main App
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Close the welcome screen
            return
        }

        // 2. If no key, show the Welcome Screen
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. Make the "Get API Key" button open the web browser
        binding.btnGetApiKey.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pexels.com/api/"))
            startActivity(browserIntent)
        }

        // 4. Make the "Unlock" button save the key
        binding.btnUnlockApp.setOnClickListener {
            val inputtedKey = binding.inputApiKey.text.toString().trim()

            if (inputtedKey.isNotEmpty()) {
                // Save the key permanently in the phone
                sharedPreferences.edit().putString("API_KEY", inputtedKey).apply()

                Toast.makeText(this, "App Unlocked!", Toast.LENGTH_SHORT).show()

                // Go to the main app
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please paste an API key first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}