package com.`fun`.walls.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import androidx.work.WorkManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.`fun`.walls.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object WallpaperSetter {

    @Suppress("DEPRECATION")
    private fun applySmoothBlur(context: Context, original: Bitmap, blurAmount: Float): Bitmap {
        if (blurAmount <= 0f) return original
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, original)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(blurAmount.coerceIn(1f, 25f))
        script.setInput(input)
        script.forEach(output)
        val blurredBitmap = createBitmap(original.width, original.height, original.config ?: Bitmap.Config.ARGB_8888)
        output.copyTo(blurredBitmap)
        rs.destroy()
        return blurredBitmap
    }

    suspend fun applyCustomWallpaper(
        context: Context,
        imageUrl: String? = null,
        bitmapOverride: Bitmap? = null,
        screenFlag: Int,
        userScale: Float, offsetX: Float, offsetY: Float, blur: Float, grayscale: Float, dimAmount: Float,
        screenWidthPx: Float, screenHeightPx: Float,
        weatherEffect: String = "None",
        colorTemperature: Float = 1.0f,
        applyVignette: Boolean = false,
        isFromAutoEngine: Boolean = false
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isFromAutoEngine) {
                    val settings = SettingsManager(context)
                    settings.saveAutomationMode("None")
                    settings.saveLastWallpaperId(-1)
                    WorkManager.getInstance(context).cancelAllWorkByTag("auto_wallpaper")
                }

                val originalBitmap = if (bitmapOverride != null) {
                    bitmapOverride
                } else {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context).data(imageUrl).allowHardware(false).build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) result.drawable.toBitmap() else return@withContext false
                }

                val canvasWidth = screenWidthPx.toInt()
                val canvasHeight = screenHeightPx.toInt()
                var finalBitmap = createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(finalBitmap)

                val baseScale = maxOf(canvasWidth.toFloat() / originalBitmap.width, canvasHeight.toFloat() / originalBitmap.height)
                val matrix = Matrix()
                matrix.postScale(baseScale, baseScale)
                matrix.postTranslate((canvasWidth - (originalBitmap.width * baseScale)) / 2f, (canvasHeight - (originalBitmap.height * baseScale)) / 2f)
                matrix.postScale(userScale, userScale, canvasWidth / 2f, canvasHeight / 2f)
                matrix.postTranslate(offsetX, offsetY)

                val paint = Paint(Paint.FILTER_BITMAP_FLAG)

                val cm = ColorMatrix()
                cm.setSaturation(1f - grayscale)

                if (colorTemperature != 1.0f) {
                    val rScale = if (colorTemperature > 1f) colorTemperature else 1f
                    val bScale = if (colorTemperature < 1f) (1f / colorTemperature) else 1f
                    val tempCm = ColorMatrix(floatArrayOf(
                        rScale, 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, bScale, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    cm.postConcat(tempCm)
                }

                if (dimAmount > 0f) {
                    val dimScale = 1f - dimAmount
                    val dimCm = ColorMatrix(floatArrayOf(
                        dimScale, 0f, 0f, 0f, 0f,
                        0f, dimScale, 0f, 0f, 0f,
                        0f, 0f, dimScale, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    cm.postConcat(dimCm)
                }

                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(originalBitmap, matrix, paint)

                if (applyVignette) {
                    val vignettePaint = Paint()
                    vignettePaint.shader = LinearGradient(0f, 0f, 0f, canvasHeight * 0.25f, "#99000000".toColorInt(), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                    canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight * 0.25f, vignettePaint)

                    vignettePaint.shader = LinearGradient(0f, canvasHeight * 0.85f, 0f, canvasHeight.toFloat(), Color.TRANSPARENT, "#CC000000".toColorInt(), Shader.TileMode.CLAMP)
                    canvas.drawRect(0f, canvasHeight * 0.85f, canvasWidth.toFloat(), canvasHeight.toFloat(), vignettePaint)
                }

                if (weatherEffect == "Rain") {
                    canvas.drawColor(Color.argb(60, 0, 20, 50))
                    val rainPaint = Paint().apply { color = Color.argb(120, 200, 220, 255); strokeWidth = 4f }
                    repeat(151) {
                        val startX = Random.nextFloat() * canvasWidth
                        val startY = Random.nextFloat() * canvasHeight
                        canvas.drawLine(startX, startY, startX - 30f, startY + 80f, rainPaint)
                    }
                } else if (weatherEffect == "Snow") {
                    canvas.drawColor(Color.argb(40, 200, 200, 220))
                    val snowPaint = Paint().apply { color = Color.argb(200, 255, 255, 255) }
                    repeat(201) {
                        canvas.drawCircle(Random.nextFloat() * canvasWidth, Random.nextFloat() * canvasHeight, Random.nextFloat() * 8f, snowPaint)
                    }
                }

                finalBitmap = applySmoothBlur(context, finalBitmap, blur)
                WallpaperManager.getInstance(context).setBitmap(finalBitmap, null, true, screenFlag)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}