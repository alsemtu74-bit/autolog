package com.autolog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autolog.app.ui.screens.*
import com.autolog.app.ui.theme.AutoLogTheme
import com.autolog.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoLogTheme {
                AutoLogApp()
            }
        }
    }
}

enum class Screen { DASHBOARD, ADD, HISTORY, STATS, SETTINGS }

@Composable
fun AutoLogApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("autolog_prefs", android.content.Context.MODE_PRIVATE)
    }

    // ДОБАВЛЕНО: синхронизируем BT устройства из Room в SharedPreferences при каждом старте
    // Это исправляет проблему когда BluetoothReceiver не находил устройство
    LaunchedEffect(Unit) {
        viewModel.syncBtDevicesToPrefs()
    }

    var onboardingDone by remember {
        mutableStateOf(prefs.getBoolean("onboarding_done", false))
    }

    if (!onboardingDone) {
        OnboardingScreen(
            onFinish = {
                prefs.edit().putBoolean("onboarding_done", true).apply()
                onboardingDone = true
            }
        )
        return
    }

    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var addType by remember { mutableStateOf(EntryType.FUEL) }

    val vehicles by viewModel.vehicles.collectAsState()
    val activeVehicle by viewModel.activeVehicle.collectAsState()
    val fuelEntries by viewModel.fuelEntries.collectAsState()
    val repairEntries by viewModel.repairEntries.collectAsState()
    val tripEntries by viewModel.tripEntries.collectAsState()
    val totalFuelCost by viewModel.totalFuelCost.collectAsState()
    val totalRepairCost by viewModel.totalRepairCost.collectAsState()
    val totalDistance by viewModel.totalDistance.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0F),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF2A1A1A),
                    contentColor = Color(0xFFFF6B47)
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF12121A)) {
                listOf(
                    Triple(Screen.DASHBOARD, Icons.Default.Home, "Главная"),
                    Triple(Screen.ADD, Icons.Default.Add, "Добавить"),
                    Triple(Screen.HISTORY, Icons.Default.List, "История"),
                    Triple(Screen.STATS, Icons.Default.BarChart, "Статистика"),
                    Triple(Screen.SETTINGS, Icons.Default.Settings, "Авто")
                ).forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFE8FF47),
                            selectedTextColor = Color(0xFFE8FF47),
                            unselectedIconColor = Color(0xFF8888AA),
                            unselectedTextColor = Color(0xFF8888AA),
                            indicatorColor = Color(0xFF1A1A26)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    vehicle = activeVehicle,
                    fuelEntries = fuelEntries,
                    repairEntries = repairEntries,
                    tripEntries = tripEntries,
                    totalFuelCost = totalFuelCost,
                    totalRepairCost = totalRepairCost,
                    totalDistance = totalDistance,
                    onAddFuel = { addType = EntryType.FUEL; currentScreen = Screen.ADD },
                    onAddRepair = { addType = EntryType.REPAIR; currentScreen = Screen.ADD },
                    onAddTrip = { addType = EntryType.TRIP; currentScreen = Screen.ADD }
                )
                Screen.ADD -> AddEntryScreen(
                    initialType = addType,
                    vehicleId = activeVehicle?.id ?: -1,
                    currentOdometer = activeVehicle?.currentOdometer?.toInt() ?: 0,
                    currency = activeVehicle?.currency ?: "₽",
                    onSaveFuel = { viewModel.addFuelEntry(it) },
                    onSaveRepair = { viewModel.addRepairEntry(it) },
                    onSaveTrip = { viewModel.addTripEntry(it) },
                    onBack = { currentScreen = Screen.DASHBOARD }
                )
                Screen.HISTORY -> HistoryScreen(
                    fuelEntries = fuelEntries,
                    repairEntries = repairEntries,
                    tripEntries = tripEntries,
                    currency = activeVehicle?.currency ?: "₽",
                    onDeleteFuel = { viewModel.deleteFuelEntry(it) },
                    onDeleteRepair = { viewModel.deleteRepairEntry(it) },
                    onDeleteTrip = { viewModel.deleteTripEntry(it) }
                )
                Screen.STATS -> StatsScreen(
                    fuelEntries = fuelEntries,
                    repairEntries = repairEntries,
                    vehicle = activeVehicle,
                    totalFuelCost = totalFuelCost,
                    totalRepairCost = totalRepairCost
                )
                Screen.SETTINGS -> VehicleSettingsScreen(
                    vehicles = vehicles,
                    activeVehicle = activeVehicle,
                    onAddVehicle = { viewModel.addVehicle(it) },
                    onUpdateVehicle = { viewModel.updateVehicle(it) },
                    onDeleteVehicle = { viewModel.deleteVehicle(it) },
                    onSelectVehicle = { viewModel.setActiveVehicle(it) }
                )
            }
        }
    }
}
