package com.autolog.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autolog.app.AutoLogApplication
import com.autolog.app.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AutoLogApplication).repository
    private val prefs = application.getSharedPreferences("autolog_prefs", android.content.Context.MODE_PRIVATE)

    private val _activeVehicleId = MutableStateFlow(prefs.getInt("active_vehicle_id", -1))
    val activeVehicleId: StateFlow<Int> = _activeVehicleId

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val vehicles: StateFlow<List<Vehicle>> = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVehicle: StateFlow<Vehicle?> = combine(vehicles, activeVehicleId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val fuelEntries: StateFlow<List<FuelEntry>> = activeVehicleId.flatMapLatest { id ->
        if (id == -1) flowOf(emptyList()) else repository.getFuelEntries(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val repairEntries: StateFlow<List<RepairEntry>> = activeVehicleId.flatMapLatest { id ->
        if (id == -1) flowOf(emptyList()) else repository.getRepairEntries(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tripEntries: StateFlow<List<TripEntry>> = activeVehicleId.flatMapLatest { id ->
        if (id == -1) flowOf(emptyList()) else repository.getTripEntries(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFuelCost: StateFlow<Double> = activeVehicleId.flatMapLatest { id ->
        if (id == -1) flowOf(0.0) else repository.getTotalFuelCost(id).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalRepairCost: StateFlow<Double> = activeVehicleId.flatMapLatest { id ->
        if (id == -1) flowOf(0.0) else repository.getTotalRepairCost(id).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDistance: StateFlow<Double> = activeVehicleId.flatMapLatest { id ->
        if (id == -1) flowOf(0.0) else repository.getTotalDistance(id).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setActiveVehicle(vehicleId: Int) {
        _activeVehicleId.value = vehicleId
        prefs.edit().putInt("active_vehicle_id", vehicleId).apply()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ИСПРАВЛЕНО: при добавлении авто сохраняем BT устройство в SharedPreferences
    // чтобы BluetoothReceiver мог его найти по ключу bt_device_{vehicleId}
    fun addVehicle(vehicle: Vehicle) = viewModelScope.launch {
        try {
            val id = repository.insertVehicle(vehicle)
            setActiveVehicle(id.toInt())
            // Сохраняем BT устройство в prefs
            saveBtDeviceToPrefs(id.toInt(), vehicle.bluetoothDeviceName)
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось добавить автомобиль"
        }
    }

    // ИСПРАВЛЕНО: при обновлении авто обновляем BT устройство в SharedPreferences
    fun updateVehicle(vehicle: Vehicle) = viewModelScope.launch {
        try {
            repository.updateVehicle(vehicle)
            // Обновляем BT устройство в prefs
            saveBtDeviceToPrefs(vehicle.id, vehicle.bluetoothDeviceName)
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось обновить автомобиль"
        }
    }

    // ИСПРАВЛЕНО: при удалении авто удаляем BT устройство из SharedPreferences
    fun deleteVehicle(vehicle: Vehicle) = viewModelScope.launch {
        try {
            repository.deleteVehicle(vehicle)
            // Удаляем BT устройство из prefs
            prefs.edit().remove("bt_device_${vehicle.id}").apply()
            if (vehicle.id == _activeVehicleId.value) {
                val next = vehicles.value.firstOrNull { it.id != vehicle.id }
                setActiveVehicle(next?.id ?: -1)
            }
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось удалить автомобиль"
        }
    }

    // Вспомогательная функция — сохраняет BT устройство в prefs
    private fun saveBtDeviceToPrefs(vehicleId: Int, btDeviceName: String) {
        val editor = prefs.edit()
        if (btDeviceName.isNotBlank()) {
            editor.putString("bt_device_$vehicleId", btDeviceName)
        } else {
            editor.remove("bt_device_$vehicleId")
        }
        editor.apply()
    }

    fun addFuelEntry(entry: FuelEntry) = viewModelScope.launch {
        try {
            repository.insertFuelEntry(entry)
            val vehicle = activeVehicle.first() ?: return@launch
            if (entry.odometer.toDouble() > vehicle.currentOdometer) {
                repository.updateVehicle(vehicle.copy(currentOdometer = entry.odometer.toDouble()))
            }
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось сохранить заправку"
        }
    }

    fun deleteFuelEntry(entry: FuelEntry) = viewModelScope.launch {
        try {
            repository.deleteFuelEntry(entry)
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось удалить запись"
        }
    }

    fun addRepairEntry(entry: RepairEntry) = viewModelScope.launch {
        try {
            repository.insertRepairEntry(entry)
            val vehicle = activeVehicle.first() ?: return@launch
            if (entry.odometer.toDouble() > vehicle.currentOdometer) {
                repository.updateVehicle(vehicle.copy(currentOdometer = entry.odometer.toDouble()))
            }
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось сохранить ремонт"
        }
    }

    fun deleteRepairEntry(entry: RepairEntry) = viewModelScope.launch {
        try {
            repository.deleteRepairEntry(entry)
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось удалить запись"
        }
    }

    fun addTripEntry(entry: TripEntry) = viewModelScope.launch {
        try {
            repository.insertTripEntry(entry)
            val vehicle = activeVehicle.first() ?: return@launch
            if (entry.endOdometer.toDouble() > vehicle.currentOdometer) {
                repository.updateVehicle(vehicle.copy(currentOdometer = entry.endOdometer.toDouble()))
            }
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось сохранить поездку"
        }
    }

    fun deleteTripEntry(entry: TripEntry) = viewModelScope.launch {
        try {
            repository.deleteTripEntry(entry)
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось удалить запись"
        }
    }

    fun updateOdometer(km: Int) = viewModelScope.launch {
        try {
            val vehicle = activeVehicle.first() ?: return@launch
            repository.updateVehicle(vehicle.copy(currentOdometer = km.toDouble()))
        } catch (e: Exception) {
            _errorMessage.value = "Не удалось обновить одометр"
        }
    }

    // ДОБАВЛЕНО: синхронизация всех BT устройств из Room в SharedPreferences
    // вызывается при старте чтобы привести prefs в соответствие с БД
    fun syncBtDevicesToPrefs() = viewModelScope.launch {
        try {
            val allVehicles = vehicles.first()
            val editor = prefs.edit()
            // Сначала очищаем старые записи
            prefs.all.keys.filter { it.startsWith("bt_device_") }.forEach {
                editor.remove(it)
            }
            // Записываем актуальные из Room
            allVehicles.forEach { vehicle ->
                if (vehicle.bluetoothDeviceName.isNotBlank()) {
                    editor.putString("bt_device_${vehicle.id}", vehicle.bluetoothDeviceName)
                }
            }
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
