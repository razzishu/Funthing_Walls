package com.aesthetic.funthingwalls

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aesthetic.funthingwalls.databinding.FragmentLiveBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LiveFragment : Fragment() {

    private var _binding: FragmentLiveBinding? = null
    private val binding get() = _binding!!

    private lateinit var videoAdapter: VideoAdapter
    private val videoList = mutableListOf<PexelsVideo>()

    private var currentPage = 1
    private var isLoading = false
    private var selectedLocalVideoUri: String? = null

    // Safe launcher for local video files
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            requireActivity().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedLocalVideoUri = it.toString()
            playLocalVideoPreview(selectedLocalVideoUri!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.videoRecyclerView.layoutManager = gridLayoutManager

        videoAdapter = VideoAdapter(videoList)
        binding.videoRecyclerView.adapter = videoAdapter

        // Infinite Scroll Logic
        binding.videoRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = gridLayoutManager.childCount
                val totalItemCount = gridLayoutManager.itemCount
                val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    currentPage++
                    fetchPopularVideos()
                }
            }
        })

        binding.btnSearchVideo.setOnClickListener {
            val query = binding.searchVideo.text.toString().trim()
            if (query.isNotEmpty()) {
                currentPage = 1
                videoList.clear()
                videoAdapter.notifyDataSetChanged()
                fetchSearchVideos(query)
            }
        }

        binding.btnPickLocalVideo.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        binding.btnApplyLive.setOnClickListener {
            if (selectedLocalVideoUri != null) {
                applyLiveWallpaper()
            }
        }

        fetchPopularVideos()
    }

    private fun fetchPopularVideos() {
        isLoading = true
        if (currentPage == 1) binding.progressBar.visibility = View.VISIBLE

        val sharedPrefs = requireActivity().getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("API_KEY", "") ?: ""

        val apiService = Retrofit.Builder()
            .baseUrl("https://api.pexels.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PexelsApiService::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getPopularVideos(apiKey, 15, currentPage)
                if (response.isSuccessful) {
                    val newVideos = response.body()?.videos ?: emptyList()
                    withContext(Dispatchers.Main) {
                        videoAdapter.addVideos(newVideos)
                        binding.progressBar.visibility = View.GONE
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    isLoading = false
                }
            }
        }
    }

    private fun fetchSearchVideos(query: String) {
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE

        val sharedPrefs = requireActivity().getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("API_KEY", "") ?: ""

        val apiService = Retrofit.Builder()
            .baseUrl("https://api.pexels.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PexelsApiService::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.searchVideos(apiKey, query, 15, currentPage)
                if (response.isSuccessful) {
                    val newVideos = response.body()?.videos ?: emptyList()
                    withContext(Dispatchers.Main) {
                        videoAdapter.addVideos(newVideos)
                        binding.progressBar.visibility = View.GONE
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    isLoading = false
                }
            }
        }
    }

    private fun playLocalVideoPreview(uriString: String) {
        binding.videoPreview.visibility = View.VISIBLE
        binding.btnApplyLive.visibility = View.VISIBLE
        binding.videoRecyclerView.visibility = View.GONE

        binding.videoPreview.stopPlayback()
        binding.videoPreview.setVideoURI(Uri.parse(uriString))
        binding.videoPreview.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(0f, 0f)
            mediaPlayer.start()
        }
    }

    private fun applyLiveWallpaper() {
        val prefs = requireActivity().getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)

        // Save the Local Video URI as BOTH the video source and the thumbnail color source!
        prefs.edit().putString("LIVE_WALLPAPER_URI", selectedLocalVideoUri).apply()
        prefs.edit().putString("LIVE_WALLPAPER_THUMBNAIL", selectedLocalVideoUri).apply()

        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(requireContext(), LiveWallpaperService::class.java)
            )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Live Wallpaper not supported.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.videoPreview?.stopPlayback()
        _binding = null
    }
}