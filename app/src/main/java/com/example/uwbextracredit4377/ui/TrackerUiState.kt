package com.example.uwbextracredit4377.ui

import com.example.uwbextracredit4377.data.TrackingMode

data class TrackerUiState(
    val mode: TrackingMode = TrackingMode.SIMULATED,
    val isLiveTrackingActive: Boolean = false,
    val isPhoneController: Boolean = false,
    val rawDistanceMeters: Float? = null,
    val smoothedDistanceMeters: Float? = null,
    val proximityZone: ProximityZone? = null,
    val distanceHistory: List<Float> = emptyList(),
    val localAddress: String = "34:12",
    val peerAddressInput: String = "00:00",
    val azimuthDegrees: Float? = null,
    val statusMessage: String = "Simulated Mode Active (Default)",
    val errorMessage: String? = null,
    val hasUwbHardware: Boolean = true
) {
    val distanceInCm: Int?
        get() = smoothedDistanceMeters?.let { (it * 100).toInt() }
}
