package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "circle_overlays")
data class CircleOverlay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val x: Int = 400,          // Position X in pixels
    val y: Int = 600,          // Position Y in pixels
    val radius: Int = 50,      // Radius in dp
    val delayMs: Long = 250,   // Delay duration in ms (default 250ms i.e. 1/4th second)
    val alpha: Float = 0.35f,  // Translucency (opacity from 0.05f to 0.95f)
    val colorHex: String = "#3182CE", // Default color accent (e.g., clean modern blue)
    val isEnabled: Boolean = true, // Master active flag for this individual circle
    val labelText: String = "" // Custom label to print inside or nearby
)
