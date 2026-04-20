package com.`fun`.walls.workers

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.AudioManager
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.core.graphics.toColorInt
import com.`fun`.walls.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class ChameleonWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return ChameleonEngine()
    }

    inner class ChameleonEngine : Engine() {
        private var isVisible = false
        private lateinit var audioManager: AudioManager
        private var time = 0f

        private var color1 = "#0F2027".toColorInt()
        private var color2 = "#203A43".toColorInt()
        private var color3 = "#2C5364".toColorInt()
        private var color4 = "#121212".toColorInt()

        private val paint1 = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        private val paint3 = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        private val paint4 = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

        private val matrix1 = Matrix()
        private val matrix2 = Matrix()
        private val matrix3 = Matrix()
        private val matrix4 = Matrix()

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                drawFrame()
                if (isVisible) Choreographer.getInstance().postFrameCallback(this)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            extractSystemWallpaperColors()
        }

        override fun onDestroy() {
            super.onDestroy()
            Choreographer.getInstance().removeFrameCallback(frameCallback)

            // FIX: Safety mechanism
            if (!isPreview) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val settings = SettingsManager(applicationContext)
                        if (settings.automationMode.firstOrNull() == "Music") {
                            settings.saveAutomationMode("None")
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                extractSystemWallpaperColors()
                Choreographer.getInstance().postFrameCallback(frameCallback)
            } else {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            setupGradients(width.toFloat())
            drawFrame()
        }

        private fun extractSystemWallpaperColors() {
            try {
                // FIX: Removed SDK_INT check as minSdk is 31
                val wm = WallpaperManager.getInstance(applicationContext)
                val colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (colors != null) {
                    color1 = colors.primaryColor.toArgb()
                    color2 = colors.secondaryColor?.toArgb() ?: manipulateColor(color1, 0.7f)
                    color3 = colors.tertiaryColor?.toArgb() ?: manipulateColor(color1, 1.3f)
                    color4 = manipulateColor(color2, 1.2f)
                    val surfaceRect = surfaceHolder.surfaceFrame
                    if (surfaceRect.width() > 0) setupGradients(surfaceRect.width().toFloat())
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        private fun manipulateColor(color: Int, factor: Float): Int {
            val a = Color.alpha(color)
            // FIX: Replaced Java Math.round with Kotlin roundToInt
            val r = (Color.red(color) * factor).roundToInt().coerceIn(0, 255)
            val g = (Color.green(color) * factor).roundToInt().coerceIn(0, 255)
            val b = (Color.blue(color) * factor).roundToInt().coerceIn(0, 255)
            return Color.argb(a, r, g, b)
        }

        private fun setupGradients(w: Float) {
            val radius = w * 1.2f
            paint1.shader = RadialGradient(0f, 0f, radius, color1, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            paint2.shader = RadialGradient(0f, 0f, radius * 0.9f, color2, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            paint3.shader = RadialGradient(0f, 0f, radius * 1.1f, color3, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            paint4.shader = RadialGradient(0f, 0f, radius * 0.8f, color4, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                // FIX: Use try-catch fallback for lockHardwareCanvas instead of SDK checks
                canvas = try { holder.lockHardwareCanvas() } catch (_: Exception) { holder.lockCanvas() }

                if (canvas != null) {
                    val w = canvas.width.toFloat()
                    val h = canvas.height.toFloat()

                    canvas.drawColor(manipulateColor(color1, 0.4f))
                    val isPlaying = audioManager.isMusicActive
                    val speed = if (isPlaying) 0.035f else 0.003f
                    time += speed
                    // FIX: Replaced Java Math.sin with Kotlin sin
                    val pulse = if (isPlaying) (sin(time * 3f) * 40f) else 0f

                    val cx = w / 2f
                    val cy = h / 2f

                    matrix1.setTranslate(cx + sin(time * 0.7f) * (cx + pulse), cy + cos(time * 0.5f) * (cy + pulse))
                    paint1.shader.setLocalMatrix(matrix1)
                    canvas.drawPaint(paint1)

                    matrix2.setTranslate(cx + cos(time * 0.9f) * (cx + pulse), cy + sin(time * 0.8f) * (cy + pulse))
                    paint2.shader.setLocalMatrix(matrix2)
                    canvas.drawPaint(paint2)

                    matrix3.setTranslate(cx + sin(time * 0.4f) * (cx * 0.5f), cy + sin(time * 1.1f) * (cy + pulse))
                    paint3.shader.setLocalMatrix(matrix3)
                    canvas.drawPaint(paint3)

                    matrix4.setTranslate(cx + cos(time * 1.2f) * (cx + pulse), cy + sin(time * 0.6f) * (cy + pulse))
                    paint4.shader.setLocalMatrix(matrix4)
                    canvas.drawPaint(paint4)
                }
            } finally {
                if (canvas != null) holder?.unlockCanvasAndPost(canvas)
            }
        }
    }
}