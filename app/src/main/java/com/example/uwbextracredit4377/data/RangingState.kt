package com.example.uwbextracredit4377.data

sealed interface RangingState {
    data object Idle : RangingState

    data class Active(
        val distanceMeters: Float,
        val azimuthDegrees: Float? = null,
        val elevationDegrees: Float? = null,
        val peerAddress: String,
        val localAddress: String,
        val isSimulated: Boolean = false,
        val timestampNanos: Long = System.nanoTime()
    ) : RangingState

    data class PeerDisconnected(
        val peerAddress: String,
        val localAddress: String
    ) : RangingState

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : RangingState
}
