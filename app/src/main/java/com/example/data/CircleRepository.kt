package com.example.data

import kotlinx.coroutines.flow.Flow

class CircleRepository(private val circleOverlayDao: CircleOverlayDao) {
    val allOverlays: Flow<List<CircleOverlay>> = circleOverlayDao.getAllOverlays()

    suspend fun getOverlayById(id: Int): CircleOverlay? {
        return circleOverlayDao.getOverlayById(id)
    }

    suspend fun insertOverlay(overlay: CircleOverlay): Long {
        return circleOverlayDao.insertOverlay(overlay)
    }

    suspend fun updateOverlay(overlay: CircleOverlay) {
        circleOverlayDao.updateOverlay(overlay)
    }

    suspend fun deleteOverlay(overlay: CircleOverlay) {
        circleOverlayDao.deleteOverlay(overlay)
    }

    suspend fun clearAll() {
        circleOverlayDao.deleteAll()
    }
}
