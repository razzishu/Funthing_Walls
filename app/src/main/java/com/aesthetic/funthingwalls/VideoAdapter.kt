package com.aesthetic.funthingwalls

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load

class VideoAdapter(private val videos: MutableList<PexelsVideo>) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.videoThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]

        holder.thumbnail.load(video.image) { crossfade(true) }

        holder.itemView.setOnClickListener {
            val files = video.video_files
            if (!files.isNullOrEmpty()) {
                val hdFile = files.find { it.quality == "hd" } ?: files.first()

                val intent = Intent(holder.itemView.context, VideoDetailActivity::class.java)
                intent.putExtra("VIDEO_URL", hdFile.link)
                // NEW: Pass the high-quality thumbnail image to the detail screen
                intent.putExtra("THUMBNAIL_URL", video.image)
                holder.itemView.context.startActivity(intent)
            } else {
                Toast.makeText(holder.itemView.context, "This video link is broken.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = videos.size

    fun addVideos(newVideos: List<PexelsVideo>) {
        val start = videos.size
        videos.addAll(newVideos)
        notifyItemRangeInserted(start, newVideos.size)
    }
}