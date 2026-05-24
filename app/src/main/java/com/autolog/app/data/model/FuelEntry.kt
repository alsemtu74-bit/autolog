package com.autolog.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_entries")
data class FuelEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vehicleId: Int,
    val liters: Double,
    val pricePerLiter: Double,
    val totalPrice: Double,
    val odometer: Int = 0,
    val fuelType: String = "АИ-95",
    val station: String = "",
    val date: Long = System.currentTimeMillis()
)
