package com.autolog.app.ui.screens

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.autolog.app.data.model.Vehicle

private val CAR_BRANDS = listOf(
    "Другое",
    "LADA (ВАЗ)", "ГАЗ", "УАЗ",
    "Toyota", "Volkswagen", "BMW", "Mercedes-Benz", "Audi",
    "Hyundai", "Kia", "Nissan", "Honda", "Mazda", "Subaru",
    "Ford", "Chevrolet", "Renault", "Peugeot", "Skoda",
    "Mitsubishi", "Suzuki", "Lexus", "Volvo", "Land Rover",
    "Jeep", "Porsche", "Tesla", "Geely", "Chery", "Haval"
).sorted().let { listOf("Другое") + it.filter { b -> b != "Другое" } }

private val CAR_MODELS = mapOf(
    "LADA (ВАЗ)" to listOf("Granta", "Vesta", "XRAY", "Largus", "Niva Legend", "Niva Travel", "2107", "2106", "2109", "2110", "2114", "2115", "Kalina", "Priora"),
    "Toyota" to listOf("Camry", "Corolla", "RAV4", "Land Cruiser", "Prado", "Highlander", "Yaris", "Auris", "Avensis", "Prius", "C-HR", "Fortuner"),
    "Volkswagen" to listOf("Polo", "Golf", "Passat", "Tiguan", "Touareg", "Jetta", "Touran", "T-Roc", "Atlas", "Arteon"),
    "BMW" to listOf("3 Series", "5 Series", "7 Series", "X3", "X5", "X6", "X7", "1 Series", "2 Series", "4 Series", "6 Series"),
    "Mercedes-Benz" to listOf("C-Class", "E-Class", "S-Class", "GLC", "GLE", "GLS", "A-Class", "B-Class", "CLA", "GLA", "GLB"),
    "Audi" to listOf("A3", "A4", "A6", "A8", "Q3", "Q5", "Q7", "Q8", "TT", "A5", "A7"),
    "Hyundai" to listOf("Solaris", "Creta", "Tucson", "Santa Fe", "Elantra", "i30", "i40", "Sonata", "Accent", "ix35"),
    "Kia" to listOf("Rio", "Sportage", "Sorento", "Cerato", "K5", "Stinger", "Seltos", "Carnival", "Soul", "Picanto"),
    "Nissan" to listOf("Qashqai", "X-Trail", "Juke", "Almera", "Pathfinder", "Navara", "Murano", "Teana", "Sentra"),
    "Honda" to listOf("Civic", "CR-V", "HR-V", "Accord", "Jazz", "Pilot", "Fit", "City", "Insight"),
    "Mazda" to listOf("3", "6", "CX-3", "CX-5", "CX-9", "2", "MX-5", "CX-30"),
    "Subaru" to listOf("Outback", "Forester", "Impreza", "XV", "Legacy", "WRX", "Tribeca"),
    "Ford" to listOf("Focus", "Fiesta", "Mondeo", "Kuga", "Explorer", "Edge", "Mustang", "Ranger", "F-150"),
    "Chevrolet" to listOf("Cruze", "Aveo", "Captiva", "Equinox", "Malibu", "Spark", "Blazer", "Trax"),
    "Renault" to listOf("Logan", "Sandero", "Duster", "Kaptur", "Megane", "Clio", "Koleos", "Arkana"),
    "Peugeot" to listOf("206", "207", "208", "301", "308", "408", "508", "2008", "3008", "5008"),
    "Skoda" to listOf("Octavia", "Superb", "Fabia", "Rapid", "Kodiaq", "Karoq", "Scala", "Kamiq"),
    "Mitsubishi" to listOf("Outlander", "ASX", "Eclipse Cross", "Pajero", "L200", "Colt", "Lancer"),
    "Suzuki" to listOf("Vitara", "Swift", "SX4", "Jimny", "Grand Vitara", "Baleno", "Ignis"),
    "Lexus" to listOf("RX", "NX", "ES", "IS", "LS", "GX", "LX", "UX", "CT"),
    "Volvo" to listOf("XC60", "XC90", "XC40", "S60", "S90", "V60", "V90"),
    "Land Rover" to listOf("Discovery", "Range Rover", "Defender", "Freelander", "Discovery Sport", "Range Rover Sport", "Range Rover Evoque"),
    "Jeep" to listOf("Wrangler", "Cherokee", "Grand Cherokee", "Compass", "Renegade", "Gladiator"),
    "Porsche" to listOf("Cayenne", "Macan", "Panamera", "911", "Taycan", "Boxster", "Cayman"),
    "Tesla" to listOf("Model 3", "Model S", "Model X", "Model Y", "Cybertruck"),
    "Geely" to listOf("Atlas", "Coolray", "Tugella", "Monjaro", "Emgrand", "Okavango"),
    "Chery" to listOf("Tiggo 4", "Tiggo 7", "Tiggo 8", "Arrizo 5", "Arrizo 8"),
    "Haval" to listOf("F7", "F7x", "Jolion", "H6", "H9", "Dargo"),
    "ГАЗ" to listOf("Газель Next", "Газель Бизнес", "Соболь", "ГАЗон Next"),
    "УАЗ" to listOf("Patriot", "Hunter", "Буханка", "Пикап", "Профи"),
    "Другое" to listOf("Другое")
)

