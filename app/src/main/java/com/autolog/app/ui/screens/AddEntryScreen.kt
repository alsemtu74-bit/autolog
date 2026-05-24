package com.autolog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autolog.app.data.model.*
import java.util.*

enum class EntryType { FUEL, REPAIR, TRIP }

@Composable
fun AddEntryScreen(
    initialType: EntryType = EntryType.FUEL,
    vehicleId: Int,
    currentOdometer: Int,
    currency: String,
    onSaveFuel: (FuelEntry) -> Unit,
    onSaveRepair: (RepairEntry) -> Unit,
    onSaveTrip: (TripEntry) -> Unit,
    onBack: () -> Unit
) {
    // ДОБАВЛЕНО: блокируем если нет активного авто
    if (vehicleId == -1) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F))
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("🚗", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Сначала добавьте автомобиль", color = Color(0xFFF0F0F8),
                fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Перейдите в раздел «Авто» и добавьте машину",
                color = Color(0xFF8888AA), fontSize = 14.sp)
        }
        return
    }

    var selectedType by remember { mutableStateOf(initialType) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(16.dp)
    ) {
        Text(
            "Добавить запись",
            color = Color(0xFFF0F0F8),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 18.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EntryType.values().forEach { type ->
                val label = when (type) {
                    EntryType.FUEL -> "⛽ Заправка"
                    EntryType.REPAIR -> "🔧 Ремонт"
                    EntryType.TRIP -> "📍 Поездка"
                }
                val selected = selectedType == type
                val color = when (type) {
                    EntryType.FUEL -> Color(0xFF47FF8A)
                    EntryType.REPAIR -> Color(0xFFFF6B47)
                    EntryType.TRIP -> Color(0xFF47C8FF)
                }
                Button(
                    onClick = { selectedType = type },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) color.copy(alpha = 0.15f) else Color(0xFF1A1A26)
                    ),
                    border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null
                ) {
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (selected) color else Color(0xFF8888AA))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        when (selectedType) {
            EntryType.FUEL -> FuelForm(vehicleId, currentOdometer, currency, onSaveFuel, onBack)
            EntryType.REPAIR -> RepairForm(vehicleId, currentOdometer, currency, onSaveRepair, onBack)
            EntryType.TRIP -> TripForm(vehicleId, currentOdometer, onSaveTrip, onBack)
        }
    }
}

