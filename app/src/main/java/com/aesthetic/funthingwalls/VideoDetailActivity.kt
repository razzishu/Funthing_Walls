package com.aesthetic.funthingwalls

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aesthetic.funthingwalls.databinding.ActivityVideoDetailBinding

class VideoDetailActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var binding: ActivityVideoDetailBinding
    private var mediaPlayer: MediaPlayer? = null
    private var videoUrl: String? = null
    private var thumbnailUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUrl = intent.getStringExtra("VIDEO_URL")
        thumbnailUrl = intent.getStringExtra("THUMBNAIL_URL")

        binding.surfaceView.holder.addCallback(this)
        binding.btnClose.setOnClickListener { finish() }

        binding.btnSetLive.setOnClickListener {
            val prefs = getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)

            // Instantly save BOTH the video URL and the static Thumbnail URL
            prefs.edit().putString("LIVE_WALLPAPER_URI", videoUrl).apply()
            prefs.edit().putString("LIVE_WALLPAPER_THUMBNAIL", thumbnailUrl).apply()

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, LiveWallpaperService::class.java))
            startActivity(intent)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (videoUrl == null) return
        try {
            mediaPlayer = MediaPlayer().apply {
                setSurface(holder.surface)
                setDataSource(videoUrl)
                isLooping = true
                setVolume(0f, 0f)
                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) {}
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { mediaPlayer?.release(); mediaPlayer = null }
}