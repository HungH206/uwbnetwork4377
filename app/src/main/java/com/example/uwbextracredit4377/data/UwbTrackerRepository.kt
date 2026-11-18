package com.example.uwbextracredit4377.data

import android.content.Context
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbDevice
import androidx.core.uwb.UwbManager
import androidx.core.uwb.exceptions.UwbHardwareNotAvailableException
import androidx.core.uwb.exceptions.UwbServiceNotAvailableException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlin.math.sin

enum class TrackingMode {
    SIMULATED,
    LIVE_HARDWARE
}

class UwbTrackerRepository(
    private val context: Context
) {
    /**
     * Converts a 2-byte ByteArray into a standard formatted hex string "XX:XX".
     */
    fun formatAddress(bytes: ByteArray): String {
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Parses a string formatted as "XX:XX" or "XXXX" into a 2-byte ByteArray.
     * Falls back to [0x00, 0x00] if parsing fails.
     */
    fun parseAddress(addressStr: String): ByteArray {
        val clean = addressStr.replace(":", "").trim()
        if (clean.length == 4) {
            val b0 = clean.substring(0, 2).toIntOrNull(16)
            val b1 = clean.substring(2, 4).toIntOrNull(16)
            if (b0 != null && b1 != null) {
                return byteArrayOf(b0.toByte(), b1.toByte())
            }
        }
        return byteArrayOf(0x00, 0x00)
    }

    /**
     * Main tracking Flow that switches between Simulated mode and Live Hardware mode.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun startRanging(
        mode: TrackingMode,
        peerAddressStr: String = "00:00",
        asController: Boolean = false
    ): Flow<RangingState> = flow {
        when (mode) {
            TrackingMode.SIMULATED -> {
                emitAll(createSimulatedFlow(peerAddressStr))
            }
            TrackingMode.LIVE_HARDWARE -> {
                emitAll(createLiveHardwareFlow(peerAddressStr, asController))
            }
        }
    }

    /**
     * Emits smooth distance readings cycling between 0.3m and 3.5m at 5 Hz (every 200ms).
     */
    fun createSimulatedFlow(peerAddressStr: String = "00:00"): Flow<RangingState> = flow {
        val localAddress = "34:12"
        val cleanPeerAddress = formatAddress(parseAddress(peerAddressStr))
        var step = 0.0

        // Cycling smoothly between 0.3m and 3.5m:
        // Midpoint = (3.5 + 0.3) / 2 = 1.9m
        // Amplitude = (3.5 - 0.3) / 2 = 1.6m
        // Sine wave: distance = 1.9 + 1.6 * sin(step)
        while (currentCoroutineContext().isActive) {
            val distance = (1.9 + 1.6 * sin(step)).toFloat()
            // Add a tiny pseudo-random variation (+-2cm) to simulate realistic sensor jitter
            val jitter = (((step.hashCode() % 100) / 100.0f) - 0.5f) * 0.04f
            val distanceWithJitter = (distance + jitter).coerceIn(0.3f, 3.5f)

            emit(
                RangingState.Active(
                    distanceMeters = distanceWithJitter,
                    azimuthDegrees = (sin(step * 0.5) * 35.0).toFloat(),
                    peerAddress = cleanPeerAddress,
                    localAddress = localAddress,
                    isSimulated = true
                )
            )
            step += 0.15
            delay(200) // 5 Hz
        }
    }

    /**
     * Live Hardware Mode:
     * Supports both Controller (Initiator) and Controlee (Responder) roles.
     */
    private fun createLiveHardwareFlow(
        peerAddressStr: String,
        asController: Boolean = false
    ): Flow<RangingState> = flow {
        val uwbManager = UwbManager.createInstance(context)
        val sessionScope = try {
            if (asController) {
                uwbManager.controllerSessionScope()
            } else {
                uwbManager.controleeSessionScope()
            }
        } catch (e: UwbHardwareNotAvailableException) {
            emit(RangingState.Error("UWB hardware is not available on this device.", e))
            return@flow
        } catch (e: UwbServiceNotAvailableException) {
            emit(RangingState.Error("UWB service is disabled or unavailable. Please enable UWB in system settings.", e))
            return@flow
        } catch (e: SecurityException) {
            emit(RangingState.Error("UWB permission denied. Missing UWB_RANGING permission.", e))
            return@flow
        } catch (e: Exception) {
            emit(RangingState.Error("Failed to initialize UWB session: ${e.message}", e))
            return@flow
        }

        val localAddressFormatted = formatAddress(sessionScope.localAddress.address)
        val peerAddressBytes = parseAddress(peerAddressStr)
        val peerAddressFormatted = formatAddress(peerAddressBytes)

        android.util.Log.d("UWB_LIVE", "Session as Controller: $asController | Local: $localAddressFormatted | Peer: $peerAddressFormatted")

        // Immediately emit Opened state so UI immediately shows the real hardware MAC address!
        emit(RangingState.Opened(localAddress = localAddressFormatted, peerAddress = peerAddressFormatted))

        // FiRa unicast DS-TWR parameters matching Qorvo QM33 UCI specification:
        // Vendor ID 0x0708 in little-endian (0x08, 0x07) followed by STATIC_STS_IV (0x01..0x06)
        val staticStsKey = byteArrayOf(
            0x08.toByte(), 0x07.toByte(), // Vendor ID 0x0708
            0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte() // STATIC_STS_IV
        )

        val parameters = RangingParameters(
            uwbConfigType = RangingParameters.CONFIG_UNICAST_DS_TWR,
            sessionId = 42,
            subSessionId = 0,
            sessionKeyInfo = staticStsKey,
            subSessionKeyInfo = null,
            complexChannel = UwbComplexChannel(channel = 9, preambleIndex = 9),
            peerDevices = listOf(UwbDevice(UwbAddress(peerAddressBytes))),
            updateRateType = RangingParameters.RANGING_UPDATE_RATE_FREQUENT
        )

        while (currentCoroutineContext().isActive) {
            try {
                sessionScope.prepareSession(parameters)
                    .mapNotNull { result ->
                        android.util.Log.d("UWB_LIVE", "Ranging Result received: $result")
                        when (result) {
                            is RangingResult.RangingResultPosition -> {
                                val distanceMeasurement = result.position.distance
                                if (distanceMeasurement != null) {
                                    android.util.Log.d("UWB_LIVE", "Distance: ${distanceMeasurement.value} m")
                                    RangingState.Active(
                                        distanceMeters = distanceMeasurement.value,
                                        azimuthDegrees = result.position.azimuth?.value,
                                        elevationDegrees = result.position.elevation?.value,
                                        peerAddress = formatAddress(result.device.address.address),
                                        localAddress = localAddressFormatted,
                                        isSimulated = false,
                                        timestampNanos = result.position.elapsedRealtimeNanos
                                    )
                                } else {
                                    null
                                }
                            }
                            is RangingResult.RangingResultPeerDisconnected -> {
                                android.util.Log.d("UWB_LIVE", "Peer Disconnected: ${formatAddress(result.device.address.address)}")
                                RangingState.PeerDisconnected(
                                    peerAddress = formatAddress(result.device.address.address),
                                    localAddress = localAddressFormatted
                                )
                            }
                            else -> null
                        }
                    }
                    .catch { cause ->
                        android.util.Log.e("UWB_LIVE", "Session error: ${cause.message}", cause)
                        emit(RangingState.Error("UWB session error: ${cause.message}", cause))
                    }
                    .collect { rangingState ->
                        emit(rangingState)
                    }
            } catch (e: Exception) {
                android.util.Log.e("UWB_LIVE", "Exception preparing session: ${e.message}", e)
                emit(RangingState.Error("Error during UWB ranging session: ${e.message}", e))
                break
            }
        }
    }
}

private suspend inline fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(flow: Flow<T>) {
    flow.collect { value -> emit(value) }
}
