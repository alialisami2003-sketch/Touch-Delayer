package com.example.ui

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TouchDelayAccessibilityService
import com.example.data.AppDatabase
import com.example.data.CircleOverlay
import com.example.data.CircleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CircleViewModel(application: Application) : AndroidViewModel(application) {

    private val _additionHistory = MutableStateFlow<List<Int>>(emptyList())
    val additionHistory: StateFlow<List<Int>> = _additionHistory.asStateFlow()

    private val _undoDelaySeconds = MutableStateFlow(5) // Default 5 seconds
    val undoDelaySeconds: StateFlow<Int> = _undoDelaySeconds.asStateFlow()

    private val _isUndoCountingDown = MutableStateFlow(false)
    val isUndoCountingDown: StateFlow<Boolean> = _isUndoCountingDown.asStateFlow()

    private val _undoCountdownLeft = MutableStateFlow(0)
    val undoCountdownLeft: StateFlow<Int> = _undoCountdownLeft.asStateFlow()

    private var undoJob: Job? = null

    private val repository: CircleRepository
    
    // List of circles from Room database
    val circles: StateFlow<List<CircleOverlay>>

    // Service active state
    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    // Screen overlay permission state
    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    // Positioning mode state (whether circles are in editable/drag mode)
    private val _isPositioningMode = MutableStateFlow(false)
    val isPositioningMode: StateFlow<Boolean> = _isPositioningMode.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CircleRepository(database.circleOverlayDao())
        
        circles = repository.allOverlays.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        _isPositioningMode.value = TouchDelayAccessibilityService.isPositioningModeActive
        
        // Start periodic permission and service status checker
        startPermissionStatusChecker()
    }

    /**
     * Periodically checks the status of Overlay and Accessibility Service.
     */
    private fun startPermissionStatusChecker() {
        viewModelScope.launch {
            while (true) {
                checkRealtimeStatuses()
                delay(1500) // Check every 1.5 seconds to refresh permission indicators dynamically
            }
        }
    }

    fun checkRealtimeStatuses() {
        val context = getApplication<Application>()
        _isServiceActive.value = TouchDelayAccessibilityService.isServiceRunning
        _isOverlayPermissionGranted.value = Settings.canDrawOverlays(context)
    }

    /**
     * Toggles positioning mode. If true, the user can drag circles around on their screen.
     */
    fun togglePositioningMode() {
        val nextVal = !_isPositioningMode.value
        _isPositioningMode.value = nextVal
        TouchDelayAccessibilityService.updatePositioningMode(nextVal)
    }

    /**
     * Inserts a new circle at default location with customized delays.
     */
    fun addNewCircle(widthPx: Int = 1080) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = circles.value.size
            val screenX = (widthPx / 2) - 150
            val newOverlay = CircleOverlay(
                name = "دائرة ${count + 1}",
                x = screenX.coerceAtLeast(100),
                y = 800 + (count * 150),
                radius = 50,
                delayMs = 250, // default quarter of a second
                alpha = 0.35f,
                colorHex = getRandomColorHex(count),
                isEnabled = true
            )
            val generatedId = repository.insertOverlay(newOverlay)
            _additionHistory.value = _additionHistory.value + generatedId.toInt()
        }
    }

    fun setUndoDelaySeconds(seconds: Int) {
        _undoDelaySeconds.value = seconds.coerceIn(1, 60)
    }

    fun triggerUndo() {
        if (_additionHistory.value.isEmpty()) return
        if (_isUndoCountingDown.value) return

        undoJob = viewModelScope.launch(Dispatchers.Main) {
            _isUndoCountingDown.value = true
            val totalSeconds = _undoDelaySeconds.value
            _undoCountdownLeft.value = totalSeconds

            for (i in totalSeconds downTo 1) {
                _undoCountdownLeft.value = i
                delay(1000)
            }

            val currentList = _additionHistory.value
            if (currentList.isNotEmpty()) {
                val lastId = currentList.last()
                _additionHistory.value = currentList.dropLast(1)
                
                launch(Dispatchers.IO) {
                    val list = circles.value
                    val targetCircle = list.find { it.id == lastId }
                    if (targetCircle != null) {
                        repository.deleteOverlay(targetCircle)
                    }
                }
            }
            _isUndoCountingDown.value = false
        }
    }

    fun cancelUndo() {
        undoJob?.cancel()
        undoJob = null
        _isUndoCountingDown.value = false
        _undoCountdownLeft.value = 0
    }

    private fun getRandomColorHex(index: Int): String {
        val list = listOf("#3182CE", "#38A169", "#DD6B20", "#E53E3E", "#805AD5", "#D53F8C")
        return list[index % list.size]
    }

    /**
     * Deletes a circle.
     */
    fun deleteCircle(overlay: CircleOverlay) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOverlay(overlay)
        }
    }

    /**
     * Updates an overlay's delays, radius, transparency, name, color, isEnabled, text labels.
     */
    fun updateCircle(overlay: CircleOverlay) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOverlay(overlay)
        }
    }

    /**
     * Resets database to factory templates or clears it.
     */
    fun clearAllCircles() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
        }
    }
}
