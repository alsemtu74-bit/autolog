package com.autolog.app

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.autolog.app.data.AppDatabase
import com.autolog.app.data.AutoLogRepository
import com.autolog.app.receiver.BluetoothReceiver
import com.autolog.app.service.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoLogApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AutoLogRepository(database.autoLogDao()) }

    private val bluetoothReceiver = BluetoothReceiver()
    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        registerBluetoothReceiver()
        // ДОБАВЛЕНО: проверяем уже подключённые BT устройства при старте
        checkAlreadyConnectedDevices()
    }

    private fun registerBluetoothReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            registerReceiver(bluetoothReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ДОБАВЛЕНО: если телефон уже подключён к BT устройству машины при старте —
    // сразу запускаем трекинг не дожидаясь события ACL_CONNECTED
    fun checkAlreadyConnectedDevices() {
        appScope.launch {
            try {
                // Проверяем разрешение
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(
                        this@AutoLogApplication,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                } else true

                if (!hasPermission) return@launch

                val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    ?: return@launch
                val adapter = btManager.adapter ?: return@launch

                // Читаем сохранённые BT устройства из prefs
                val prefs = getSharedPreferences("autolog_prefs", Context.MODE_PRIVATE)
                val savedDevice = prefs.getString("bluetooth_device", "") ?: ""
                val allPrefs = prefs.all
                val btDevices = allPrefs.entries
                    .filter { it.key.startsWith("bt_device_") }
                    .mapNotNull { it.value as? String }

                if (btDevices.isEmpty() && savedDevice.isEmpty()) return@launch

                // Получаем список подключённых устройств
                val connectedDevices = btManager.getConnectedDevices(BluetoothProfile.GATT) +
                        btManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)

                // Также проверяем bonded devices которые могут быть подключены через A2DP
                val bondedAndConnected = adapter.bondedDevices?.filter { device ->
                    try {
                        // Проверяем через reflection если устройство подключено
                        val method = device.javaClass.getMethod("isConnected")
                        method.invoke(device) as? Boolean ?: false
                    } catch (e: Exception) {
                        false
                    }
                } ?: emptyList()

                val allConnected = (connectedDevices + bondedAndConnected).distinctBy {
                    try { it.address } catch (e: Exception) { "" }
                }

                for (device in allConnected) {
                    val deviceName = try { device.name } catch (e: SecurityException) { null }
                        ?: continue

                    val normalizedName = BluetoothReceiver.normalizeName(deviceName)

                    // Проверяем совпадает ли с сохранённым устройством
                    val isMatch = btDevices.any {
                        BluetoothReceiver.normalizeName(it).equals(normalizedName, ignoreCase = true)
                    } || BluetoothReceiver.normalizeName(savedDevice).equals(normalizedName, ignoreCase = true)

                    if (isMatch) {
                        // Находим vehicleId для этого устройства
                        val vehicleEntry = allPrefs.entries.firstOrNull {
                            it.key.startsWith("bt_device_") &&
                                    BluetoothReceiver.normalizeName(it.value as? String ?: "")
                                        .equals(normalizedName, ignoreCase = true)
                        }
                        val vehicleId = vehicleEntry?.key?.removePrefix("bt_device_")?.toIntOrNull() ?: -1
                        prefs.edit().putInt("active_vehicle_id", vehicleId).apply()

                        // Запускаем LocationService если ещё не запущен
                        val isTracking = prefs.getBoolean("is_tracking", false)
                        if (!isTracking) {
                            val serviceIntent = Intent(this@AutoLogApplication, LocationService::class.java)
                            serviceIntent.action = LocationService.ACTION_START
                            try {
                                startForegroundService(serviceIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
