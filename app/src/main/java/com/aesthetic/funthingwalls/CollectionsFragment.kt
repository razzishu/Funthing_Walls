package com.aesthetic.funthingwalls

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aesthetic.funthingwalls.databinding.FragmentCollectionsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CollectionsFragment : Fragment() {

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var wallpaperAdapter: WallpaperAdapter
    private val photoList = mutableListOf<Photo>()

    // Track our infinite scrolling
    private var currentPage = 1
    private var isLoading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the Grid (2 columns wide)
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.layoutManager = gridLayoutManager

        wallpaperAdapter = WallpaperAdapter(photoList)
        binding.recyclerView.adapter = wallpaperAdapter

        // The Infinite Scroll Logic
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = gridLayoutManager.childCount
                val totalItemCount = gridLayoutManager.itemCount
                val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()

                // If we scroll to the bottom, fetch the next page!
                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    currentPage++
                    fetchCuratedWallpapers()
                }
            }
        })

        // Fetch the very first batch of wallpapers when the room opens
        fetchCuratedWallpapers()
    }

    private fun fetchCuratedWallpapers() {
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
                // Fetch 40 images at a time from Pexels Curated selection
                val response = apiService.getCuratedWallpapers(apiKey, 40, currentPage)
                if (response.isSuccessful) {
                    val newPhotos = response.body()?.photos ?: emptyList()
                    withContext(Dispatchers.Main) {
                        wallpaperAdapter.addPhotos(newPhotos)
                        binding.progressBar.visibility = View.GONE
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (context != null) Toast.makeText(requireContext(), "Error loading images", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    isLoading = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevents memory leaks!
    }
}