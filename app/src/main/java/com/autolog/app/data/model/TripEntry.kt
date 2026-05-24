package com.autolog.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_entries")
data class TripEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vehicleId: Int,
    val startOdometer: Int = 0,
    val endOdometer: Int = 0,
    val distanceKm: Double,
    val durationMinutes: Int = 0,
    val fromLocation: String = "",
    val toLocation: String = "",
    val date: Long = System.currentTimeMillis()
)
