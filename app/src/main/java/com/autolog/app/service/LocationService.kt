package com.autolog.app.service

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.autolog.app.AutoLogApplication
import com.autolog.app.data.model.TripEntry
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LocationService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "autolog_location"
        const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var fusedClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var startLocation: Location? = null
    private var totalDistance = 0f
    private var startTime = 0L
    private var activeVehicleId = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val prefs = getSharedPreferences("autolog_prefs", MODE_PRIVATE)
                activeVehicleId = prefs.getInt("active_vehicle_id", -1)
                startTracking()
            }
            ACTION_STOP -> stopTracking()
            // ИСПРАВЛЕНО: если система перезапустила сервис без intent — останавливаемся,
            // нет смысла трекать без знания vehicleId
            null -> stopSelf()
        }
        // ИСПРАВЛЕНО: START_NOT_STICKY — система не будет перезапускать сервис
        // без явного ACTION_START от BluetoothReceiver
        return START_NOT_STICKY
    }

    private fun startTracking() {
        createNotificationChannel()
        val notification = buildNotification("Запись поездки...")
        startForeground(NOTIFICATION_ID, notification)

        val prefs = getSharedPreferences("autolog_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("is_tracking", true).apply()

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        totalDistance = 0f
        startTime = System.currentTimeMillis()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (startLocation == null) {
                        startLocation = location
                    } else {
                        totalDistance += startLocation!!.distanceTo(location)
                        startLocation = location
                    }
                    val km = totalDistance / 1000
                    updateNotification("Поездка: %.1f км".format(km))
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedClient?.requestLocationUpdates(
                request, locationCallback!!, Looper.getMainLooper()
            )
        }
    }

    private fun stopTracking() {
        val prefs = getSharedPreferences("autolog_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("is_tracking", false).apply()

        locationCallback?.let { fusedClient?.removeLocationUpdates(it) }

        if (totalDistance > 100 && activeVehicleId != -1) {
            val km = (totalDistance / 1000).toDouble()
            val duration = ((System.currentTimeMillis() - startTime) / 60000).toInt()

            serviceScope.launch {
                try {
                    val app = application as AutoLogApplication
                    val dao = app.database.autoLogDao()

                    // ИСПРАВЛЕНО: читаем одометр из Room, а не из SharedPreferences
                    val vehicle = dao.getVehicleById(activeVehicleId)
                    val currentOdo = vehicle?.currentOdometer?.toInt() ?: 0
                    val newOdo = currentOdo + km.toInt()

                    dao.insertTripEntry(
                        TripEntry(
                            vehicleId = activeVehicleId,
                            startOdometer = currentOdo,
                            endOdometer = newOdo,
                            distanceKm = km,
                            durationMinutes = duration
                        )
                    )

                    // ИСПРАВЛЕНО: обновляем одометр в Room через DAO
                    dao.addOdometerDistance(activeVehicleId, km)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "GPS Трекинг", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoLog")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
