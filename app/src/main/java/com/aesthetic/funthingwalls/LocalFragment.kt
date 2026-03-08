package com.aesthetic.funthingwalls

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aesthetic.funthingwalls.databinding.FragmentLocalBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class LocalFragment : Fragment() {

    private var _binding: FragmentLocalBinding? = null
    private val binding get() = _binding!!

    private var localLockUri: Uri? = null
    private var localHomeUri: Uri? = null

    private val pickLockLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            localLockUri = it
            binding.lockScreenPreview.load(it) { crossfade(true) }
            checkIfBothReady()
        }
    }

    private val pickHomeLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            localHomeUri = it
            binding.homeScreenPreview.load(it) { crossfade(true) }
            checkIfBothReady()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLocalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val metrics = resources.displayMetrics
        val exactPhoneRatio = "${metrics.widthPixels}:${metrics.heightPixels}"

        (binding.lockScreenPreview.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = exactPhoneRatio
        (binding.homeScreenPreview.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = exactPhoneRatio

        binding.btnPickLock.setOnClickListener { pickLockLauncher.launch("image/*") }
        binding.btnPickHome.setOnClickListener { pickHomeLauncher.launch("image/*") }

        binding.btnApplyLocal.setOnClickListener {
            if (localLockUri != null && localHomeUri != null) {
                applyLocalWallpapers(metrics.widthPixels, metrics.heightPixels)
            }
        }
    }

    private fun checkIfBothReady() {
        if (localLockUri != null && localHomeUri != null) {
            binding.btnApplyLocal.visibility = View.VISIBLE
        }
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

    private fun applyLocalWallpapers(screenW: Int, screenH: Int) {
        Toast.makeText(requireContext(), "Processing Local Images...", Toast.LENGTH_LONG).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val loader = ImageLoader(requireContext())
                val wallpaperManager = WallpaperManager.getInstance(requireContext())

                val lockReq = ImageRequest.Builder(requireContext())
                    .data(localLockUri).allowHardware(false).build()
                val rawLockBitmap = ((loader.execute(lockReq) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap

                val homeReq = ImageRequest.Builder(requireContext())
                    .data(localHomeUri).allowHardware(false).build()
                val rawHomeBitmap = ((loader.execute(homeReq) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap

                if (rawLockBitmap != null && rawHomeBitmap != null) {
                    val exactLock = centerCropBitmap(rawLockBitmap, screenW, screenH)
                    val exactHome = centerCropBitmap(rawHomeBitmap, screenW, screenH)

                    val lockStream = ByteArrayOutputStream().apply {
                        exactLock.compress(Bitmap.CompressFormat.JPEG, 100, this)
                    }.toByteArray().inputStream()

                    val homeStream = ByteArrayOutputStream().apply {
                        exactHome.compress(Bitmap.CompressFormat.JPEG, 100, this)
                    }.toByteArray().inputStream()

                    wallpaperManager.setStream(lockStream, null, true, WallpaperManager.FLAG_LOCK)
                    wallpaperManager.setStream(homeStream, null, true, WallpaperManager.FLAG_SYSTEM)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Local Wallpapers Applied Perfectly!", Toast.LENGTH_SHORT).show()
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