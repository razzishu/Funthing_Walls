package com.aesthetic.funthingwalls

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class WallpaperAdapter(private val photos: MutableList<Photo>) : RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder>() {

    class WallpaperViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.wallpaperImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper, parent, false)
        return WallpaperViewHolder(view)
    }

    override fun onBindViewHolder(holder: WallpaperViewHolder, position: Int) {
        val photo = photos[position]

        holder.imageView.load(photo.src.large2x) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
        }

        // NEW: Listen for clicks!
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, WallpaperDetailActivity::class.java)
            // Pass the original high-res image URL to the detail screen
            intent.putExtra("IMAGE_URL", photo.src.original)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = photos.size

    // This helps our Infinite Scroll add new images to the bottom seamlessly
    fun addPhotos(newPhotos: List<Photo>) {
        val startPosition = photos.size
        photos.addAll(newPhotos)
        notifyItemRangeInserted(startPosition, newPhotos.size)
    }
}