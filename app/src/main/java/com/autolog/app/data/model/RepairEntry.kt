package com.autolog.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repair_entries")
data class RepairEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vehicleId: Int,
    val description: String,
    val price: Double,
    val odometer: Int = 0,
    val category: String = "",
    val serviceName: String = "",
    val notes: String = "",
    val date: Long = System.currentTimeMillis()
)
