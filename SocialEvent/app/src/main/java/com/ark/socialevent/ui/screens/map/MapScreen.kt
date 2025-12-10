package com.ark.socialevent.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Geocoder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ark.socialevent.network.EventRepository
import com.ark.socialevent.network.UserRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle as YandexCircle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

// Режимы отображения карты
enum class MapMode {
    EVENTS, HEATMAP
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    userRepository: UserRepository,
    eventRepository: EventRepository,
    onOpenEvent: (com.ark.socialevent.network.Event) -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    val context = LocalContext.current
    var mapMode by remember { mutableStateOf(MapMode.EVENTS) }
    var selectedLocation by remember { mutableStateOf<Point?>(null) }
    var locationCity by remember { mutableStateOf<String?>(null) }
    var currentUserLocation by remember { mutableStateOf<Point?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Запрос разрешений на геолокацию
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation(context) { point ->
                currentUserLocation = point
                selectedLocation = point
                getCityFromPoint(context, point) { city ->
                    locationCity = city
                    showSaveDialog = true

                    // Перемещаем карту к местоположению
                    mapView?.mapWindow?.map?.move(
                        CameraPosition(point, 15f, 0f, 0f)
                    )
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Для использования GPS требуется разрешение на геолокацию",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Панель управления
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Карта событий",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = mapMode == MapMode.EVENTS,
                            onClick = { mapMode = MapMode.EVENTS },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.Filled.Event, null) }
                        ) {
                            Text("События")
                        }

