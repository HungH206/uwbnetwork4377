package com.example.uwbextracredit4377.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uwbextracredit4377.data.RangingState
import com.example.uwbextracredit4377.data.UwbTrackerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrackerViewModel(
    private val repository: UwbTrackerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    private var rangingJob: Job? = null

    // Rolling window buffer of size 5 for noise reduction
    private val rollingWindow = ArrayDeque<Float>(5)

    // Sparkline history buffer of 20 samples
    private val historyPoints = ArrayDeque<Float>(20)

    init {
        // Automatically check UWB hardware capability and start session
        _uiState.update {
            it.copy(
                hasUwbHardware = repository.isUwbSupported(),
                isLiveTrackingActive = true
            )
        }
        startTracking()
    }

    fun setPeerAddress(newAddress: String) {
        _uiState.update { it.copy(peerAddressInput = newAddress) }
        if (_uiState.value.isLiveTrackingActive) {
            startTracking()
        }
    }

    fun toggleTracking(isActive: Boolean) {
        if (isActive) {
            startTracking()
        } else {
            stopTracking()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun stopTracking() {
        rangingJob?.cancel()
        rollingWindow.clear()
        historyPoints.clear()

        _uiState.update {
            it.copy(
                isLiveTrackingActive = false,
                isConnected = false,
                rawDistanceMeters = null,
                smoothedDistanceMeters = null,
                proximityZone = null,
                distanceHistory = emptyList(),
                statusMessage = "Tracking Paused",
                errorMessage = null
            )
        }
    }

    private fun startTracking() {
        rangingJob?.cancel()
        rollingWindow.clear()
        historyPoints.clear()

        _uiState.update { current ->
            current.copy(
                isLiveTrackingActive = true,
                isConnected = false,
                rawDistanceMeters = null,
                smoothedDistanceMeters = null,
                proximityZone = null,
                distanceHistory = emptyList(),
                statusMessage = "Session Initializing...",
                errorMessage = null
            )
        }

        rangingJob = viewModelScope.launch {
            repository.startRanging(_uiState.value.peerAddressInput, asController = true)
                .collect { state ->
                    when (state) {
                        is RangingState.Opened -> {
                            _uiState.update { current ->
                                current.copy(
                                    localAddress = state.localAddress,
                                    statusMessage = "Session Active • Waiting for Qorvo Board..."
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
                                    isConnected = true,
                                    rawDistanceMeters = state.distanceMeters,
                                    smoothedDistanceMeters = smoothedDistance,
                                    proximityZone = zone,
                                    distanceHistory = historyPoints.toList(),
                                    localAddress = state.localAddress,
                                    azimuthDegrees = state.azimuthDegrees,
                                    statusMessage = "Live Qorvo Ranging Active (Peer: ${state.peerAddress})",
                                    errorMessage = null
                                )
                            }
                        }

                        is RangingState.PeerDisconnected -> {
                            rollingWindow.clear()
                            _uiState.update { current ->
                                current.copy(
                                    isConnected = false,
                                    rawDistanceMeters = null,
                                    smoothedDistanceMeters = null,
                                    proximityZone = null,
                                    statusMessage = "Qorvo Board Disconnected (Session Ready)",
                                    errorMessage = null
                                )
                            }
                        }

                        is RangingState.Error -> {
                            _uiState.update { current ->
                                current.copy(
                                    isConnected = false,
                                    statusMessage = "Hardware Error",
                                    errorMessage = state.message
                                )
                            }
                        }

                        RangingState.Idle -> {
                            _uiState.update { current ->
                                current.copy(
                                    isConnected = false,
                                    statusMessage = "Idle"
                                )
                            }
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
