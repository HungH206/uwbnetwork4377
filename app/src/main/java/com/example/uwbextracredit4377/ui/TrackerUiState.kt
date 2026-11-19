package com.example.uwbextracredit4377.ui

/**
 * UI State for UWB Tracker Screen.
 * Purely hardware-driven: only shows measurements when actively connected to Qorvo board.
 */
data class TrackerUiState(
    val isLiveTrackingActive: Boolean = false,
    val isConnected: Boolean = false,
    val rawDistanceMeters: Float? = null,
    val smoothedDistanceMeters: Float? = null,
    val proximityZone: ProximityZone? = null,
    val distanceHistory: List<Float> = emptyList(),
    val localAddress: String = "--:--",
    val peerAddressInput: String = "00:00",
    val azimuthDegrees: Float? = null,
    val statusMessage: String = "Tracking Inactive",
    val errorMessage: String? = null,
    val hasUwbHardware: Boolean = true
) {
    val distanceInCm: Int?
        get() = smoothedDistanceMeters?.let { (it * 100).toInt() }
}