@Composable
fun VehicleSettingsScreen(
    vehicles: List<Vehicle>,
    activeVehicle: Vehicle?,
    onAddVehicle: (Vehicle) -> Unit,
    onUpdateVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
    onSelectVehicle: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editVehicle by remember { mutableStateOf<Vehicle?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Мои автомобили", color = Color(0xFFF0F0F8),
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFE8FF47),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.Black)
            }
        }

        if (vehicles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚗", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Нет автомобилей", color = Color(0xFF8888AA), fontSize = 16.sp)
                    Text("Нажмите + чтобы добавить", color = Color(0xFF8888AA), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(vehicles) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        isActive = vehicle.id == activeVehicle?.id,
                        onSelect = { onSelectVehicle(vehicle.id) },
                        onEdit = { editVehicle = vehicle },
                        onDelete = { onDeleteVehicle(vehicle) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        VehicleDialog(
            vehicle = null,
            onSave = { onAddVehicle(it); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }

    editVehicle?.let { v ->
        VehicleDialog(
            vehicle = v,
            onSave = { onUpdateVehicle(it); editVehicle = null },
            onDismiss = { editVehicle = null }
        )
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121A)),
        border = if (isActive) BorderStroke(1.5.dp, Color(0xFFE8FF47)) else null
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.name, color = Color(0xFFF0F0F8),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (vehicle.brand.isNotEmpty() || vehicle.model.isNotEmpty()) {
                    Text(
                        "${vehicle.brand} ${vehicle.model} ${if (vehicle.year > 0) vehicle.year else ""}".trim(),
                        color = Color(0xFF8888AA), fontSize = 13.sp
                    )
                }
                Text("Одометр: ${vehicle.currentOdometer} км",
                    color = Color(0xFF8888AA), fontSize = 12.sp)
                if (vehicle.licensePlate.isNotEmpty()) {
                    Text("Номер: ${vehicle.licensePlate}",
                        color = Color(0xFF8888AA), fontSize = 12.sp)
                }
                if (vehicle.bluetoothDeviceName.isNotEmpty()) {
                    Text("🔵 ${vehicle.bluetoothDeviceName}",
                        color = Color(0xFF47C8FF), fontSize = 12.sp)
                }
            }
            Row {
                if (!isActive) {
                    IconButton(onClick = onSelect) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Выбрать",
                            tint = Color(0xFF8888AA))
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Изменить",
                        tint = Color(0xFF8888AA))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить",
                        tint = Color(0xFFFF6B47))
                }
            }
        }
    }
}

