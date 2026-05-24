package com.autolog.app.data

import com.autolog.app.data.dao.AutoLogDao
import com.autolog.app.data.model.*
import kotlinx.coroutines.flow.Flow

class AutoLogRepository(private val dao: AutoLogDao) {

    // ── VEHICLES ──
    fun getAllVehicles(): Flow<List<Vehicle>> = dao.getAllVehicles()
    suspend fun getVehicleById(id: Int): Vehicle? = dao.getVehicleById(id)
    suspend fun insertVehicle(vehicle: Vehicle): Long = dao.insertVehicle(vehicle)
    suspend fun updateVehicle(vehicle: Vehicle) = dao.updateVehicle(vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle) = dao.deleteVehicle(vehicle)

    // ── FUEL ──
    fun getFuelEntries(vehicleId: Int): Flow<List<FuelEntry>> = dao.getFuelEntries(vehicleId)
    fun getTotalFuelCost(vehicleId: Int): Flow<Double?> = dao.getTotalFuelCost(vehicleId)
    fun getTotalLiters(vehicleId: Int): Flow<Double?> = dao.getTotalLiters(vehicleId)
    suspend fun insertFuelEntry(entry: FuelEntry) = dao.insertFuelEntry(entry)
    suspend fun deleteFuelEntry(entry: FuelEntry) = dao.deleteFuelEntry(entry)

    // ── REPAIRS ──
    fun getRepairEntries(vehicleId: Int): Flow<List<RepairEntry>> = dao.getRepairEntries(vehicleId)
    fun getTotalRepairCost(vehicleId: Int): Flow<Double?> = dao.getTotalRepairCost(vehicleId)
    suspend fun insertRepairEntry(entry: RepairEntry) = dao.insertRepairEntry(entry)
    suspend fun deleteRepairEntry(entry: RepairEntry) = dao.deleteRepairEntry(entry)

    // ── TRIPS ──
    fun getTripEntries(vehicleId: Int): Flow<List<TripEntry>> = dao.getTripEntries(vehicleId)
    fun getTotalDistance(vehicleId: Int): Flow<Double?> = dao.getTotalDistance(vehicleId)
    suspend fun insertTripEntry(entry: TripEntry) = dao.insertTripEntry(entry)
    suspend fun deleteTripEntry(entry: TripEntry) = dao.deleteTripEntry(entry)

    // ── STATS BY PERIOD ──
    fun getFuelByPeriod(vehicleId: Int, start: Long, end: Long) =
        dao.getFuelEntriesByPeriod(vehicleId, start, end)

    fun getRepairByPeriod(vehicleId: Int, start: Long, end: Long) =
        dao.getRepairEntriesByPeriod(vehicleId, start, end)
}