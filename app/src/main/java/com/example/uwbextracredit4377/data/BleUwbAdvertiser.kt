package com.example.uwbextracredit4377.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID

/**
 * Advertises the Phone's dynamic UWB MAC address over Bluetooth Low Energy (BLE)
 * using custom Service UUID 00004377-0000-1000-8000-00805F9B34FB.
 *
 * This allows automatic zero-touch discovery by the Python auto-tracker script!
 */
class BleUwbAdvertiser(private val context: Context) {

    companion object {
        private const val TAG = "BLE_UWB_ADV"
        val UWB_SERVICE_UUID: UUID = UUID.fromString("00004377-0000-1000-8000-00805F9B34FB")
        val PARCEL_UUID = ParcelUuid(UWB_SERVICE_UUID)
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var advertiser: BluetoothLeAdvertiser? = null

    private var activeCallback: AdvertiseCallback? = null

    fun startAdvertising(uwbAddressBytes: ByteArray) {
        stopAdvertising()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth is disabled or not supported.")
            return
        }

        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.w(TAG, "BluetoothLeAdvertiser is not supported on this device.")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0) // Indefinite
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(PARCEL_UUID)
            // Put the exact 2-byte UWB MAC address in the service data payload!
            .addServiceData(PARCEL_UUID, uwbAddressBytes)
            .setIncludeDeviceName(false)
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "BLE advertising started successfully for UWB MAC: ${uwbAddressBytes.joinToString(":") { "%02X".format(it) }}")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "BLE advertising failed with error code: $errorCode")
            }
        }

        activeCallback = callback
        try {
            advertiser?.startAdvertising(settings, data, callback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLUETOOTH_ADVERTISE permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting advertising: ${e.message}", e)
        }
    }

    fun stopAdvertising() {
        try {
            activeCallback?.let { callback ->
                advertiser?.stopAdvertising(callback)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException stopping advertising: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping advertising: ${e.message}")
        } finally {
            activeCallback = null
        }
    }
}
