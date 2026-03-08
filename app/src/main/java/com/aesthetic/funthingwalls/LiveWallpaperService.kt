package com.aesthetic.funthingwalls

import android.app.WallpaperColors
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var mediaPlayer: MediaPlayer? = null
        private lateinit var prefs: SharedPreferences

        // This holds the exact Material You colors in memory
        private var dynamicWallpaperColors: WallpaperColors? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs = getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)
            extractColorsAsync()
        }

        private fun extractColorsAsync() {
            val thumbnailUrl = prefs.getString("LIVE_WALLPAPER_THUMBNAIL", null) ?: return

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    var bitmap: Bitmap? = null

                    if (thumbnailUrl.startsWith("http")) {
                        // Fast download for online Pexels thumbnails
                        val connection = URL(thumbnailUrl).openConnection()
                        connection.connect()
                        bitmap = BitmapFactory.decodeStream(connection.getInputStream())
                    } else {
                        // Fast local frame extraction for gallery videos
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(applicationContext, Uri.parse(thumbnailUrl))
                        bitmap = retriever.getFrameAtTime(0)
                        retriever.release()
                    }

                    if (bitmap != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        // Generate the exact Material You color profile natively
                        dynamicWallpaperColors = WallpaperColors.fromBitmap(bitmap)

                        // THE MAGIC FIX: We explicitly tell the Android System to reload the colors NOW!
                        withContext(Dispatchers.Main) {
                            notifyColorsChanged()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // When Android hears 'notifyColorsChanged', it runs this function to grab the new colors!
        override fun onComputeColors(): WallpaperColors? {
            return dynamicWallpaperColors ?: super.onComputeColors()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == "LIVE_WALLPAPER_URI") {
                playVideo()
            }
            if (key == "LIVE_WALLPAPER_THUMBNAIL") {
                extractColorsAsync() // Run the math immediately when a new video is chosen!
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) mediaPlayer?.start() else mediaPlayer?.pause()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            playVideo()
        }

        private fun playVideo() {
            val videoUriString = prefs.getString("LIVE_WALLPAPER_URI", null)
            if (videoUriString != null && surfaceHolder.surface.isValid) {
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setSurface(surfaceHolder.surface)
                        setDataSource(applicationContext, Uri.parse(videoUriString))
                        isLooping = true
                        setVolume(0f, 0f)
                        setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                        prepareAsync()
                        setOnPreparedListener { start() }
                    }
                } catch (e: Exception) {}
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            mediaPlayer?.release()
            mediaPlayer = null
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}