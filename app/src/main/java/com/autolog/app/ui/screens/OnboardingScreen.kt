package com.autolog.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val tip: String? = null
)

private val pages = listOf(
    OnboardingPage(
        emoji = "🚗",
        title = "Добро пожаловать\nв AutoLog",
        description = "Приложение автоматически записывает ваши поездки, расходы на топливо и ремонт.",
        tip = null
    ),
    OnboardingPage(
        emoji = "📍",
        title = "Разрешите доступ\nк геолокации",
        description = "AutoLog использует GPS чтобы считать километры во время поездки.",
        tip = "Важно: выберите «Разрешить всегда» — иначе поездки не будут записываться когда телефон в кармане"
    ),
    OnboardingPage(
        emoji = "🔵",
        title = "Разрешите доступ\nк Bluetooth",
        description = "Приложение автоматически начинает запись когда вы подключаетесь к магнитоле или hands-free.",
        tip = "Без этого разрешения придётся запускать запись вручную"
    ),
    OnboardingPage(
        emoji = "🔔",
        title = "Разрешите\nуведомления",
        description = "Пока идёт запись поездки — в шторке будет виден счётчик километров.",
        tip = null
    ),
    OnboardingPage(
        emoji = "✅",
        title = "Всё готово!",
        description = "Теперь добавьте свой автомобиль и привяжите Bluetooth устройство машины.\n\nПосле этого поездки будут записываться автоматически.",
        tip = null
    )
)

// Вспомогательная функция проверки разрешения
fun hasPermission(context: android.content.Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(0) }

    // ИСПРАВЛЕНО: проверяем разрешения сразу при старте
    var locationGranted by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var backgroundLocationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else true
        )
    }
    var bluetoothGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            else true
        )
    }
    var notificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            else true
        )
    }

    // Страница геолокации считается пройденной если есть хотя бы обычное разрешение
    val locationPageDone = locationGranted
    // Показываем инструкцию про "Всегда" если обычное есть но фонового нет
    val showSettingsHint = locationGranted && !backgroundLocationGranted &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        bluetoothGranted = results.values.any { it }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // После возврата из настроек — перепроверяем
        backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
    ) {
        // Скроллируемый контент
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Индикатор шагов
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (index == currentPage) 32.dp else 16.dp)
                            .background(
                                if (index <= currentPage) Color(0xFFE8FF47)
                                else Color(0xFF2A2A3F),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            Text(
                pages[currentPage].emoji,
                fontSize = 72.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                pages[currentPage].title,
                color = Color(0xFFF0F0F8),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                pages[currentPage].description,
                color = Color(0xFF8888AA),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Подсказка
            pages[currentPage].tip?.let { tip ->
                // На странице геолокации показываем подсказку только если нет разрешения
                val showTip = currentPage != 1 || !locationGranted
                if (showTip) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A26)),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("💡", fontSize = 18.sp)
                            Text(tip, color = Color(0xFFE8FF47),
                                fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }

            // Инструкция "Как разрешить Всегда" — только если нужно
            if (currentPage == 1 && showSettingsHint) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Как разрешить «Всегда»:",
                            color = Color(0xFF47FF8A), fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp))
                        listOf(
                            "1. Нажмите «Открыть настройки» ниже",
                            "2. Выберите «Разрешения»",
                            "3. Нажмите «Местоположение»",
                            "4. Выберите «Разрешить всегда»"
                        ).forEach { step ->
                            Text(step, color = Color(0xFF8888AA), fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                ).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                settingsLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF47FF8A)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Открыть настройки", color = Color.Black,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Статус разрешений на странице геолокации
            if (currentPage == 1 && locationGranted) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✅", fontSize = 16.sp)
                        Column {
                            Text("Геолокация разрешена",
                                color = Color(0xFF47FF8A), fontSize = 13.sp,
                                fontWeight = FontWeight.Bold)
                            if (!backgroundLocationGranted &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                Text("Фоновый доступ не выдан — нажмите «Открыть настройки»",
                                    color = Color(0xFFFF6B47), fontSize = 12.sp)
                            } else {
                                Text("Фоновый доступ разрешён ✓",
                                    color = Color(0xFF8888AA), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Кнопки всегда внизу
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Текст и цвет кнопки
            val (buttonText, buttonColor) = when (currentPage) {
                1 -> when {
                    !locationGranted -> "Разрешить геолокацию" to Color(0xFFE8FF47)
                    showSettingsHint -> "Открыть настройки" to Color(0xFFE8FF47)
                    else -> "Далее →" to Color(0xFF47FF8A)
                }
                2 -> if (!bluetoothGranted) "Разрешить Bluetooth" to Color(0xFFE8FF47)
                     else "Далее →" to Color(0xFF47FF8A)
                3 -> if (!notificationGranted) "Разрешить уведомления" to Color(0xFFE8FF47)
                     else "Далее →" to Color(0xFF47FF8A)
                pages.size - 1 -> "Начать" to Color(0xFFE8FF47)
                else -> "Далее →" to Color(0xFFE8FF47)
            }

            Button(
                onClick = {
                    when (currentPage) {
                        1 -> {
                            when {
                                !locationGranted -> {
                                    permissionLauncher.launch(arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ))
                                }
                                showSettingsHint -> {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    settingsLauncher.launch(intent)
                                }
                                else -> currentPage++
                            }
                        }
                        2 -> {
                            if (!bluetoothGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                bluetoothLauncher.launch(arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                ))
                            } else {
                                currentPage++
                            }
                        }
                        3 -> {
                            if (!notificationGranted &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                currentPage++
                            }
                        }
                        pages.size - 1 -> onFinish()
                        else -> currentPage++
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text(buttonText, color = Color.Black, fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold)
            }

            if (currentPage < pages.size - 1) {
                TextButton(
                    onClick = { currentPage++ },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Пропустить", color = Color(0xFF8888AA), fontSize = 14.sp)
                }
            }
        }
    }
}
