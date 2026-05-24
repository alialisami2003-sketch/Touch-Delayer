package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.CircleOverlay
import com.example.data.CircleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TouchDelayAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private lateinit var repository: CircleRepository
    private val activeOverlays = mutableMapOf<Int, OverlayCircleView>()
    
    private var isHandlingTap = false
    private var floatingToggleButton: FloatingToggleView? = null

    companion object {
        private const val TAG = "TouchDelayService"
        
        @Volatile
        var isServiceRunning = false
            private set

        @Volatile
        var isPositioningModeActive = false
            private set

        @Volatile
        var isDelayEnabled = true
            private set

        private var serviceInstance: TouchDelayAccessibilityService? = null

        fun updatePositioningMode(active: Boolean) {
            isPositioningModeActive = active
            serviceInstance?.refreshOverlayLayouts()
        }

        fun updateDelayEnabled(context: Context, enabled: Boolean) {
            isDelayEnabled = enabled
            val prefs = context.getSharedPreferences("TouchDelayPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_delay_enabled", enabled).apply()
            serviceInstance?.apply {
                refreshOverlayLayouts()
                floatingToggleButton?.updateState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val database = AppDatabase.getDatabase(this)
        repository = CircleRepository(database.circleOverlayDao())
        
        val prefs = getSharedPreferences("TouchDelayPrefs", Context.MODE_PRIVATE)
        isDelayEnabled = prefs.getBoolean("is_delay_enabled", true)
        
        serviceInstance = this
        isServiceRunning = true
        Log.d(TAG, "Accessibility Service created")
        
        // Add floating Enable/Disable widget
        showFloatingToggleButton()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service connected")
        
        // Start observing circle overlays from Room database
        serviceScope.launch {
            repository.allOverlays.collectLatest { list ->
                updateOverlaysOnScreen(list)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op for events, we only use gestures and window overlays
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceInstance = null
        serviceScope.cancel()
        removeAllOverlays()
        removeFloatingToggleButton()
        Log.d(TAG, "Accessibility Service destroyed")
    }

    private fun removeAllOverlays() {
        activeOverlays.forEach { (_, view) ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing view on destroy", e)
            }
        }
        activeOverlays.clear()
    }

    private fun showFloatingToggleButton() {
        if (floatingToggleButton != null) return
        val density = resources.displayMetrics.density
        val sizePx = (48 * density).roundToInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val widthPx = resources.displayMetrics.widthPixels
            x = widthPx - sizePx - (16 * density).roundToInt()
            y = (resources.displayMetrics.heightPixels / 2) - (sizePx / 2)
        }

        val view = FloatingToggleView(this)
        try {
            windowManager.addView(view, params)
            floatingToggleButton = view
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating toggle button", e)
        }
    }

    private fun removeFloatingToggleButton() {
        floatingToggleButton?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove floating toggle button", e)
            }
        }
        floatingToggleButton = null
    }

    /**
     * Recreates or updates layouts of on-screen floating circle overlays.
     */
    private fun updateOverlaysOnScreen(overlays: List<CircleOverlay>) {
        // Find overlays to remove
        val currentIdsInDb = overlays.map { it.id }.toSet()
        val idsToRemove = activeOverlays.keys.filter { it !in currentIdsInDb }
        
        idsToRemove.forEach { id ->
            activeOverlays[id]?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove view for id $id", e)
                }
                activeOverlays.remove(id)
            }
        }

        // Add or update overlays
        overlays.forEach { circle ->
            if (!circle.isEnabled) {
                // If disabled in DB, remove if visible
                activeOverlays[circle.id]?.let { view ->
                    try {
                        windowManager.removeView(view)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to remove view for disabled id ${circle.id}", e)
                    }
                    activeOverlays.remove(circle.id)
                }
                return@forEach
            }

            val density = resources.displayMetrics.density
            val diameterPx = (circle.radius * 2 * density).roundToInt()

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // Standard flags: non-focusable so surrounding clicks pass through, and always touchable
            val flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            val params = WindowManager.LayoutParams(
                diameterPx,
                diameterPx,
                layoutType,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = circle.x
                y = circle.y
            }

            val existingView = activeOverlays[circle.id]
            if (existingView != null) {
                // Update properties of the view
                existingView.updateCircleProperties(circle)
                try {
                    windowManager.updateViewLayout(existingView, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update layout parameters for id ${circle.id}", e)
                }
            } else {
                // Create a new Overlay view
                val newView = OverlayCircleView(this, circle)
                try {
                    windowManager.addView(newView, params)
                    activeOverlays[circle.id] = newView
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add overlay view for id ${circle.id}", e)
                }
            }
        }
    }

    /**
     * Called when states change. Forces all views to redraw and adapts layout params.
     */
    fun refreshOverlayLayouts() {
        serviceScope.launch {
            repository.allOverlays.collectLatest { list ->
                updateOverlaysOnScreen(list)
                cancel() // Collect once and finish
            }
        }
    }

    private var isOverlaysCurrentlyOnScreen = true

    private fun setOverlaysActive(active: Boolean) {
        if (active == isOverlaysCurrentlyOnScreen) return
        isOverlaysCurrentlyOnScreen = active

        if (active) {
            refreshOverlayLayouts()
            showFloatingToggleButton()
        } else {
            activeOverlays.forEach { (id, view) ->
                try {
                    view.stopCountdown()
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to completely remove overlay view $id to bypass tapjacking", e)
                }
            }
            activeOverlays.clear()
            removeFloatingToggleButton()
        }
    }

    /**
     * Handles tap interception and delayed tap simulation, complete with layout bypass.
     */
    fun handleDelayedTap(circle: CircleOverlay, tapX: Float, tapY: Float, remainingDelay: Long) {
        if (isHandlingTap) return
        isHandlingTap = true
        Log.d(TAG, "Delaying tap at ($tapX, $tapY) with remaining execution delay ${remainingDelay}ms")

        val handler = Handler(Looper.getMainLooper())
        
        // Safety Watchdog: force-restore layouts if the system fails to trigger the gesture callbacks
        val watchdog = Runnable {
            if (isHandlingTap) {
                Log.w(TAG, "Safety Watchdog triggered: Force-restoring system overlays")
                setOverlaysActive(true)
                isHandlingTap = false
            }
        }
        handler.postDelayed(watchdog, remainingDelay + 600L)

        // 1. Schedule simulated touch gesture on main thread with remaining custom delay
        handler.postDelayed({
            // 2. Hide and disable touches on all overlay windows to bypass Android's tapjacking filter
            setOverlaysActive(false)

            // 3. Wait 80ms for the WindowManager to apply layout changes and remove overlay input channels from screen layout
            handler.postDelayed({
                dispatchTapGesture(tapX, tapY) {
                    handler.removeCallbacks(watchdog)
                    // 4. Restore overlays after the tap gesture completes, giving it 80ms cooldown
                    handler.postDelayed({
                        setOverlaysActive(true)
                        isHandlingTap = false
                    }, 80L)
                }
            }, 80L)
        }, remainingDelay)
    }

    private fun dispatchTapGesture(rawX: Float, rawY: Float, onFinished: () -> Unit) {
        val path = Path().apply {
            moveTo(rawX, rawY)
            lineTo(rawX, rawY + 1.0f) // 1-pixel robust offset to make stroke path valid
        }
        val duration = 15L // Fast tap signature to trigger instant responses on backing apps
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        
        try {
            dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    onFinished()
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    onFinished()
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Dispatch gesture failed", e)
            onFinished()
        }
    }

    // --- Elegant Floating Toggle View ---
    inner class FloatingToggleView(context: Context) : View(context) {
        private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        private val paintIcon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 50f
        }

        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var touchDownTime = 0L

        fun updateState() {
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val radius = width / 2f
            val centerX = width / 2f
            val centerY = height / 2f

            val isEnabled = isDelayEnabled
            val startColor = if (isEnabled) Color.parseColor("#805AD5") else Color.parseColor("#4A5568")
            val endColor = if (isEnabled) Color.parseColor("#553C9A") else Color.parseColor("#2D3748")
            
            val gradient = android.graphics.LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                startColor, endColor,
                android.graphics.Shader.TileMode.CLAMP
            )
            paintBg.shader = gradient
            canvas.drawCircle(centerX, centerY, radius - 4f, paintBg)
            paintBg.shader = null

            paintBorder.color = if (isEnabled) Color.parseColor("#B794F4") else Color.parseColor("#718096")
            paintBorder.alpha = 220
            canvas.drawCircle(centerX, centerY, radius - 4f, paintBorder)

            val emoji = if (isEnabled) "⏳" else "⚡"
            val fontMetrics = paintIcon.fontMetrics
            val textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(emoji, centerX, textY, paintIcon)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val params = layoutParams as WindowManager.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchDownTime = android.os.SystemClock.uptimeMillis()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    params.x = (initialX + dx).roundToInt()
                    params.y = (initialY + dy).roundToInt()
                    windowManager.updateViewLayout(this, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = android.os.SystemClock.uptimeMillis() - touchDownTime
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
                    
                    if (elapsed < 300 && distance < 15) {
                        val nextState = !isDelayEnabled
                        updateDelayEnabled(context, nextState)
                        val msg = if (nextState) 
                            "تم تفعيل تأخير اللمس ⏳" 
                        else 
                            "تم تعطيل تأخير اللمس، النقرات مباشرة بالخلفية ⚡"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    // --- Custom Overlay Child View ---
    inner class OverlayCircleView(context: Context, private var mCircle: CircleOverlay) : View(context) {

        private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 28f
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        }

        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f

        private var isPressedDown = false

        // Interactive Live Countdown state
        private var countdownActive = false
        private var countdownRemainingMs = 0L
        private var maxCountdownMs = 1L
        private val countdownHandler = Handler(Looper.getMainLooper())
        private val countdownRunnable = object : Runnable {
            override fun run() {
                if (countdownActive) {
                    countdownRemainingMs -= 20L
                    if (countdownRemainingMs <= 0L) {
                        countdownActive = false
                        countdownRemainingMs = 0L
                    } else {
                        countdownHandler.postDelayed(this, 20L)
                    }
                    invalidate()
                }
            }
        }

        fun startCountdown(delayMs: Long) {
            if (delayMs <= 0) return
            countdownActive = true
            countdownRemainingMs = delayMs
            maxCountdownMs = delayMs
            countdownHandler.removeCallbacks(countdownRunnable)
            countdownHandler.post(countdownRunnable)
            invalidate()
        }

        fun stopCountdown() {
            countdownActive = false
            countdownRemainingMs = 0L
            countdownHandler.removeCallbacks(countdownRunnable)
            invalidate()
        }

        fun updateCircleProperties(newCircle: CircleOverlay) {
            mCircle = newCircle
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val radius = width / 2f
            val centerX = width / 2f
            val centerY = height / 2f

            val primaryColorVal = try {
                Color.parseColor(mCircle.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#3182CE")
            }

            // Glassmorphism Fill
            val whiteGlossAlpha = (0.35f * 255).roundToInt()
            val glassTopColor = Color.argb(whiteGlossAlpha, 255, 255, 255)
            
            val primaryAlphaVal = (mCircle.alpha * 255).roundToInt().coerceIn(0, 255)
            val glassBottomColor = Color.argb(
                primaryAlphaVal,
                Color.red(primaryColorVal),
                Color.green(primaryColorVal),
                Color.blue(primaryColorVal)
            )

            val glassGrad = android.graphics.LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                glassTopColor,
                glassBottomColor,
                android.graphics.Shader.TileMode.CLAMP
            )
            paintFill.shader = glassGrad
            canvas.drawCircle(centerX, centerY, radius - 6f, paintFill)
            paintFill.shader = null

            // Outline
            paintStroke.color = primaryColorVal
            if (isPositioningModeActive) {
                paintStroke.alpha = 255
            } else {
                paintStroke.alpha = 130
            }
            canvas.drawCircle(centerX, centerY, radius - 6f, paintStroke)

            // If countdown is active, draw a brilliant high-contrast scanning sweep progress ring!
            if (countdownActive && maxCountdownMs > 0L) {
                val progress = (countdownRemainingMs.toFloat() / maxCountdownMs.toFloat()).coerceIn(0f, 1f)
                val sweepAngle = progress * 360f
                val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 8f
                    color = Color.WHITE
                    strokeCap = Paint.Cap.ROUND
                }
                val arcBounds = android.graphics.RectF(6f, 6f, width - 6f, height - 6f)
                canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint)
            }

            // White top reflection rim
            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.WHITE
                alpha = 180
            }
            val rimRect = android.graphics.RectF(6f, 6f, width - 6f, height - 6f)
            canvas.drawArc(rimRect, 140f, 150f, false, rimPaint)

            // Inner light flare
            val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
                alpha = 80
            }
            canvas.drawCircle(centerX - radius * 0.35f, centerY - radius * 0.35f, radius * 0.16f, flarePaint)

            // Touch press feedback glow!
            if (isPressedDown && !isPositioningModeActive && isDelayEnabled) {
                val pressGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 10f
                    color = Color.argb(200, 255, 255, 255)
                }
                canvas.drawCircle(centerX, centerY, radius - 8f, pressGlowPaint)
            }

            // Typography text render
            if (isPositioningModeActive) {
                canvas.drawLine(centerX, centerY - 15f, centerX, centerY + 15f, paintStroke)
                canvas.drawLine(centerX - 15f, centerY, centerX + 15f, centerY, paintStroke)
                
                canvas.drawText("سحب (${mCircle.id})", centerX, centerY - radius * 0.35f, paintText)
                canvas.drawText("${mCircle.delayMs}ms", centerX, centerY + radius * 0.55f, paintText)
            } else {
                if (countdownActive) {
                    val seconds = countdownRemainingMs / 1000f
                    val tickLabel = String.format("%.2fث", seconds)
                    val countdownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 30f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                        setShadowLayer(6f, 0f, 3f, Color.BLACK)
                    }
                    canvas.drawText(tickLabel, centerX, centerY + 10f, countdownPaint)
                } else {
                    val fractionSec = String.format("%.2f", mCircle.delayMs / 1000.0)
                    val label = if (mCircle.labelText.isNotEmpty()) {
                        mCircle.labelText
                    } else {
                        "${fractionSec} ثانية"
                    }
                    
                    if (mCircle.alpha > 0.15f) {
                        canvas.drawText(label, centerX, centerY + 10f, paintText)
                    }
                }
            }
        }

        private var downTime = 0L
        private var isPendingTap = false
        private var tapX = 0f
        private var tapY = 0f

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val layoutParams = layoutParams as WindowManager.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val touchX = event.x
                    val touchY = event.y
                    val dx = touchX - centerX
                    val dy = touchY - centerY
                    val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val radius = width / 2f

                    if (distance <= radius) {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        downTime = android.os.SystemClock.uptimeMillis()
                        isPendingTap = true
                        isPressedDown = true
                        invalidate()
                        return true
                    } else {
                        return false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isPendingTap) {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        
                        val density = resources.displayMetrics.density
                        val threshold = 8 * density
                        
                        // If moved beyond threshold and positioning mode is active, it is a drag. Let's move the overlay dynamically
                        if (isPositioningModeActive && kotlin.math.sqrt((dx * dx + dy * dy).toDouble()) > threshold) {
                            layoutParams.x = (initialX + dx).roundToInt()
                            layoutParams.y = (initialY + dy).roundToInt()
                            try {
                                windowManager.updateViewLayout(this, layoutParams)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to Update overlay position as user dragged", e)
                            }
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (isPendingTap) {
                        isPendingTap = false
                        isPressedDown = false
                        invalidate()
                        
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        val density = resources.displayMetrics.density
                        val threshold = 8 * density
                        val isDrag = isPositioningModeActive && (kotlin.math.sqrt((dx * dx + dy * dy).toDouble()) > threshold)
                        
                        if (isDrag || isPositioningModeActive) {
                            // User dragged it: Save updated coordinate of the circle to the database
                            serviceScope.launch {
                                val updated = mCircle.copy(x = layoutParams.x, y = layoutParams.y)
                                repository.updateOverlay(updated)
                            }
                        } else {
                            // User tapped it: Delay click if enabled, tap instantly if disabled!
                            val elapsed = android.os.SystemClock.uptimeMillis() - downTime
                            if (isDelayEnabled) {
                                val remainingDelay = (mCircle.delayMs - elapsed).coerceAtLeast(0L)
                                if (remainingDelay > 0L) {
                                    startCountdown(remainingDelay)
                                }
                                handleDelayedTap(mCircle, initialTouchX, initialTouchY, remainingDelay)
                            } else {
                                handleDelayedTap(mCircle, initialTouchX, initialTouchY, 0L)
                            }
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isPendingTap = false
                    isPressedDown = false
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }
}