                        SegmentedButton(
                            selected = mapMode == MapMode.HEATMAP,
                            onClick = { mapMode = MapMode.HEATMAP },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Filled.Whatshot, null) }
                        ) {
                            Text("Тепловая карта")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Мое местоположение")
                        }
                    }
                }
            }

            // Информация о местоположении
            if (selectedLocation != null || currentUserLocation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (locationCity != null) "Город: $locationCity" else "Местоположение",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val lat = selectedLocation?.latitude ?: currentUserLocation?.latitude
                            val lon = selectedLocation?.longitude ?: currentUserLocation?.longitude
                            Text(
                                text = "Координаты: ${"%.6f".format(lat)}, ${"%.6f".format(lon)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Карта
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                YandexMapView(
                    modifier = Modifier.fillMaxSize(),
                    mapMode = mapMode,
                    userRepository = userRepository,
                    eventRepository = eventRepository,
                    onMapReady = { view ->
                        mapView = view
                        view.mapWindow.map.move(
                            CameraPosition(Point(55.7558, 37.6176), 11f, 0f, 0f)
                        )

                        // Обработчик кликов по карте
                        view.mapWindow.map.addInputListener(object : InputListener {
                            override fun onMapTap(map: Map, point: Point) {
                                selectedLocation = point
                                getCityFromPoint(context, point) { city ->
                                    locationCity = city
                                    showSaveDialog = true
                                }

                                // Добавляем маркер местоположения
                                map.mapObjects.clear()
                                val placemark = map.mapObjects.addPlacemark(point)
                                placemark.setIcon(createLocationMarkerIcon())
                            }

                            override fun onMapLongTap(map: Map, point: Point) {
                                // Не используется
                            }
                        })
                    },
                    onEventClicked = onOpenEvent
                )
            }
        }
    }

    // Диалог сохранения местоположения
    if (showSaveDialog && selectedLocation != null) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранение местоположения") },
            text = {
                Column {
                    Text("Сохранить выбранное местоположение?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Город: ${locationCity ?: "Не определен"}")
                    Text("Координаты: ${"%.6f".format(selectedLocation!!.latitude)}, ${"%.6f".format(selectedLocation!!.longitude)}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        if (selectedLocation != null && locationCity != null) {
                            userRepository.saveUserLocation(
                                selectedLocation!!.latitude,
                                selectedLocation!!.longitude,
                                locationCity!!
                            ) { success: Boolean, message: String? ->
                                coroutineScope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar(
                                            message = "Местоположение сохранено",
                                            duration = SnackbarDuration.Short
                                        )
                                        currentUserLocation = selectedLocation
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            message = message ?: "Ошибка сохранения",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSaveDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun YandexMapView(
    modifier: Modifier = Modifier,
    mapMode: MapMode,
    userRepository: UserRepository,
    eventRepository: EventRepository,
    onMapReady: (MapView) -> Unit,
    onEventClicked: (com.ark.socialevent.network.Event) -> Unit
) {
    val context = LocalContext.current
    var mapViewState by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        val mapView = MapView(context)
        mapViewState = mapView
        onMapReady(mapView)

        onDispose {
            mapView.onStop()
            mapViewState = null
        }
    }

    DisposableEffect(mapMode) {
        mapViewState?.let { view ->
            when (mapMode) {
                MapMode.EVENTS -> loadEventsOnMap(view, eventRepository, onEventClicked)
                MapMode.HEATMAP -> loadSimpleHeatmap(view, eventRepository, onEventClicked)
            }
        }

        onDispose {
            // Cleanup при смене режима
        }
    }

    AndroidView(
        factory = { mapViewState ?: MapView(it) },
        modifier = modifier,
        update = { view ->
            // Обновление при смене режима
            when (mapMode) {
                MapMode.EVENTS -> loadEventsOnMap(view, eventRepository, onEventClicked)
                MapMode.HEATMAP -> loadSimpleHeatmap(view, eventRepository, onEventClicked)
            }
        }
    )
}

private fun loadEventsOnMap(
    mapView: MapView,
    eventRepository: EventRepository,
    onEventClicked: (com.ark.socialevent.network.Event) -> Unit
) {
    val map = mapView.mapWindow.map
    map.mapObjects.clear()

    eventRepository.getEventsWithFilters { events, error ->
        if (error != null) {
            Log.e("MapScreen", "Error loading events: $error")
            return@getEventsWithFilters
        }

        events?.forEach { event ->
            if (event.latitude != null && event.longitude != null) {
                try {
                    val point = Point(event.latitude, event.longitude)
                    val placemark = map.mapObjects.addPlacemark(point)
                    val icon = createSimpleMarkerIcon(event.type)
                    placemark.setIcon(icon)
                    placemark.setText(event.title ?: "Событие")
                    placemark.userData = event

                    placemark.addTapListener { mapObject, _ ->
                        val clickedEvent = mapObject.userData as? com.ark.socialevent.network.Event
                        clickedEvent?.let { onEventClicked(it) }
                        true
                    }
                } catch (e: Exception) {
                    Log.e("MapScreen", "Error adding event marker: ${e.message}")
                }
            }
        }
    }
}

// ПРОСТАЯ ТЕПЛОВАЯ КАРТА НА ОСНОВЕ ОБЫЧНЫХ СОБЫТИЙ
private fun loadSimpleHeatmap(
    mapView: MapView,
    eventRepository: EventRepository,
    onEventClicked: (com.ark.socialevent.network.Event) -> Unit
) {
    val map = mapView.mapWindow.map
    map.mapObjects.clear()

    Log.d("MapScreen", "Загрузка тепловой карты...")

    // Используем ТЕ ЖЕ САМЫЕ события что и в обычном режиме
    eventRepository.getEventsWithFilters { events, error ->
        if (error != null) {
            Log.e("MapScreen", "Ошибка загрузки событий: $error")
            return@getEventsWithFilters
        }

        if (events.isNullOrEmpty()) {
            Log.d("MapScreen", "Нет событий для тепловой карты")
            return@getEventsWithFilters
        }

        Log.d("MapScreen", "Найдено ${events.size} событий для тепловой карты")

        // Считаем максимальное количество участников
        var maxParticipants = 1
        events.forEach { event ->
            // Используем subscribers_count который уже есть в событии
            val participants = event.subscribersCount ?: 0
            if (participants > maxParticipants) {
                maxParticipants = participants
            }
        }

        Log.d("MapScreen", "Максимальное количество участников: $maxParticipants")

        // ОТОБРАЖАЕМ КАЖДОЕ СОБЫТИЕ КАК ЦВЕТНОЙ КРУГ
        events.forEach { event ->
            if (event.latitude != null && event.longitude != null) {
                try {
                    val participants = event.subscribersCount ?: 0

                    // РАЗМЕР КРУГА ЗАВИСИТ ОТ КОЛИЧЕСТВА УЧАСТНИКОВ
                    // Чем больше участников - тем больше круг
                    val radius = 100f + (participants * 250000f) // Базовый размер + за каждого участника

                    // ЦВЕТ ЗАВИСИТ ОТ КОЛИЧЕСТВА УЧАСТНИКОВ
                    val color = when {
                        participants < 5 -> 0x8000FF00.toInt()    // Зеленый (мало участников)
                        participants < 10 -> 0x80FFFF00.toInt()   // Желтый (средне)
                        participants < 20 -> 0x80FFA500.toInt()   // Оранжевый (много)
                        else -> 0x80FF0000.toInt()                // Красный (очень много)
                    }

                    // Создаем круг для тепловой карты
                    val circle = YandexCircle(
                        Point(event.latitude, event.longitude),
                        radius
                    )

                    val circleMapObject = map.mapObjects.addCircle(circle)
                    circleMapObject.fillColor = color
                    circleMapObject.strokeColor = Color.WHITE
                    circleMapObject.strokeWidth = 2f
                    circleMapObject.zIndex = 1f
                    circleMapObject.userData = event

                    // Добавляем возможность кликать на круг
                    circleMapObject.addTapListener { mapObject, _ ->
                        val clickedEvent = mapObject.userData as? com.ark.socialevent.network.Event
                        clickedEvent?.let { onEventClicked(it) }
                        true
                    }

                    // ДОБАВЛЯЕМ МАРКЕР ПОВЕРХ КРУГА
                    val point = Point(event.latitude, event.longitude)
                    val placemark = map.mapObjects.addPlacemark(point)

                    // Создаем маленький маркер с числом участников
                    val icon = createHeatmapMarkerIcon(participants)
                    placemark.setIcon(icon)

                    // Показываем количество участников
                    placemark.setText("${participants} чел")
                    placemark.userData = event
                    placemark.zIndex = 2f // Поверх круга

                    placemark.addTapListener { mapObject, _ ->
                        val clickedEvent = mapObject.userData as? com.ark.socialevent.network.Event
                        clickedEvent?.let { onEventClicked(it) }
                        true
                    }

                } catch (e: Exception) {
                    Log.e("MapScreen", "Ошибка добавления тепловой точки: ${e.message}")
                }
            }
        }

        Log.d("MapScreen", "Тепловая карта создана: ${events.count { it.latitude != null && it.longitude != null }} точек")
    }
}

// Создаем иконку маркера для тепловой карты (с числом участников)
private fun createHeatmapMarkerIcon(participants: Int): ImageProvider {
    val size = 40
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Цвет зависит от количества участников
    val color = when {
        participants < 5 -> Color.GREEN
        participants < 10 -> Color.YELLOW
        participants < 20 -> Color.rgb(255, 165, 0) // Оранжевый
        else -> Color.RED
    }

    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
    }

    val center = size / 2f
    canvas.drawCircle(center, center, center - 4, paint)

    // Белая обводка
    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawCircle(center, center, center - 4, paint)

    // Рисуем число участников в центре (если меньше 100)
    if (participants > 0 && participants < 100) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER

        val text = participants.toString()
        val yPos = center - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(text, center, yPos, paint)
    }

    return ImageProvider.fromBitmap(bitmap)
}

// Создаем иконку маркера события
private fun createSimpleMarkerIcon(eventType: String?): ImageProvider {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val color = when (eventType?.lowercase()) {
        "concert" -> Color.BLUE
        "lecture" -> Color.GREEN
        "sport" -> Color.RED
        "meeting" -> Color.YELLOW
        "party" -> Color.MAGENTA
        "conference" -> Color.CYAN
        "exhibition" -> Color.GRAY
        else -> Color.DKGRAY
    }

    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
    }

    val center = size / 2f
    canvas.drawCircle(center, center, center - 2, paint)

    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    canvas.drawCircle(center, center, center - 2, paint)

    return ImageProvider.fromBitmap(bitmap)
}

