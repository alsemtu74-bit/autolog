package com.autolog.app.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.autolog.app.service.LocationService

class BluetoothReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AutoLog_BT"

        fun normalizeName(name: String): String {
            return name.replace(" [LE]", "").replace("[LE]", "").trim()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        val deviceName = try {
            device?.name
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            null
        }

        Log.d(TAG, "Device name: $deviceName")

        val prefs = context.getSharedPreferences("autolog_prefs", Context.MODE_PRIVATE)
        val savedDevice = prefs.getString("bluetooth_device", "")
        val allPrefs = prefs.all
        val btDevices = allPrefs.entries
            .filter { it.key.startsWith("bt_device_") }
            .map { it.value as? String }
            .filterNotNull()

        Log.d(TAG, "Saved BT device: $savedDevice")
        Log.d(TAG, "All BT devices: $btDevices")

        fun isMatch(name: String): Boolean {
            val normalizedName = normalizeName(name)
            return btDevices.any { normalizeName(it).equals(normalizedName, ignoreCase = true) } ||
                    normalizeName(savedDevice ?: "").equals(normalizedName, ignoreCase = true)
        }

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.d(TAG, "BT Connected: $deviceName")
                if (deviceName != null && isMatch(deviceName)) {
                    Log.d(TAG, "Starting LocationService for: $deviceName")
                    val vehicleEntry = allPrefs.entries
                        .firstOrNull { it.key.startsWith("bt_device_") &&
                                normalizeName(it.value as? String ?: "")
                                    .equals(normalizeName(deviceName), ignoreCase = true) }
                    val vehicleId = vehicleEntry?.key?.removePrefix("bt_device_")?.toIntOrNull() ?: -1
                    prefs.edit().putInt("active_vehicle_id", vehicleId).apply()
                    val serviceIntent = Intent(context, LocationService::class.java)
                    serviceIntent.action = LocationService.ACTION_START
                    context.startForegroundService(serviceIntent)
                } else {
                    Log.d(TAG, "Device $deviceName not matched, skipping")
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.d(TAG, "BT Disconnected: $deviceName")
                if (deviceName != null && isMatch(deviceName)) {
                    Log.d(TAG, "Stopping LocationService")
                    val serviceIntent = Intent(context, LocationService::class.java)
                    serviceIntent.action = LocationService.ACTION_STOP
                    context.startService(serviceIntent)
                }
            }
        }
    }
}