@Composable
fun FuelForm(vehicleId: Int, currentOdo: Int, currency: String, onSave: (FuelEntry) -> Unit, onBack: () -> Unit) {
    var liters by remember { mutableStateOf("") }
    var pricePerL by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf(currentOdo.toString()) }
    var fuelType by remember { mutableStateOf("АИ-95") }
    var station by remember { mutableStateOf("") }
    // ДОБАВЛЕНО: состояние ошибок
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(liters, pricePerL) {
        val l = liters.toDoubleOrNull()
        val p = pricePerL.toDoubleOrNull()
        if (l != null && p != null) total = "%.0f".format(l * p)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        AppTextField("Литры", liters, { liters = it; error = null }, KeyboardType.Decimal)
        AppTextField("Цена за литр, $currency", pricePerL, { pricePerL = it; error = null }, KeyboardType.Decimal)
        AppTextField("Сумма итого, $currency", total, { total = it; error = null }, KeyboardType.Decimal)
        AppTextField("Одометр, км", odometer, { odometer = it; error = null }, KeyboardType.Number)
        AppDropdown("Тип топлива", fuelType, listOf("АИ-92","АИ-95","АИ-98","Дизель","Газ")) { fuelType = it }
        AppTextField("АЗС / Заметка", station, { station = it })

        // ДОБАВЛЕНО: показываем ошибку
        error?.let {
            Text(it, color = Color(0xFFFF6B47), fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val l = liters.toDoubleOrNull()
                val p = pricePerL.toDoubleOrNull()
                val t = total.toDoubleOrNull()

                // ДОБАВЛЕНО: валидация
                when {
                    l == null || l <= 0 -> error = "Введите количество литров"
                    p == null || p <= 0 -> error = "Введите цену за литр"
                    t == null || t <= 0 -> error = "Введите сумму"
                    else -> {
                        onSave(FuelEntry(
                            vehicleId = vehicleId, liters = l, pricePerLiter = p,
                            totalPrice = t, odometer = odometer.toIntOrNull() ?: currentOdo,
                            fuelType = fuelType, station = station
                        ))
                        onBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8FF47))
        ) { Text("💾 Сохранить заправку", color = Color.Black, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
fun RepairForm(vehicleId: Int, currentOdo: Int, currency: String, onSave: (RepairEntry) -> Unit, onBack: () -> Unit) {
    var desc by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf(currentOdo.toString()) }
    var category by remember { mutableStateOf("🛢️ Масло / Фильтры") }
    var service by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val categories = listOf("🛢️ Масло / Фильтры","🔧 Ходовая","🛑 Тормоза","⚡ Электрика",
        "🏎️ Двигатель","🔲 Шины / Диски","🌡️ Охлаждение","📋 ТО плановое","🔑 Другое")

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        AppTextField("Описание", desc, { desc = it; error = null })
        AppTextField("Сумма, $currency", price, { price = it; error = null }, KeyboardType.Decimal)
        AppTextField("Одометр, км", odometer, { odometer = it }, KeyboardType.Number)
        AppDropdown("Категория", category, categories) { category = it }
        AppTextField("СТО / Мастер", service, { service = it })
        AppTextField("Заметки", notes, { notes = it })

        error?.let {
            Text(it, color = Color(0xFFFF6B47), fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val p = price.toDoubleOrNull()
                // ДОБАВЛЕНО: валидация
                when {
                    desc.isBlank() -> error = "Введите описание ремонта"
                    p == null || p <= 0 -> error = "Введите сумму"
                    else -> {
                        onSave(RepairEntry(
                            vehicleId = vehicleId, description = desc, price = p,
                            odometer = odometer.toIntOrNull() ?: currentOdo,
                            category = category, serviceName = service, notes = notes
                        ))
                        onBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8FF47))
        ) { Text("💾 Сохранить ремонт", color = Color.Black, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
fun TripForm(vehicleId: Int, currentOdo: Int, onSave: (TripEntry) -> Unit, onBack: () -> Unit) {
    var startOdo by remember { mutableStateOf(currentOdo.toString()) }
    var endOdo by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val km = (endOdo.toIntOrNull() ?: 0) - (startOdo.toIntOrNull() ?: 0)

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        // ДОБАВЛЕНО: подсказка текущего одометра
        Text(
            "Текущий одометр: $currentOdo км",
            color = Color(0xFF8888AA), fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        AppTextField("Одометр — старт, км", startOdo, { startOdo = it; error = null }, KeyboardType.Number)
        AppTextField("Одометр — финиш, км", endOdo, { endOdo = it; error = null }, KeyboardType.Number)

        if (km > 0) {
            Text("Расстояние: $km км", color = Color(0xFF47C8FF),
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp))
        }

        AppTextField("Откуда", from, { from = it })
        AppTextField("Куда", to, { to = it })

        error?.let {
            Text(it, color = Color(0xFFFF6B47), fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val s = startOdo.toIntOrNull()
                val e = endOdo.toIntOrNull()
                // ДОБАВЛЕНО: валидация
                when {
                    s == null -> error = "Введите начальный одометр"
                    e == null -> error = "Введите конечный одометр"
                    e <= s -> error = "Конечный одометр должен быть больше начального"
                    else -> {
                        onSave(TripEntry(
                            vehicleId = vehicleId, startOdometer = s,
                            endOdometer = e, distanceKm = (e - s).toDouble(),
                            fromLocation = from, toLocation = to
                        ))
                        onBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8FF47))
        ) { Text("💾 Сохранить поездку", color = Color.Black, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
fun AppTextField(label: String, value: String, onValueChange: (String) -> Unit,
                 keyboardType: KeyboardType = KeyboardType.Text) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(label.uppercase(), color = Color(0xFF8888AA), fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 7.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE8FF47),
                unfocusedBorderColor = Color(0xFF2A2A3F),
                focusedTextColor = Color(0xFFF0F0F8),
                unfocusedTextColor = Color(0xFFF0F0F8),
                focusedContainerColor = Color(0xFF1A1A26),
                unfocusedContainerColor = Color(0xFF1A1A26)
            )
        )
    }
}

@Composable
fun AppDropdown(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(label.uppercase(), color = Color(0xFF8888AA), fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 7.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = value, onValueChange = {},
                readOnly = true,
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
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1A1A26))) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color(0xFFF0F0F8)) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}
