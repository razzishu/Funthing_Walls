package com.aesthetic.funthingwalls

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aesthetic.funthingwalls.databinding.FragmentFunthingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

class FunthingFragment : Fragment() {

    private var _binding: FragmentFunthingBinding? = null
    private val binding get() = _binding!!

    private var exactLockBitmap: Bitmap? = null
    private var exactHomeBitmap: Bitmap? = null

    // Memory trackers
    private var lockImageUrl: String? = null
    private var homeImageUrl: String? = null
    private var currentAutoQuery: String = ""

    private val masterAesthetics = listOf(
        "Cyberpunk City", "Minimalist Nature", "Neon Lights", "Dark Academia",
        "Vaporwave", "Deep Space", "Abstract Fluid", "Vintage Aesthetic",
        "Macro Photography", "Moody Forest", "Pastel Sky", "Retrowave Cars",
        "Golden Hour Architecture", "Ocean Waves", "Monochrome Minimalist"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFunthingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels
        val exactPhoneRatio = "$screenW:$screenH"

        (binding.lockScreenPreview.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = exactPhoneRatio
        (binding.homeScreenPreview.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = exactPhoneRatio

        val prefs = requireActivity().getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
        lockImageUrl = prefs.getString("CURRENT_LOCK_URL", null)
        homeImageUrl = prefs.getString("CURRENT_HOME_URL", null)

        if (lockImageUrl != null && homeImageUrl != null) {
            binding.lockScreenPreview.load(lockImageUrl) { crossfade(true) }
            binding.homeScreenPreview.load(homeImageUrl) { crossfade(true) }
        }

        binding.btnFetchRandomPair.setOnClickListener {
            val query = binding.searchTheme.text.toString().trim()
            if (query.isNotEmpty()) {
                currentAutoQuery = ""
                Toast.makeText(requireContext(), "Brewing custom pair...", Toast.LENGTH_SHORT).show()
                fetchAndPreCropWallpapers(query, screenW, screenH)
            } else {
                Toast.makeText(requireContext(), "Please type a theme first!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSurpriseMe.setOnClickListener { runAutoMagicEngine(screenW, screenH) }

        binding.btnApplyPair.setOnClickListener {
            if (exactLockBitmap != null && exactHomeBitmap != null) {
                applyExactBitmaps(screenW, screenH)
            }
        }
    }

    private fun runAutoMagicEngine(screenW: Int, screenH: Int) {
        val prefs = requireActivity().getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
        val disliked = prefs.getStringSet("DISLIKED_THEMES", mutableSetOf()) ?: mutableSetOf()
        val liked = prefs.getStringSet("LIKED_THEMES", mutableSetOf()) ?: mutableSetOf()

        if (currentAutoQuery.isNotEmpty()) {
            disliked.add(currentAutoQuery)
            prefs.edit().putStringSet("DISLIKED_THEMES", disliked).apply()
        }

        var newTheme = masterAesthetics.random()
        var attempts = 0
        while (disliked.contains(newTheme) && attempts < 15) {
            newTheme = masterAesthetics.random()
            attempts++
        }

        if (liked.isNotEmpty() && (1..5).random() == 1) newTheme = liked.random()

        currentAutoQuery = newTheme
        binding.searchTheme.setText(newTheme)
        Toast.makeText(requireContext(), "✨ Magic Search: $newTheme", Toast.LENGTH_SHORT).show()

        fetchAndPreCropWallpapers(newTheme, screenW, screenH)
    }

    private fun centerCropBitmap(original: Bitmap, targetW: Int, targetH: Int): Bitmap {
        return try {
            val bitmapRatio = original.width.toFloat() / original.height.toFloat()
            val screenRatio = targetW.toFloat() / targetH.toFloat()

            var cropW = original.width
            var cropH = original.height
            var cropX = 0
            var cropY = 0

            if (bitmapRatio > screenRatio) {
                cropW = (original.height * screenRatio).toInt()
                cropX = (original.width - cropW) / 2
            } else {
                cropH = (original.width / screenRatio).toInt()
                cropY = (original.height - cropH) / 2
            }

            val cropped = Bitmap.createBitmap(original, cropX, cropY, cropW, cropH)
            Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
        } catch (e: Exception) {
            Bitmap.createScaledBitmap(original, targetW, targetH, true)
        }
    }

    private fun fetchAndPreCropWallpapers(query: String, screenW: Int, screenH: Int) {
        val sharedPrefs = requireActivity().getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("API_KEY", "") ?: ""

        val apiService = Retrofit.Builder()
            .baseUrl("https://api.pexels.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PexelsApiService::class.java)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val randomPage = (1..5).random()
                val response = apiService.searchWallpapers(apiKey, query, null, 15, randomPage)

                if (response.isSuccessful) {
                    val photos = response.body()?.photos
                    if (photos != null && photos.size >= 2) {
                        val shuffledPhotos = photos.shuffled()

                        lockImageUrl = shuffledPhotos[0].src.large2x
                        homeImageUrl = shuffledPhotos[1].src.large2x

                        val loader = ImageLoader(requireContext())

                        val lockReq = ImageRequest.Builder(requireContext())
                            .data(lockImageUrl).allowHardware(false).build()
                        val rawLockBitmap = ((loader.execute(lockReq) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap

                        val homeReq = ImageRequest.Builder(requireContext())
                            .data(homeImageUrl).allowHardware(false).build()
                        val rawHomeBitmap = ((loader.execute(homeReq) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap

                        if (rawLockBitmap != null && rawHomeBitmap != null) {
                            exactLockBitmap = centerCropBitmap(rawLockBitmap, screenW, screenH)
                            exactHomeBitmap = centerCropBitmap(rawHomeBitmap, screenW, screenH)

                            withContext(Dispatchers.Main) {
                                binding.lockScreenPreview.setImageBitmap(exactLockBitmap)
                                binding.homeScreenPreview.setImageBitmap(exactHomeBitmap)
                                binding.btnApplyPair.visibility = View.VISIBLE
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Not enough images for this vibe.", Toast.LENGTH_SHORT).show() }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Network error.", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun applyExactBitmaps(screenW: Int, screenH: Int) {
        Toast.makeText(requireContext(), "Applying Exact Cropped Pair...", Toast.LENGTH_LONG).show()

        val appliedQuery = binding.searchTheme.text.toString()
        val prefs = requireActivity().getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
        val liked = prefs.getStringSet("LIKED_THEMES", mutableSetOf()) ?: mutableSetOf()
        liked.add(appliedQuery)

        prefs.edit().putStringSet("LIKED_THEMES", liked).apply()
        prefs.edit().putString("CURRENT_LOCK_URL", lockImageUrl).apply()
        prefs.edit().putString("CURRENT_HOME_URL", homeImageUrl).apply()

        currentAutoQuery = ""

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(requireContext())

                if (exactLockBitmap != null && exactHomeBitmap != null) {
                    val lockStream = ByteArrayOutputStream().apply {
                        exactLockBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, this)
                    }.toByteArray().inputStream()

                    val homeStream = ByteArrayOutputStream().apply {
                        exactHomeBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, this)
                    }.toByteArray().inputStream()

                    wallpaperManager.setStream(lockStream, null, true, WallpaperManager.FLAG_LOCK)
                    wallpaperManager.setStream(homeStream, null, true, WallpaperManager.FLAG_SYSTEM)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Center Crop Applied Perfectly!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}