@Composable
fun VehicleDialog(
    vehicle: Vehicle?,
    onSave: (Vehicle) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(vehicle?.name ?: "") }
    var brand by remember { mutableStateOf(vehicle?.brand ?: "") }
    var model by remember { mutableStateOf(vehicle?.model ?: "") }
    var year by remember { mutableStateOf(vehicle?.year?.toString() ?: "") }
    var plate by remember { mutableStateOf(vehicle?.licensePlate ?: "") }
    var odo by remember { mutableStateOf(vehicle?.initialOdometer?.toString() ?: "") }
    var currency by remember { mutableStateOf(vehicle?.currency ?: "₽") }
    var selectedBt by remember { mutableStateOf(vehicle?.bluetoothDeviceName ?: "") }

    val currencies = listOf("₽", "$", "€", "₸", "₴", "₾", "₪")

    val modelOptions = remember(brand) {
        CAR_MODELS[brand] ?: listOf("Другое")
    }

    LaunchedEffect(brand) {
        if (model.isNotEmpty() && !modelOptions.contains(model)) {
            model = ""
        }
    }

    val btDevices = remember {
        try {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else true

            if (!hasPermission) return@remember emptyList()

            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            btManager.adapter?.bondedDevices?.mapNotNull { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A26),
        title = {
            Text(
                if (vehicle == null) "Новый автомобиль" else "Редактировать",
                color = Color(0xFFF0F0F8), fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VehicleTextField("Название", name, { name = it })
                VehicleDropdown("Марка", brand, CAR_BRANDS) { brand = it }

                if (brand.isNotEmpty() && brand != "Другое") {
                    VehicleDropdown("Модель", model, modelOptions) { model = it }
                } else {
                    VehicleTextField("Модель", model, { model = it })
                }

                VehicleTextField("Год", year, { year = it })
                VehicleTextField("Гос. номер", plate, { plate = it })
                VehicleTextField("Начальный одометр (км)", odo, { odo = it })
                VehicleDropdown("Валюта", currency, currencies) { currency = it }

                Spacer(modifier = Modifier.height(4.dp))
                Text("BLUETOOTH МАШИНЫ", color = Color(0xFF8888AA),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)

                if (btDevices.isEmpty()) {
                    Text("Нет сопряжённых устройств",
                        color = Color(0xFF8888AA), fontSize = 12.sp)
                } else {
                    btDevices.forEach { deviceName ->
                        val isSelected = selectedBt == deviceName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0xFF47C8FF).copy(alpha = 0.15f)
                                    else Color(0xFF12121A),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(1.5.dp,
                                    if (isSelected) Color(0xFF47C8FF) else Color(0xFF2A2A3F),
                                    RoundedCornerShape(10.dp))
                                .clickable { selectedBt = if (isSelected) "" else deviceName }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔵", fontSize = 16.sp)
                            Text(deviceName,
                                color = if (isSelected) Color(0xFF47C8FF) else Color(0xFFF0F0F8),
                                fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = Color(0xFF47C8FF), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(Vehicle(
                            id = vehicle?.id ?: 0,
                            name = name,
                            brand = brand,
                            model = model,
                            year = year.toIntOrNull() ?: 0,
                            licensePlate = plate,
                            engineVolume = "",
                            fuelType = "АИ-95",
                            initialOdometer = odo.toIntOrNull() ?: 0,
                            currentOdometer = vehicle?.currentOdometer ?: (odo.toDoubleOrNull() ?: 0.0),
                            currency = currency,
                            bluetoothDeviceName = selectedBt,
                            isActive = vehicle?.isActive ?: false,
                            createdAt = vehicle?.createdAt ?: System.currentTimeMillis()
                        ))
                        onDismiss() // ИСПРАВЛЕНО: закрываем диалог после сохранения
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8FF47))
            ) {
                Text("Сохранить", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFF8888AA))
            }
        }
    )
}

@Composable
fun VehicleTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF8888AA)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFE8FF47),
            unfocusedBorderColor = Color(0xFF2A2A3F),
            focusedTextColor = Color(0xFFF0F0F8),
            unfocusedTextColor = Color(0xFFF0F0F8)
        )
    )
}

@Composable
fun VehicleDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color(0xFF8888AA)) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE8FF47),
                unfocusedBorderColor = Color(0xFF2A2A3F),
                focusedTextColor = Color(0xFFF0F0F8),
                unfocusedTextColor = Color(0xFFF0F0F8),
                focusedContainerColor = Color(0xFF1A1A26),
                unfocusedContainerColor = Color(0xFF1A1A26)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1A1A26))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color(0xFFF0F0F8)) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
