package com.example.uwbextracredit4377.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uwbextracredit4377.data.RangingState
import com.example.uwbextracredit4377.data.TrackingMode
import com.example.uwbextracredit4377.data.UwbTrackerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrackerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = UwbTrackerRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    // 5-sample rolling buffer for jitter filtering
    private val rollingWindow = ArrayDeque<Float>(5)
    // Distance history for the live sparkline/bar indicator (last 20 samples)
    private val historyPoints = ArrayDeque<Float>(20)

    private var rangingJob: Job? = null

    init {
        // Start in Simulated Mode by default on launch
        startTracking(TrackingMode.SIMULATED)
    }

    fun setPeerAddress(address: String) {
        _uiState.update { it.copy(peerAddressInput = address) }
        // If live tracking is running, restart session with the new peer address
        if (_uiState.value.mode == TrackingMode.LIVE_HARDWARE) {
            startTracking(TrackingMode.LIVE_HARDWARE)
        }
    }

    fun toggleMode(useLive: Boolean) {
        val newMode = if (useLive) TrackingMode.LIVE_HARDWARE else TrackingMode.SIMULATED
        startTracking(newMode, _uiState.value.isPhoneController)
    }

    fun togglePhoneRole(asController: Boolean) {
        _uiState.update { it.copy(isPhoneController = asController) }
        if (_uiState.value.mode == TrackingMode.LIVE_HARDWARE) {
            startTracking(TrackingMode.LIVE_HARDWARE, asController)
        }
    }

    fun fallbackToSimulated(reason: String) {
        _uiState.update {
            it.copy(
                mode = TrackingMode.SIMULATED,
                errorMessage = reason
            )
        }
        startTracking(TrackingMode.SIMULATED, false)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun startTracking(mode: TrackingMode, asController: Boolean = false) {
        rangingJob?.cancel()
        rollingWindow.clear()

        _uiState.update { current ->
            current.copy(
                mode = mode,
                isLiveTrackingActive = mode == TrackingMode.LIVE_HARDWARE,
                isPhoneController = asController,
                statusMessage = if (mode == TrackingMode.SIMULATED) {
                    "Simulated Mode Active (0.3m – 3.5m cycling)"
                } else {
                    "Connecting to Qorvo DWM3001CDK (${if (asController) "Phone: Controller" else "Phone: Controlee"})..."
                },
                errorMessage = null
            )
        }

        rangingJob = viewModelScope.launch {
            repository.startRanging(mode, _uiState.value.peerAddressInput, asController)
                .collect { state ->
                    when (state) {
                        is RangingState.Opened -> {
                            _uiState.update { current ->
                                current.copy(
                                    localAddress = state.localAddress,
                                    statusMessage = "Session Opened. Waiting for Qorvo peer..."
                                )
                            }
                        }

                        is RangingState.Active -> {
                            // Update 5-sample rolling window
                            if (rollingWindow.size >= 5) {
                                rollingWindow.removeFirst()
                            }
                            rollingWindow.addLast(state.distanceMeters)

                            val smoothedDistance = rollingWindow.average().toFloat()
                            val zone = ProximityZone.fromDistance(smoothedDistance)

                            // Keep last 20 samples for sparkline visualization
                            if (historyPoints.size >= 20) {
                                historyPoints.removeFirst()
                            }
                            historyPoints.addLast(smoothedDistance)

                            _uiState.update { current ->
                                current.copy(
                                    rawDistanceMeters = state.distanceMeters,
                                    smoothedDistanceMeters = smoothedDistance,
                                    proximityZone = zone,
                                    distanceHistory = historyPoints.toList(),
                                    localAddress = state.localAddress,
                                    azimuthDegrees = state.azimuthDegrees,
                                    statusMessage = if (state.isSimulated) {
                                        "Simulated Ranging (5 Hz active)"
                                    } else {
                                        "Live Qorvo Ranging Active (Peer: ${state.peerAddress})"
                                    },
                                    errorMessage = null
                                )
                            }
                        }

                        is RangingState.PeerDisconnected -> {
                            _uiState.update { current ->
                                current.copy(
                                    localAddress = state.localAddress,
                                    statusMessage = "Peer disconnected (${state.peerAddress})",
                                    errorMessage = "Peer ${state.peerAddress} disconnected."
                                )
                            }
                        }

                        is RangingState.Error -> {
                            _uiState.update { current ->
                                current.copy(
                                    statusMessage = "Tracking Error",
                                    errorMessage = state.message,
                                    mode = TrackingMode.SIMULATED
                                )
                            }
                            // Gracefully fallback to simulated mode on hardware/system error
                            startTracking(TrackingMode.SIMULATED)
                        }

                        is RangingState.Idle -> {
                            // No-op
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        rangingJob?.cancel()
    }
}
