package com.autolog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autolog.app.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(
    fuelEntries: List<FuelEntry>,
    repairEntries: List<RepairEntry>,
    vehicle: Vehicle?,
    totalFuelCost: Double,
    totalRepairCost: Double
) {
    val currency = vehicle?.currency ?: "₽"
    val grand = totalFuelCost + totalRepairCost
    val totalKm = vehicle?.let { it.currentOdometer - it.initialOdometer.toDouble() } ?: 0.0
    val totalLiters = fuelEntries.sumOf { it.liters }
    val avgCons = if (totalKm > 0 && totalLiters > 0) totalLiters / totalKm * 100 else 0.0
    val avgPrice = if (totalLiters > 0) totalFuelCost / totalLiters else 0.0
    val costPerKm = if (totalKm > 0 && grand > 0) grand / totalKm else 0.0

    val byMonth = mutableMapOf<String, Pair<Double, Double>>()
    fuelEntries.forEach {
        val key = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.date))
        val cur = byMonth[key] ?: Pair(0.0, 0.0)
        byMonth[key] = Pair(cur.first + it.totalPrice, cur.second)
    }
    repairEntries.forEach {
        val key = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.date))
        val cur = byMonth[key] ?: Pair(0.0, 0.0)
        byMonth[key] = Pair(cur.first, cur.second + it.price)
    }
    val months = byMonth.keys.sortedDescending()
    val monthlyTotals = months.map { byMonth[it]!!.first + byMonth[it]!!.second }
    val monthlyAvg = if (monthlyTotals.isNotEmpty()) monthlyTotals.average() else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0F)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Аналитика расходов", color = Color(0xFFF0F0F8),
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        item {
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12121A))) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("СТРУКТУРА РАСХОДОВ", color = Color(0xFF8888AA),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    if (grand > 0) {
                        ChartBar("⛽ Топливо", totalFuelCost, (totalFuelCost/grand).toFloat(), currency, Color(0xFF47FF8A))
                        ChartBar("🔧 Ремонты", totalRepairCost, (totalRepairCost/grand).toFloat(), currency, Color(0xFFFF6B47))
                    } else {
                        Text("Добавьте первую запись", color = Color(0xFF8888AA), fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "💧 Расход", if (avgCons > 0) "${"%.1f".format(avgCons)} л" else "—", "л/100 км", Color(0xFF47FF8A))
                StatCard(Modifier.weight(1f), "⛽ За литр", if (avgPrice > 0) "${"%.1f".format(avgPrice)} $currency" else "—", "средняя цена", Color(0xFF47C8FF))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "🛣️ На км", if (costPerKm > 0) "${"%.1f".format(costPerKm)} $currency" else "—", "суммарно", Color(0xFFE8FF47))
                StatCard(Modifier.weight(1f), "📅 В месяц", if (monthlyAvg > 0) "${"%,.0f".format(monthlyAvg)} $currency" else "—", "среднее", Color(0xFFFF6B47))
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12121A))) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("ПО МЕСЯЦАМ", color = Color(0xFF8888AA),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (months.isEmpty()) {
                        Text("Нет данных", color = Color(0xFF8888AA), fontSize = 13.sp)
                    } else {
                        months.forEach { month ->
                            val (fuel, repair) = byMonth[month]!!
                            val total = fuel + repair
                            val name = SimpleDateFormat("LLLL yyyy", Locale("ru"))
                                .format(SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(month)!!)
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(name.replaceFirstChar { it.uppercase() },
                                        color = Color(0xFFF0F0F8), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("⛽ ${"%,.0f".format(fuel)} · 🔧 ${"%,.0f".format(repair)}",
                                        color = Color(0xFF8888AA), fontSize = 11.sp)
                                }
                                Text("${"%,.0f".format(total)} $currency",
                                    color = Color(0xFFE8FF47), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFF2A2A3F), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartBar(label: String, value: Double, pct: Float, currency: String, color: Color) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFF8888AA), fontSize = 11.sp)
            Text("${"%,.0f".format(value)} $currency (${(pct * 100).toInt()}%)", color = Color(0xFF8888AA), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF1A1A26), RoundedCornerShape(4.dp))) {
            Box(modifier = Modifier.fillMaxWidth(pct).height(8.dp).background(color, RoundedCornerShape(4.dp)))
        }
    }
}