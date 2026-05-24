package com.autolog.app.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autolog.app.data.model.*

enum class HistoryFilter { ALL, FUEL, REPAIR, TRIP }

@Composable
fun HistoryScreen(
    fuelEntries: List<FuelEntry>,
    repairEntries: List<RepairEntry>,
    tripEntries: List<TripEntry>,
    currency: String,
    onDeleteFuel: (FuelEntry) -> Unit,
    onDeleteRepair: (RepairEntry) -> Unit,
    onDeleteTrip: (TripEntry) -> Unit
) {
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(16.dp)
    ) {
        Text("История записей", color = Color(0xFFF0F0F8),
            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp))

        // Фильтры
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)) {
            items(HistoryFilter.values()) { f ->
                val label = when (f) {
                    HistoryFilter.ALL -> "Все"
                    HistoryFilter.FUEL -> "⛽ Заправки"
                    HistoryFilter.REPAIR -> "🔧 Ремонты"
                    HistoryFilter.TRIP -> "📍 Поездки"
                }
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1A1A26),
                        selectedLabelColor = Color(0xFFE8FF47),
                        containerColor = Color(0xFF12121A),
                        labelColor = Color(0xFF8888AA)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = filter == f,
                        selectedBorderColor = Color(0xFFE8FF47),
                        borderColor = Color(0xFF2A2A3F)
                    )
                )
            }
        }

        // Список
        val allItems = buildList {
            if (filter == HistoryFilter.ALL || filter == HistoryFilter.FUEL)
                fuelEntries.forEach { add(Triple("fuel", it.date, it as Any)) }
            if (filter == HistoryFilter.ALL || filter == HistoryFilter.REPAIR)
                repairEntries.forEach { add(Triple("repair", it.date, it as Any)) }
            if (filter == HistoryFilter.ALL || filter == HistoryFilter.TRIP)
                tripEntries.forEach { add(Triple("trip", it.date, it as Any)) }
        }.sortedByDescending { it.second }

        if (allItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет записей", color = Color(0xFF8888AA), fontSize = 14.sp)
            }
        } else {
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12121A))) {
                LazyColumn {
                    items(allItems) { (type, _, entry) ->
                        when (type) {
                            "fuel" -> DeletableEntryRow(
                                content = { FuelEntryItem(entry as FuelEntry, currency) },
                                onDelete = { onDeleteFuel(entry as FuelEntry) }
                            )
                            "repair" -> DeletableEntryRow(
                                content = { RepairEntryItem(entry as RepairEntry, currency) },
                                onDelete = { onDeleteRepair(entry as RepairEntry) }
                            )
                            "trip" -> DeletableEntryRow(
                                content = { TripEntryItem(entry as TripEntry) },
                                onDelete = { onDeleteTrip(entry as TripEntry) }
                            )
                        }
                        HorizontalDivider(color = Color(0xFF2A2A3F), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun DeletableEntryRow(content: @Composable () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) { content() }
        IconButton(onClick = { showConfirm = true }) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить",
                tint = Color(0xFF8888AA), modifier = Modifier.size(18.dp))
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Удалить запись?", color = Color(0xFFF0F0F8)) },
            text = { Text("Это действие нельзя отменить", color = Color(0xFF8888AA)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("Удалить", color = Color(0xFFFF6B47))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Отмена", color = Color(0xFF8888AA))
                }
            },
            containerColor = Color(0xFF1A1A26)
        )
    }
}