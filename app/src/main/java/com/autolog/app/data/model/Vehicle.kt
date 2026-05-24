package com.autolog.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val brand: String = "",
    val model: String = "",
    val year: Int = 0,
    val licensePlate: String = "",
    val engineVolume: String = "",
    val fuelType: String = "АИ-95",
    val initialOdometer: Int = 0,
    val currentOdometer: Double = 0.0,
    val currency: String = "₽",
    val bluetoothDeviceName: String = "",
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)