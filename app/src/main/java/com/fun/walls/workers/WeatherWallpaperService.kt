package com.`fun`.walls.workers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.`fun`.walls.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class WeatherWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return WeatherEngine()
    }

    inner class WeatherEngine : Engine(), SensorEventListener {
        private var isVisible = false
        private var time = 0f
        private var lastHour = -1
        private var isFirstUpdate = true
        private var lastDrawTime = 0L

        private val calendar = Calendar.getInstance()
        private var lastTimeCheckMillis = 0L

        private lateinit var sensorManager: SensorManager
        private var powerManager: PowerManager? = null
        private var rotationSensor: Sensor? = null
        private var targetParallaxX = 0f
        private var targetParallaxY = 0f
        private var currentParallaxX = 0f
        private var currentParallaxY = 0f

        private var targetTopColor = "#2980B9".toColorInt()
        private var targetBottomColor = "#6DD5FA".toColorInt()
        private var targetCloudColor = Color.argb(180, 255, 255, 255)
        private var targetWindSpeed = 0.5f
        private var targetCloudCover = 0f
        private var targetRainIntensity = 0f
        private var targetStarAlpha = 0f
        private var targetHazeAlpha = 0f
        private var currentCondition = "Clear"
        private var forceHideCelestial = false
        private var windDirectionX = 1f

        private var currentTopColor = targetTopColor
        private var currentBottomColor = targetBottomColor
        private var currentCloudColor = targetCloudColor
        private var currentWindSpeed = targetWindSpeed
        private var currentCloudCover = targetCloudCover
        private var currentRainIntensity = targetRainIntensity
        private var currentStarAlpha = targetStarAlpha
        private var currentHazeAlpha = targetHazeAlpha

        private var currentTemp = "--°C"
        private var currentLocation = "Syncing..."

        private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        private val splashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val glassDropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        private val lightningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 10f; strokeJoin = Paint.Join.ROUND }
        private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 5f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
        private val fireflyCorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fireflyGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val textTempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 38f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT; setShadowLayer(8f, 0f, 4f, Color.BLACK)
        }
        private val textLocPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 24f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT; setShadowLayer(6f, 0f, 3f, Color.BLACK)
        }

        private val clouds = Array(15) { Cloud() }
        private val raindrops = Array(350) { RainDrop() }
        private val hailDrops = Array(50) { HailDrop() }
        private val glassDrops = Array(15) { GlassDrop() }
        private val stars = Array(150) { Star() }
        private val shootingStars = Array(4) { ShootingStar() }
        private val birds = Array(15) { Bird() }
        private val fireflies = Array(30) { Firefly() }

        private var lightningPath = Path()
        private var lightningAlpha = 0

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isVisible) return

                val now = System.currentTimeMillis()
                val isPowerSave = powerManager?.isPowerSaveMode == true
                
                // Dynamic FPS: 60 (Normal), 30 (Weathering), 20 (Power Save)
                val targetFrameTime = when {
                    isPowerSave -> 50L // 20 FPS
                    currentRainIntensity > 0.01f || currentCondition == "Blizzard" -> 22L // ~45 FPS
                    else -> 33L // 30 FPS
                }

                if (now - lastDrawTime >= targetFrameTime) {
                    drawFrame()
                    lastDrawTime = now
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            powerManager = ContextCompat.getSystemService(applicationContext, PowerManager::class.java)
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

            val scope = CoroutineScope(Dispatchers.IO)
            val settings = SettingsManager(applicationContext)

            scope.launch {
                settings.weatherCondition.collect { condition ->
                    currentCondition = condition
                    updateClimateTargets(condition)
                }
            }
            scope.launch { settings.weatherTemp.collect { currentTemp = it } }
            scope.launch { settings.weatherLoc.collect { currentLocation = it } }
        }

        private fun updateClimateTargets(condition: String) {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isNight = hour !in 6..18

            fun parseDim(colorStr: String): Int {
                val c = colorStr.toColorInt()
                if (!isNight) return c
                return Color.argb(Color.alpha(c), (Color.red(c) * 0.25f).toInt(), (Color.green(c) * 0.25f).toInt(), (Color.blue(c) * 0.25f).toInt())
            }

            val baseTop = if (isNight) "#080B12".toColorInt() else "#2980B9".toColorInt()
            val baseBottom = if (isNight) "#1A2035".toColorInt() else "#6DD5FA".toColorInt()

            targetStarAlpha = if (isNight) 1f else 0f
            forceHideCelestial = false
            windDirectionX = listOf(-1f, 1f).random()
            targetHazeAlpha = 0f

            when (condition) {
                "Clear" -> {
                    targetTopColor = baseTop; targetBottomColor = baseBottom
                    targetCloudColor = if (isNight) Color.argb(20, 200, 200, 220) else Color.argb(100, 255, 255, 255)
                    targetCloudCover = 0.1f; targetWindSpeed = 0.5f; targetRainIntensity = 0f
                }
                "Partly Cloudy" -> {
                    targetTopColor = baseTop; targetBottomColor = baseBottom
                    targetCloudColor = if (isNight) Color.argb(40, 200, 200, 220) else Color.argb(180, 255, 255, 255)
                    targetCloudCover = 0.4f; targetWindSpeed = 1.0f; targetRainIntensity = 0f
                }
                "Windy" -> {
                    targetTopColor = baseTop; targetBottomColor = baseBottom
                    targetCloudColor = if (isNight) Color.argb(50, 200, 200, 220) else Color.argb(160, 255, 255, 255)
                    targetCloudCover = 0.5f; targetWindSpeed = 4.5f; targetRainIntensity = 0f
                }
                "Cloudy", "Overcast" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#5D6D7E"); targetBottomColor = parseDim("#95A5A6")
                    targetCloudColor = Color.argb(220, 190, 200, 210)
                    targetCloudCover = 1.0f; targetWindSpeed = 0.8f; targetRainIntensity = 0f
                }
                "Haze" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#8E9EAB"); targetBottomColor = parseDim("#EEF2F3")
                    targetCloudCover = 0.8f; targetWindSpeed = 0.2f; targetRainIntensity = 0f
                    targetHazeAlpha = 0.6f
                }
                "Drizzle" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#4B5358"); targetBottomColor = parseDim("#7B858B")
                    targetCloudColor = Color.argb(240, 150, 160, 170)
                    targetCloudCover = 0.8f; targetWindSpeed = 1.5f; targetRainIntensity = 0.3f
                }
                "Rain" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#2C3E50"); targetBottomColor = parseDim("#4CA1AF")
                    targetCloudColor = Color.argb(255, 120, 130, 140)
                    targetCloudCover = 1.0f; targetWindSpeed = 3.0f; targetRainIntensity = 0.6f
                }
                "Heavy Rain" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#1F2833"); targetBottomColor = parseDim("#34495E")
                    targetCloudColor = Color.argb(255, 100, 110, 120)
                    targetCloudCover = 1.0f; targetWindSpeed = 4.0f; targetRainIntensity = 1.0f
                }
                "Thunderstorm" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#141E30"); targetBottomColor = parseDim("#243B55")
                    targetCloudColor = Color.argb(255, 80, 90, 100)
                    targetCloudCover = 1.0f; targetWindSpeed = 5.0f; targetRainIntensity = 1.0f
                }
                "Snow" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#757F9A"); targetBottomColor = parseDim("#D7DDE8")
                    targetCloudColor = Color.argb(200, 230, 240, 255)
                    targetCloudCover = 1.0f; targetWindSpeed = 1.0f; targetRainIntensity = 0f
                }
                "Blizzard" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#4B5A6F"); targetBottomColor = parseDim("#B0BEC5")
                    targetCloudColor = Color.argb(240, 240, 245, 255)
                    targetCloudCover = 1.0f; targetWindSpeed = 6.0f; targetRainIntensity = 0f
                }
                "Hail" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#243B55"); targetBottomColor = parseDim("#455A64")
                    targetCloudColor = Color.argb(255, 100, 110, 120)
                    targetCloudCover = 1.0f; targetWindSpeed = 3.5f; targetRainIntensity = 0.5f
                }
                "Fog" -> {
                    targetStarAlpha = 0f
                    targetTopColor = parseDim("#BBD2C5"); targetBottomColor = parseDim("#536976")
                    targetCloudColor = Color.argb(100, 220, 230, 240)
                    targetCloudCover = 1.0f; targetWindSpeed = 0.3f; targetRainIntensity = 0f
                    targetHazeAlpha = 0.8f
                }
            }

            if (isFirstUpdate) {
                currentTopColor = targetTopColor
                currentBottomColor = targetBottomColor
                currentCloudColor = targetCloudColor
                currentWindSpeed = targetWindSpeed
                currentCloudCover = targetCloudCover
                currentRainIntensity = targetRainIntensity
                currentStarAlpha = targetStarAlpha
                currentHazeAlpha = targetHazeAlpha
                isFirstUpdate = false
            }
        }

        private fun blendColor(current: Int, target: Int): Int {
            if (current == target) return target
            val blendFactor = 0.015f
            fun step(c: Int, t: Int): Int {
                val diff = t - c
                if (diff == 0) return t
                val stepAmount = diff * blendFactor
                return c + when {
                    stepAmount > 0f && stepAmount < 1f -> 1
                    stepAmount < 0f && stepAmount > -1f -> -1
                    else -> stepAmount.toInt()
                }
            }
            return Color.argb(
                step(Color.alpha(current), Color.alpha(target)),
                step(Color.red(current), Color.red(target)),
                step(Color.green(current), Color.green(target)),
                step(Color.blue(current), Color.blue(target))
            )
        }

        private fun generateLightning(w: Float, h: Float) {
            lightningPath.reset()
            var startX = Random.nextFloat() * w
            var startY = 0f
            lightningPath.moveTo(startX, startY)
            repeat(15) {
                startX += (Random.nextFloat() - 0.5f) * 400f
                startY += Random.nextFloat() * (h / 6f)
                lightningPath.lineTo(startX, startY)
            }
            lightningAlpha = 255
        }

        override fun onDestroy() {
            super.onDestroy()
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            sensorManager.unregisterListener(this)
            if (!isPreview) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (SettingsManager(applicationContext).automationMode.firstOrNull() == "Weather") {
                            SettingsManager(applicationContext).saveAutomationMode("None")
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                Choreographer.getInstance().postFrameCallback(frameCallback)
                rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            } else {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
                sensorManager.unregisterListener(this)
            }
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (!isVisible) return
            if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientationValues = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationValues)
                targetParallaxX = (orientationValues[2] * -180f)
                targetParallaxY = (orientationValues[1] * -180f)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            val w = width.toFloat()
            val h = height.toFloat()
            clouds.forEach { it.reset(w, h, true, windDirectionX) }
            raindrops.forEach { it.reset(w) }
            stars.forEach { it.reset(w, h) }
            shootingStars.forEach { it.reset(w, h) }
            hailDrops.forEach { it.reset(w) }
            glassDrops.forEach { it.active = false }
            birds.forEach { it.reset(w, h, initialSpawn = true) }
            fireflies.forEach { it.reset(w, h, initialSpawn = true) }
        }

        private var lastSkyColors = Pair(0, 0)
        private var lastSkyHeight = 0f

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockHardwareCanvas() ?: holder.lockCanvas()

                if (canvas != null) {
                    val w = canvas.width.toFloat()
                    val h = canvas.height.toFloat()
                    time += 0.05f

                    currentParallaxX += (targetParallaxX - currentParallaxX) * 0.1f
                    currentParallaxY += (targetParallaxY - currentParallaxY) * 0.1f

                    val now = System.currentTimeMillis()
                    if (now - lastTimeCheckMillis > 60000) {
                        calendar.timeInMillis = now
                        lastTimeCheckMillis = now
                    }

                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                    if (currentHour != lastHour) {
                        lastHour = currentHour
                        updateClimateTargets(currentCondition)
                    }

                    val blendSpeed = 0.015f
                    currentTopColor = blendColor(currentTopColor, targetTopColor)
                    currentBottomColor = blendColor(currentBottomColor, targetBottomColor)
                    currentCloudColor = blendColor(currentCloudColor, targetCloudColor)
                    currentWindSpeed += (targetWindSpeed - currentWindSpeed) * blendSpeed
                    currentCloudCover += (targetCloudCover - currentCloudCover) * blendSpeed
                    currentRainIntensity += (targetRainIntensity - currentRainIntensity) * blendSpeed
                    currentStarAlpha += (targetStarAlpha - currentStarAlpha) * blendSpeed
                    currentHazeAlpha += (targetHazeAlpha - currentHazeAlpha) * blendSpeed

                    // 1. SKY (Optimized: Cache Shader)
                    if (lastSkyColors.first != currentTopColor || lastSkyColors.second != currentBottomColor || lastSkyHeight != h) {
                        skyPaint.shader = LinearGradient(0f, 0f, 0f, h, currentTopColor, currentBottomColor, Shader.TileMode.CLAMP)
                        lastSkyColors = Pair(currentTopColor, currentBottomColor)
                        lastSkyHeight = h
                    }
                    canvas.drawRect(-100f, -100f, w + 100f, h + 100f, skyPaint)

                    // 2. CELESTIAL BODIES
                    val floatHour = currentHour + (calendar.get(Calendar.MINUTE) / 60f)
                    val isDay = floatHour in 6.0..18.0
                    val celestialAlpha = if (forceHideCelestial) 0 else (255 * (1f - currentCloudCover)).toInt().coerceIn(0, 255)

                    if (celestialAlpha > 10) {
                        if (isDay) {
                            val progress = (floatHour - 6f) / 12f
                            val sunX = (w * progress) + (currentParallaxX * 0.2f)
                            val sunY = (h * 0.5f - sin(progress * PI.toFloat()) * (h * 0.35f)) + (currentParallaxY * 0.2f)
                            val sunPulse = sin(time * 2f) * 15f
                            // Sun Shader is Dynamic, but we can reuse RadialGradient object if supported (not easy in vector draw)
                            sunPaint.shader = RadialGradient(sunX, sunY, 180f + sunPulse, Color.argb(celestialAlpha, 255, 235, 150), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                            canvas.drawCircle(sunX, sunY, 190f + sunPulse, sunPaint)
                            sunPaint.shader = RadialGradient(sunX, sunY, 60f, Color.argb(celestialAlpha, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                            canvas.drawCircle(sunX, sunY, 60f, sunPaint)
                        } else {
                            val nightHour = if (floatHour > 18) floatHour - 18f else floatHour + 6f
                            val progress = nightHour / 12f
                            val moonX = (w * progress) + (currentParallaxX * 0.2f)
                            val moonY = (h * 0.5f - sin(progress * PI.toFloat()) * (h * 0.35f)) + (currentParallaxY * 0.2f)
                            moonPaint.shader = RadialGradient(moonX, moonY, 110f, Color.argb(celestialAlpha, 200, 220, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                            canvas.drawCircle(moonX, moonY, 110f, moonPaint)
                            moonPaint.shader = RadialGradient(moonX, moonY, 40f, Color.argb(celestialAlpha, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                            canvas.drawCircle(moonX, moonY, 40f, moonPaint)
                        }
                    }

                    // 3. ECOSYSTEM
                    val isRaining = currentRainIntensity > 0.05f || currentCondition in listOf("Snow", "Blizzard", "Hail", "Thunderstorm")

                    if (isDay && !isRaining && currentCloudCover < 0.7f) {
                        val birdAlpha = (180 * (1f - currentCloudCover)).toInt().coerceIn(0, 255)
                        if (birdAlpha > 10) {
                            birdPaint.color = Color.argb(birdAlpha, 30, 35, 45)
                            birds.forEach { b ->
                                b.x += b.speed
                                b.y += sin(time + b.offset) * 0.6f
                                val drawX = b.x + (currentParallaxX * 0.6f)
                                val drawY = b.y + (currentParallaxY * 0.6f)
                                val wingFlap = sin(time * 10f + b.offset) * b.size * 0.8f
                                canvas.drawLine(drawX, drawY, drawX - b.size * 1.5f, drawY - b.size * 0.5f - wingFlap, birdPaint)
                                canvas.drawLine(drawX, drawY, drawX - b.size * 1.5f, drawY + b.size * 0.5f - wingFlap, birdPaint)
                                if (b.x > w + 100f) b.reset(w, h, initialSpawn = false)
                            }
                        }
                    }

                    if (!isDay && !isRaining) {
                        val fireflyAlphaFactor = (1f - currentCloudCover).coerceIn(0.2f, 1f)
                        fireflies.forEach { f ->
                            f.x += sin(time + f.phase) * 1.5f
                            f.y -= f.speed
                            val drawX = f.x + (currentParallaxX * 0.7f)
                            val drawY = f.y + (currentParallaxY * 0.7f)
                            val pulse = (sin(time * 3f + f.phase) + 1f) / 2f
                            val alpha = (220 * pulse * fireflyAlphaFactor).toInt()
                            fireflyCorePaint.color = Color.argb(alpha, 150, 255, 100)
                            // Firefly Glow is dynamic
                            fireflyGlowPaint.shader = RadialGradient(drawX, drawY, f.size * 5f, Color.argb((alpha * 0.3f).toInt(), 100, 255, 50), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                            canvas.drawCircle(drawX, drawY, f.size * 5f, fireflyGlowPaint)
                            canvas.drawCircle(drawX, drawY, f.size, fireflyCorePaint)
                            if (f.y < h * 0.2f) f.reset(w, h, initialSpawn = false)
                        }
                    }

                    // 4. STARS & SHOOTING STARS
                    val finalStarAlpha = if (forceHideCelestial) 0 else (currentStarAlpha * (1f - currentCloudCover) * 255).toInt().coerceIn(0, 255)
                    if (finalStarAlpha > 5) {
                        stars.forEach { s ->
                            val twinkle = (sin(time + s.seed) + 1f) / 2f
                            starPaint.color = Color.argb((finalStarAlpha * twinkle).toInt(), 255, 255, 255)
                            canvas.drawCircle(s.x + (currentParallaxX * 0.1f), s.y + (currentParallaxY * 0.1f), s.size, starPaint)
                        }
                        shootingStars.forEach { ss ->
                            if (ss.active) {
                                ss.x -= ss.speed
                                ss.y += ss.speed * 0.5f
                                ss.alpha -= 8
                                if (ss.alpha <= 0) ss.active = false
                                else {
                                    starPaint.color = Color.argb(ss.alpha, 255, 255, 255)
                                    starPaint.strokeWidth = 4f
                                    val drawX = ss.x + (currentParallaxX * 0.1f)
                                    val drawY = ss.y + (currentParallaxY * 0.1f)
                                    canvas.drawLine(drawX, drawY, drawX + (ss.speed * 4), drawY - (ss.speed * 2f), starPaint)
                                }
                            } else if (Random.nextFloat() > 0.992f) {
                                ss.reset(w, h)
                                ss.active = true
                            }
                        }
                    }

                    // 5. WIND-AWARE VOLUMETRIC CLOUDS
                    val activeClouds = (clouds.size * currentCloudCover).toInt()
                    repeat(activeClouds) { i ->
                        val c = clouds[i]
                        val targetY = if (currentCondition == "Fog") h * 0.7f else c.baseY
                        c.y += (targetY - c.y) * 0.02f
                        c.x += (currentWindSpeed * c.z * windDirectionX)
                        for (puff in c.puffs) {
                            val puffX = c.x + puff.dx + (currentParallaxX * c.z * 0.4f)
                            val puffY = c.y + puff.dy + (currentParallaxY * c.z * 0.4f)
                            // Cloud Shaders are dynamic
                            cloudPaint.shader = RadialGradient(puffX, puffY, puff.radius, currentCloudColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                            canvas.drawCircle(puffX, puffY, puff.radius, cloudPaint)
                        }
                        if (windDirectionX > 0 && c.x - 400f > w) c.reset(w, h, false, windDirectionX)
                        else if (windDirectionX < 0 && c.x + 400f < 0) c.reset(w, h, false, windDirectionX)
                    }

                    // 6. VOLUMETRIC FOG (Optimized: Cache Shader)
                    if (currentHazeAlpha > 0.05f) {
                        val fogAlphaInt = (currentHazeAlpha * 200).toInt()
                        val fogColor = Color.argb(fogAlphaInt, 220, 220, 230)
                        fogPaint.shader = LinearGradient(0f, h * 0.6f, 0f, h, Color.TRANSPARENT, fogColor, Shader.TileMode.CLAMP)
                        canvas.drawRect(0f, h * 0.6f, w, h, fogPaint)
                    }

                    // 7. LIGHTNING
                    if (currentCondition == "Thunderstorm" || currentCondition == "Hail") {
                        if (Random.nextFloat() > 0.985f && lightningAlpha <= 0) generateLightning(w, h)
                        if (lightningAlpha > 0) {
                            canvas.drawColor(Color.argb(lightningAlpha / 2, 255, 255, 255))
                            lightningPaint.color = Color.argb(lightningAlpha, 255, 255, 255)
                            canvas.drawPath(lightningPath, lightningPaint)
                            lightningAlpha -= 12
                        }
                    }

                    // 8. RAIN, BLIZZARD, HAIL
                    if (currentCondition == "Hail") {
                        rainPaint.color = Color.argb(240, 240, 240, 255)
                        hailDrops.forEach { hDrop ->
                            hDrop.y += hDrop.speed
                            hDrop.x += currentWindSpeed * windDirectionX
                            val drawX = hDrop.x + (currentParallaxX * 1.2f)
                            val drawY = hDrop.y + (currentParallaxY * 1.2f)
                            canvas.drawCircle(drawX, drawY, hDrop.size, rainPaint)
                            if (hDrop.y > h - 50f && !hDrop.bounced) {
                                hDrop.speed = -hDrop.speed * 0.4f
                                hDrop.bounced = true
                            } else if (hDrop.bounced && hDrop.speed > 0 && hDrop.y > h + 100f) {
                                hDrop.reset(w)
                            }
                        }
                    }

                    if (currentRainIntensity > 0.01f || currentCondition == "Snow" || currentCondition == "Blizzard") {
                        val activeRain = (raindrops.size * (if (currentCondition == "Snow") 0.5f else if(currentCondition == "Blizzard") 1.0f else currentRainIntensity)).toInt()
                        repeat(activeRain) { i ->
                            val r = raindrops[i]
                            if (currentCondition == "Snow" || currentCondition == "Blizzard") {
                                val snowAlpha = if (currentCondition == "Blizzard") 255 else 200
                                rainPaint.color = Color.argb((snowAlpha * r.z).toInt(), 255, 255, 255)
                                r.y += (r.speed * (if(currentCondition == "Blizzard") 3.0f else 1.5f)) * r.z
                                r.x += sin(time + r.seed) * (2f * r.z) + (currentWindSpeed * r.z * windDirectionX)
                                val drawX = r.x + (currentParallaxX * r.z * 1.5f)
                                val drawY = r.y + (currentParallaxY * r.z * 1.5f)
                                canvas.drawCircle(drawX, drawY, (r.size * 2f) * r.z, rainPaint)
                            } else {
                                rainPaint.color = Color.argb((180 * r.z).toInt(), 200, 220, 255)
                                val windDx = currentWindSpeed * 3f * r.z * windDirectionX
                                val fallDy = r.speed * 4f * r.z
                                r.x += windDx
                                r.y += fallDy
                                rainPaint.strokeWidth = 6f * r.z
                                val streakLength = if (currentCondition == "Heavy Rain") 2.5f else 1.5f
                                val drawX = r.x + (currentParallaxX * r.z * 1.5f)
                                val drawY = r.y + (currentParallaxY * r.z * 1.5f)
                                canvas.drawLine(drawX, drawY, drawX - (windDx * streakLength), drawY - (fallDy * streakLength), rainPaint)
                                if (r.y > h && r.z > 0.7f && currentCondition != "Drizzle") {
                                    splashPaint.color = Color.argb(120, 200, 220, 255)
                                    canvas.drawOval(drawX - 20f, h - 10f, drawX + 20f, h + 8f, splashPaint)
                                }
                            }
                            if (r.y > h || r.x > w + 400f || r.x < -400f) r.reset(w)
                        }

                        if (currentCondition == "Drizzle" || currentCondition == "Rain" || currentCondition == "Heavy Rain") {
                            if (Random.nextFloat() > (if(currentCondition == "Drizzle") 0.96f else 0.85f)) {
                                val inactiveDrop = glassDrops.firstOrNull { !it.active }
                                inactiveDrop?.spawn(w, h)
                            }
                            glassDrops.forEach { g ->
                                if (g.active) {
                                    g.radius += g.expansionRate
                                    g.alpha -= 4
                                    if (g.alpha <= 0) g.active = false
                                    else {
                                        glassDropPaint.color = Color.argb(g.alpha, 255, 255, 255)
                                        canvas.drawCircle(g.x, g.y, g.radius, glassDropPaint)
                                        canvas.drawCircle(g.x, g.y, g.radius * 0.7f, glassDropPaint)
                                    }
                                }
                            }
                        }
                    }

                    // 9. HUD DASHBOARD
                    val textX = w - 50f
                    val textY = 180f
                    val hudText = "$currentTemp  •  $currentCondition"
                    canvas.drawText(hudText, textX, textY, textTempPaint)
                    canvas.drawText(currentLocation, textX, textY + 35f, textLocPaint)
                }
            } finally {
                if (canvas != null) holder?.unlockCanvasAndPost(canvas)
            }
        }
    }
}

// --- UTILITY CLASSES (Placed securely outside the main class!) ---
class Bird {
    var x = 0f; var y = 0f; var speed = 0f; var size = 0f; var offset = 0f
    fun reset(w: Float, h: Float, initialSpawn: Boolean) {
        x = if (initialSpawn) Random.nextFloat() * w else -Random.nextFloat() * 300f - 50f
        y = Random.nextFloat() * (h * 0.4f) + 100f
        speed = Random.nextFloat() * 2.5f + 1.5f
        size = Random.nextFloat() * 4f + 6f
        offset = Random.nextFloat() * 10f
    }
}

class Firefly {
    var x = 0f; var y = 0f; var speed = 0f; var size = 0f; var phase = 0f
    fun reset(w: Float, h: Float, initialSpawn: Boolean) {
        x = Random.nextFloat() * w
        y = if (initialSpawn) Random.nextFloat() * h else h + Random.nextFloat() * 200f
        speed = Random.nextFloat() * 1.5f + 0.5f
        size = Random.nextFloat() * 3f + 2f
        phase = Random.nextFloat() * 100f
    }
}

class Star {
    var x = 0f; var y = 0f; var size = 0f; var seed = 0f
    fun reset(w: Float, h: Float) {
        x = Random.nextFloat() * w; y = Random.nextFloat() * (h * 0.7f)
        size = Random.nextFloat() * 4f + 1f; seed = Random.nextFloat() * 100f
    }
}

class ShootingStar {
    var x = 0f; var y = 0f; var speed = 0f; var alpha = 0; var active = false
    fun reset(w: Float, h: Float) {
        x = w * (0.5f + Random.nextFloat()); y = Random.nextFloat() * (h * 0.3f)
        speed = Random.nextFloat() * 25f + 20f; alpha = 255
    }
}

class CloudPuff(var dx: Float, var dy: Float, var radius: Float)

class Cloud {
    var x = 0f; var y = 0f; var baseY = 0f; var z = 0f
    val puffs = Array(8) { CloudPuff(Random.nextFloat() * 300f - 150f, Random.nextFloat() * 100f - 50f, Random.nextFloat() * 250f + 150f) }
    fun reset(w: Float, h: Float, randomX: Boolean, windDirX: Float) {
        x = if(randomX) Random.nextFloat() * w else if (windDirX > 0) -400f else w + 400f
        baseY = Random.nextFloat() * (h * 0.4f); y = baseY; z = Random.nextFloat() * 0.8f + 0.2f
    }
}

class RainDrop {
    var x = 0f; var y = 0f; var speed = 0f; var z = 0f; var size = 0f; var seed = 0f
    fun reset(w: Float) {
        x = Random.nextFloat() * (w + 1000f) - 500f; y = Random.nextFloat() * -300f - 50f
        speed = Random.nextFloat() * 8f + 5f; size = Random.nextFloat() * 4f + 2f
        seed = Random.nextFloat() * 100f; z = Random.nextFloat() * 0.8f + 0.2f
    }
}

class HailDrop {
    var x = 0f; var y = 0f; var speed = 0f; var size = 0f; var bounced = false
    fun reset(w: Float) {
        x = Random.nextFloat() * (w + 400f) - 200f; y = Random.nextFloat() * -300f - 50f
        speed = Random.nextFloat() * 18f + 12f; size = Random.nextFloat() * 8f + 4f; bounced = false
    }
}

class GlassDrop {
    var x = 0f; var y = 0f; var radius = 0f; var alpha = 0; var expansionRate = 0f; var active = false
    fun spawn(w: Float, h: Float) {
        x = Random.nextFloat() * w; y = Random.nextFloat() * h
        radius = 0f; alpha = Random.nextInt(150, 220)
        expansionRate = Random.nextFloat() * 1.8f + 0.5f; active = true
    }
}