// Создаем иконку маркера местоположения
private fun createLocationMarkerIcon(): ImageProvider {
    val size = 56
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        color = Color.BLUE
        isAntiAlias = true
    }

    val center = size / 2f
    val radius = center - 4f
    canvas.drawCircle(center, center, radius, paint)

    paint.color = Color.WHITE
    canvas.drawCircle(center, center, radius - 8f, paint)

    paint.color = Color.BLUE
    canvas.drawCircle(center, center, 4f, paint)

    return ImageProvider.fromBitmap(bitmap)
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, callback: (Point) -> Unit) {
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        location?.let {
            callback(Point(it.latitude, it.longitude))
        } ?: run {
            Log.e("MapScreen", "Location is null, using default location")
            callback(Point(55.7558, 37.6176))
        }
    }.addOnFailureListener { e ->
        Log.e("MapScreen", "Error getting location", e)
        callback(Point(55.7558, 37.6176))
    }
}

private fun getCityFromPoint(context: Context, point: Point, callback: (String) -> Unit) {
    val geocoder = Geocoder(context, Locale.getDefault())

    try {
        val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
        if (addresses?.isNotEmpty() == true) {
            val city = addresses[0].locality ?: addresses[0].adminArea ?: "Неизвестный город"
            callback(city)
        } else {
            callback("Неизвестный город")
        }
    } catch (e: IOException) {
        Log.e("MapScreen", "Error getting city from location", e)
        callback("Неизвестный город")
    } catch (e: IllegalArgumentException) {
        Log.e("MapScreen", "Invalid coordinates: ${point.latitude}, ${point.longitude}")
        callback("Неизвестный город")
    }
}