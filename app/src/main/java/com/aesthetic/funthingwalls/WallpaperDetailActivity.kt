package com.aesthetic.funthingwalls

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aesthetic.funthingwalls.databinding.ActivityWallpaperDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WallpaperDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra("IMAGE_URL")

        binding.fullScreenImageView.load(imageUrl) {
            crossfade(true)
        }

        binding.btnClose.setOnClickListener { finish() }

        binding.btnApplyWallpaper.setOnClickListener {
            if (imageUrl != null) applyWallpaper(imageUrl)
        }
    }

    private fun applyWallpaper(url: String) {
        Toast.makeText(this, "Applying...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val loader = ImageLoader(this@WallpaperDetailActivity)
                val req = ImageRequest.Builder(this@WallpaperDetailActivity)
                    .data(url)
                    .allowHardware(false) // Safe bitmap unlock
                    .build()

                val result = (loader.execute(req) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap

                if (bitmap != null) {
                    WallpaperManager.getInstance(this@WallpaperDetailActivity).setBitmap(bitmap)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@WallpaperDetailActivity, "Success!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WallpaperDetailActivity, "Failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}