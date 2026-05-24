package com.autolog.app.data.dao

import androidx.room.*
import com.autolog.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoLogDao {

    // ───────────────────────────── VEHICLES ─────────────────────────────

    @Query("SELECT * FROM vehicles ORDER BY isActive DESC, createdAt DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Int): Vehicle?

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    fun getActiveVehicle(): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveVehicleSync(): Vehicle?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicles SET isActive = 0 WHERE isActive = 1")
    suspend fun clearActiveVehicle()

    @Query("UPDATE vehicles SET isActive = 1 WHERE id = :id")
    suspend fun setActiveVehicle(id: Int)

    @Query("UPDATE vehicles SET currentOdometer = currentOdometer + :distance WHERE id = :vehicleId")
    suspend fun addOdometerDistance(vehicleId: Int, distance: Double)

    @Query("UPDATE vehicles SET currentOdometer = :odometer WHERE id = :vehicleId")
    suspend fun setOdometer(vehicleId: Int, odometer: Double)

    // ───────────────────────────── FUEL ENTRIES ─────────────────────────────

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getFuelEntries(vehicleId: Int): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries WHERE id = :id")
    suspend fun getFuelEntryById(id: Int): FuelEntry?

    @Query("SELECT SUM(totalPrice) FROM fuel_entries WHERE vehicleId = :vehicleId")
    fun getTotalFuelCost(vehicleId: Int): Flow<Double?>

    @Query("SELECT SUM(liters) FROM fuel_entries WHERE vehicleId = :vehicleId")
    fun getTotalLiters(vehicleId: Int): Flow<Double?>

    // ИСПРАВЛЕНО: убрали getAverageFuelConsumption с некорректным JOIN.
    // Расчёт среднего расхода теперь делается на стороне UI в StatsScreen
    // на основе totalLiters и пробега из Vehicle — это точнее и безопаснее.

    @Query("""
        SELECT 
            strftime('%Y-%m', datetime(date/1000, 'unixepoch')) as month,
            SUM(totalPrice) as totalCost,
            SUM(liters) as totalLiters
        FROM fuel_entries 
        WHERE vehicleId = :vehicleId 
        GROUP BY month 
        ORDER BY month DESC
    """)
    fun getFuelStatsByMonth(vehicleId: Int): Flow<List<FuelMonthStat>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFuelEntry(entry: FuelEntry): Long

    @Update
    suspend fun updateFuelEntry(entry: FuelEntry)

    @Delete
    suspend fun deleteFuelEntry(entry: FuelEntry)

    @Query("DELETE FROM fuel_entries WHERE vehicleId = :vehicleId")
    suspend fun deleteAllFuelEntries(vehicleId: Int)

    // ───────────────────────────── REPAIR ENTRIES ─────────────────────────────

    @Query("SELECT * FROM repair_entries WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getRepairEntries(vehicleId: Int): Flow<List<RepairEntry>>

    @Query("SELECT * FROM repair_entries WHERE id = :id")
    suspend fun getRepairEntryById(id: Int): RepairEntry?

    @Query("SELECT SUM(price) FROM repair_entries WHERE vehicleId = :vehicleId")
    fun getTotalRepairCost(vehicleId: Int): Flow<Double?>

    @Query("""
        SELECT 
            strftime('%Y-%m', datetime(date/1000, 'unixepoch')) as month,
            SUM(price) as totalCost
        FROM repair_entries 
        WHERE vehicleId = :vehicleId 
        GROUP BY month 
        ORDER BY month DESC
    """)
    fun getRepairStatsByMonth(vehicleId: Int): Flow<List<RepairMonthStat>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRepairEntry(entry: RepairEntry): Long

    @Update
    suspend fun updateRepairEntry(entry: RepairEntry)

    @Delete
    suspend fun deleteRepairEntry(entry: RepairEntry)

    @Query("DELETE FROM repair_entries WHERE vehicleId = :vehicleId")
    suspend fun deleteAllRepairEntries(vehicleId: Int)

    // ───────────────────────────── TRIP ENTRIES ─────────────────────────────

    @Query("SELECT * FROM trip_entries WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getTripEntries(vehicleId: Int): Flow<List<TripEntry>>

    @Query("SELECT * FROM trip_entries WHERE id = :id")
    suspend fun getTripEntryById(id: Int): TripEntry?

    @Query("SELECT SUM(distanceKm) FROM trip_entries WHERE vehicleId = :vehicleId")
    fun getTotalDistance(vehicleId: Int): Flow<Double?>

    @Query("SELECT SUM(distanceKm) FROM trip_entries WHERE vehicleId = :vehicleId AND date >= :startDate")
    suspend fun getDistanceSince(vehicleId: Int, startDate: Long): Double?

    @Query("""
        SELECT 
            strftime('%Y-%m', datetime(date/1000, 'unixepoch')) as month,
            SUM(distanceKm) as totalDistance
        FROM trip_entries 
        WHERE vehicleId = :vehicleId 
        GROUP BY month 
        ORDER BY month DESC
    """)
    fun getTripStatsByMonth(vehicleId: Int): Flow<List<TripMonthStat>>

    // ИСПРАВЛЕНО: возвращаем Long чтобы LocationService мог получить id записи
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTripEntry(entry: TripEntry): Long

    @Update
    suspend fun updateTripEntry(entry: TripEntry)

    @Delete
    suspend fun deleteTripEntry(entry: TripEntry)

    @Query("DELETE FROM trip_entries WHERE vehicleId = :vehicleId")
    suspend fun deleteAllTripEntries(vehicleId: Int)

    // ───────────────────────────── STATS BY PERIOD ─────────────────────────────

    @Query("""
        SELECT * FROM fuel_entries 
        WHERE vehicleId = :vehicleId 
        AND date >= :startDate AND date <= :endDate 
        ORDER BY date DESC
    """)
    fun getFuelEntriesByPeriod(
        vehicleId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<FuelEntry>>

    @Query("""
        SELECT * FROM repair_entries 
        WHERE vehicleId = :vehicleId 
        AND date >= :startDate AND date <= :endDate 
        ORDER BY date DESC
    """)
    fun getRepairEntriesByPeriod(
        vehicleId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<RepairEntry>>

    @Query("""
        SELECT * FROM trip_entries 
        WHERE vehicleId = :vehicleId 
        AND date >= :startDate AND date <= :endDate 
        ORDER BY date DESC
    """)
    fun getTripEntriesByPeriod(
        vehicleId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<TripEntry>>
}

// ───────────────────────────── DATA CLASSES FOR STATS ─────────────────────────────

data class FuelMonthStat(
    val month: String,
    val totalCost: Double,
    val totalLiters: Double
)

data class RepairMonthStat(
    val month: String,
    val totalCost: Double
)

data class TripMonthStat(
    val month: String,
    val totalDistance: Double
)
