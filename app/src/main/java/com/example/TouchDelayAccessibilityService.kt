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

    companion object {
        private const val TAG = "TouchDelayService"
        
        @Volatile
        var isServiceRunning = false
            private set

        @Volatile
        var isPositioningModeActive = false
            private set

        private var serviceInstance: TouchDelayAccessibilityService? = null

        fun updatePositioningMode(active: Boolean) {
            isPositioningModeActive = active
            serviceInstance?.refreshOverlayLayouts()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val database = AppDatabase.getDatabase(this)
        repository = CircleRepository(database.circleOverlayDao())
        serviceInstance = this
        isServiceRunning = true
        Log.d(TAG, "Accessibility Service created")
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
                // If disabled, remove if visible
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

            // Standard flags: non-focusable so surrounding clicks pass through
            var flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            if (!isPositioningModeActive) {
                // In delay-touch mode, we catch clicks. So we need view to be touchable (no FLAG_NOT_TOUCHABLE).
                // Ensure overlays are interactive.
            } else {
                // In positioning mode, we need views to be touchable so we can drag them.
            }

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
     * Called when positioning mode changes. Forces all views to redraw and adapts layout params.
     */
    fun refreshOverlayLayouts() {
        serviceScope.launch {
            // Re-fetch all and trigger refresh
            repository.allOverlays.collectLatest { list ->
                updateOverlaysOnScreen(list)
                cancel() // Collect once and finish
            }
        }
    }

    /**
     * Handles tap interception and delayed tap simulation.
     */
    fun handleDelayedTap(circle: CircleOverlay, tapX: Float, tapY: Float) {
        if (isHandlingTap) return
        isHandlingTap = true
        Log.d(TAG, "Delaying tap at ($tapX, $tapY) for ${circle.delayMs}ms")

        // 1. Instantly make all overlays non-touchable so that the simulated tap goes to the window below!
        toggleOverlaysTouchability(touchable = false)

        // 2. Schedule simulated touch gesture on main thread with custom delay
        Handler(Looper.getMainLooper()).postDelayed({
            dispatchTapGesture(tapX, tapY) {
                // 3. Reactivate overlay touchability once gesture is printed
                toggleOverlaysTouchability(touchable = true)
                isHandlingTap = false
            }
        }, circle.delayMs)
    }

    private fun toggleOverlaysTouchability(touchable: Boolean) {
        activeOverlays.forEach { (_, view) ->
            try {
                val params = view.layoutParams as WindowManager.LayoutParams
                if (touchable) {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                } else {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                }
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed window touch flag toggle", e)
            }
        }
    }

    private fun dispatchTapGesture(rawX: Float, rawY: Float, onFinished: () -> Unit) {
        val path = Path().apply {
            moveTo(rawX, rawY)
        }
        val duration = 50L // Instant tap duration is 50ms
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

        fun updateCircleProperties(newCircle: CircleOverlay) {
            mCircle = newCircle
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val radius = width / 2f
            val centerX = width / 2f
            val centerY = height / 2f

            // Parse hex color or default to clean blue
            val primaryColorVal = try {
                Color.parseColor(mCircle.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#3182CE")
            }

            // --- Frosted Glassmorphism Fill ---
            // Gradient spanning top-left to bottom-right representing light shining on glassy surface
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

            // Draw glossy translucent main circle body
            canvas.drawCircle(centerX, centerY, radius - 6f, paintFill)
            
            // Clean up shader so general paint stays pristine
            paintFill.shader = null

            // --- Glass Reflection Border Rim ---
            paintStroke.color = primaryColorVal
            if (isPositioningModeActive) {
                paintStroke.alpha = 255
            } else {
                paintStroke.alpha = 130
            }
            // Draw main subtle translucent colored contour outline
            canvas.drawCircle(centerX, centerY, radius - 6f, paintStroke)

            // Top-Left Glossy Edge Reflection Rim (Refracted Light)
            val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.WHITE
                alpha = 180
            }
            val rimRect = android.graphics.RectF(6f, 6f, width - 6f, height - 6f)
            canvas.drawArc(rimRect, 140f, 150f, false, rimPaint)

            // Inner Specular Light Flare Spot
            val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
                alpha = 80
            }
            canvas.drawCircle(centerX - radius * 0.35f, centerY - radius * 0.35f, radius * 0.16f, flarePaint)

            // --- Typography ---
            if (isPositioningModeActive) {
                // Crosshair indicator
                canvas.drawLine(centerX, centerY - 15f, centerX, centerY + 15f, paintStroke)
                canvas.drawLine(centerX - 15f, centerY, centerX + 15f, centerY, paintStroke)
                
                // Overlay label Name
                canvas.drawText("سحب (${mCircle.id})", centerX, centerY - radius * 0.35f, paintText)
                canvas.drawText("${mCircle.delayMs}ms", centerX, centerY + radius * 0.55f, paintText)
            } else {
                // Delay textual display (Arabic fractions or simple labels)
                val fractionSec = String.format("%.2f", mCircle.delayMs / 1000.0)
                val label = if (mCircle.labelText.isNotEmpty()) {
                    mCircle.labelText
                } else {
                    "${fractionSec} ثانية"
                }
                
                // Draw inside circle overlay if opacity is not too small
                if (mCircle.alpha > 0.15f) {
                    canvas.drawText(label, centerX, centerY + 10f, paintText)
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (isPositioningModeActive) {
                val layoutParams = layoutParams as WindowManager.LayoutParams
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        layoutParams.x = (initialX + dx).roundToInt()
                        layoutParams.y = (initialY + dy).roundToInt()
                        windowManager.updateViewLayout(this, layoutParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // Persist the new position coordinates to the Room SQLite Database!
                        serviceScope.launch {
                            val updated = mCircle.copy(x = layoutParams.x, y = layoutParams.y)
                            repository.updateOverlay(updated)
                        }
                        return true
                    }
                }
            } else {
                // Active Delay Touch Mode! Intercept on Action Down
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val rawX = event.rawX
                    val rawY = event.rawY
                    handleDelayedTap(mCircle, rawX, rawY)
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }
}
