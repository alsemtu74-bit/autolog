package com.autolog.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autolog.app.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    vehicle: Vehicle?,
    fuelEntries: List<FuelEntry>,
    repairEntries: List<RepairEntry>,
    tripEntries: List<TripEntry>,
    totalFuelCost: Double,
    totalRepairCost: Double,
    totalDistance: Double,
    onAddFuel: () -> Unit,
    onAddRepair: () -> Unit,
    onAddTrip: () -> Unit
) {
    val context = LocalContext.current
    val currency = vehicle?.currency ?: "₽"
    val grandTotal = totalFuelCost + totalRepairCost
    val kmDriven = vehicle?.let { it.currentOdometer - it.initialOdometer.toDouble() } ?: 0.0
    val costPerKm = if (kmDriven > 0) grandTotal / kmDriven else 0.0

    // Проверяем запущен ли GPS сервис
    val isTracking by produceState(initialValue = false) {
        while (true) {
            val prefs = context.getSharedPreferences("autolog_prefs", android.content.Context.MODE_PRIVATE)
            value = prefs.getBoolean("is_tracking", false)
            kotlinx.coroutines.delay(2000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Индикатор активной поездки
        if (isTracking) {
            item {
                TripActiveCard()
            }
        }

        // Одометр
        item {
            OdometerCard(vehicle = vehicle, tripDistance = totalDistance)
        }

        // Статистика
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "⛽ Топливо",
                    value = "${"%,.0f".format(totalFuelCost)} $currency",
                    sub = "${"%.1f".format(fuelEntries.sumOf { it.liters })} л",
                    valueColor = Color(0xFF47FF8A)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "🔧 Ремонты",
                    value = "${"%,.0f".format(totalRepairCost)} $currency",
                    sub = "${repairEntries.size} записей",
                    valueColor = Color(0xFFFF6B47)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "📍 Пробег",
                    value = "$kmDriven км",
                    sub = "от ${vehicle?.initialOdometer ?: 0} км",
                    valueColor = Color(0xFF47C8FF)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "💰 Итого",
                    value = "${"%,.0f".format(grandTotal)} $currency",
                    sub = if (costPerKm > 0) "${"%.1f".format(costPerKm)} $currency/км" else "—",
                    valueColor = Color(0xFFE8FF47)
                )
            }
        }

        // Кнопки добавления
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AddButton(modifier = Modifier.weight(1f), text = "⛽ Заправка", onClick = onAddFuel)
                AddButton(modifier = Modifier.weight(1f), text = "🔧 Ремонт", onClick = onAddRepair)
                AddButton(modifier = Modifier.weight(1f), text = "📍 Поездка", onClick = onAddTrip)
            }
        }

        // Последние записи
        item {
            Text(
                "Последние записи",
                color = Color(0xFF8888AA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        val allEntries = buildList {
            fuelEntries.take(3).forEach { add(Triple("fuel", it.date, it)) }
            repairEntries.take(3).forEach { add(Triple("repair", it.date, it)) }
            tripEntries.take(3).forEach { add(Triple("trip", it.date, it)) }
        }.sortedByDescending { it.second }.take(6)

        if (allEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Записей пока нет", color = Color(0xFF8888AA))
                }
            }
        } else {
            items(allEntries) { (type, _, entry) ->
                when (type) {
                    "fuel" -> FuelEntryItem(entry as FuelEntry, currency)
                    "repair" -> RepairEntryItem(entry as RepairEntry, currency)
                    "trip" -> TripEntryItem(entry as TripEntry)
                }
            }
        }
    }
}

@Composable
fun TripActiveCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A26)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE8FF47).copy(alpha = alpha))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFFE8FF47).copy(alpha = alpha), RoundedCornerShape(5.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Поездка записывается...", color = Color(0xFFE8FF47),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("GPS активен · километры считаются",
                    color = Color(0xFF8888AA), fontSize = 12.sp)
            }
            Text("🚗", fontSize = 24.sp)
        }
    }
}

@Composable
fun OdometerCard(vehicle: Vehicle?, tripDistance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121A))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("ОДОМЕТР", color = Color(0xFF8888AA), fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(
                "${vehicle?.currentOdometer?.toString() ?: "0"} км",
                color = Color(0xFF47C8FF), fontSize = 44.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "Пробег от старта: ${vehicle?.let { it.currentOdometer - it.initialOdometer } ?: 0} км",
                color = Color(0xFF8888AA), fontSize = 13.sp
            )
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, sub: String, valueColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = Color(0xFF8888AA), fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = Color(0xFF8888AA), fontSize = 11.sp)
        }
    }
}

@Composable
fun AddButton(modifier: Modifier, text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A26))
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8FF47))
    }
}

@Composable
fun FuelEntryItem(entry: FuelEntry, currency: String) {
    EntryRow(
        icon = "⛽",
        iconBg = Color(0x1A47FF8A),
        title = entry.fuelType,
        sub = "${"%.1f".format(entry.liters)} л · ${entry.station.ifEmpty { "" }} · ${formatDate(entry.date)}",
        price = "${"%,.0f".format(entry.totalPrice)} $currency",
        priceColor = Color(0xFF47FF8A)
    )
}

@Composable
fun RepairEntryItem(entry: RepairEntry, currency: String) {
    EntryRow(
        icon = "🔧",
        iconBg = Color(0x1AFF6B47),
        title = entry.description,
        sub = "${entry.category} · ${formatDate(entry.date)}",
        price = "${"%,.0f".format(entry.price)} $currency",
        priceColor = Color(0xFFFF6B47)
    )
}

@Composable
fun TripEntryItem(entry: TripEntry) {
    EntryRow(
        icon = "📍",
        iconBg = Color(0x1A47C8FF),
        title = if (entry.fromLocation.isNotEmpty() && entry.toLocation.isNotEmpty())
            "${entry.fromLocation} → ${entry.toLocation}" else "Поездка",
        sub = "${"%.1f".format(entry.distanceKm)} км · ${formatDate(entry.date)}",
        price = "${"%.1f".format(entry.distanceKm)} км",
        priceColor = Color(0xFF47C8FF)
    )
}

@Composable
fun EntryRow(icon: String, iconBg: Color, title: String, sub: String, price: String, priceColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 18.sp) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFFF0F0F8), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Color(0xFF8888AA), fontSize = 12.sp)
        }
        Text(price, color = priceColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(timestamp))
}