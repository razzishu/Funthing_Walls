package com.aesthetic.funthingwalls

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.aesthetic.funthingwalls.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    // A list of beautiful, modern hex colors
    private val dynamicColors = listOf(
        "#FF5252", "#E040FB", "#7C4DFF", "#536DFE", "#448AFF",
        "#18FFFF", "#64FFDA", "#69F0AE", "#B2FF59", "#EEFF41", "#FFAB40"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Randomize the text color on every single boot!
        val randomColor = Color.parseColor(dynamicColors.random())
        binding.appNameText.setTextColor(randomColor)

        // 2. Animate the text smoothly (Fade in + Scale up)
        binding.appNameText.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1200)
            .withEndAction {
                // 3. When animation finishes, check where to route the user
                routeUser()
            }
            .start()
    }

    private fun routeUser() {
        val prefs = getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("API_KEY", "")

        val intent = if (apiKey.isNullOrEmpty()) {
            Intent(this, WelcomeActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish() // Close the splash screen so they can't hit "Back" to return to it
    }
}