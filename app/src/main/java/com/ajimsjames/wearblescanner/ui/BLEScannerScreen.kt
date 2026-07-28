package com.ajimsjames.wearblescanner.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material.Text
import java.util.Locale
import kotlin.math.pow

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int,
    val distanceEstMeters: Double,
    val uuids: List<String> = emptyList(),
    val lastSeenMs: Long = System.currentTimeMillis()
)

enum class BleTab {
    RADAR,
    GATT_LIST,
    FINDER,
    ABOUT
}

@Composable
fun BLEScannerScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(BleTab.RADAR) }
    var isScanning by remember { mutableStateOf(true) }
    var bleDevices by remember { mutableStateOf<Map<String, BleDeviceItem>>(emptyMap()) }
    var selectedDevice by remember { mutableStateOf<BleDeviceItem?>(null) }

    // Compass Heading State for AK09918C Magnetometer Radar
    var compassAzimuth by remember { mutableStateOf(0f) }

    // Bluetooth LE Scanner Integration
    DisposableEffect(Unit) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        val bleScanner = bluetoothAdapter?.bluetoothLeScanner

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { res ->
                    val dev = res.device
                    val address = dev.address ?: "Unknown"
                    val name = dev.name ?: res.scanRecord?.deviceName ?: "BLE Tag (${address.takeLast(5)})"
                    val rssi = res.rssi
                    val distEst = 10.0.pow((-69 - rssi) / (10.0 * 2.0)).coerceIn(0.1, 20.0)

                    val uuidStrings = res.scanRecord?.serviceUuids?.map { it.uuid.toString().uppercase() } ?: emptyList()

                    bleDevices = bleDevices + (address to BleDeviceItem(
                        name = name,
                        address = address,
                        rssi = rssi,
                        distanceEstMeters = distEst,
                        uuids = uuidStrings
                    ))
                }
            }
        }

        if (bluetoothAdapter?.isEnabled == true && bleScanner != null) {
            try {
                bleScanner.startScan(scanCallback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            try {
                bleScanner?.stopScan(scanCallback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Magnetometer Heading Listener
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var gravity = FloatArray(3)
        var geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let { e ->
                    if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = e.values.clone()
                    if (e.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = e.values.clone()

                    val R = FloatArray(9)
                    val I = FloatArray(9)
                    if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(R, orientation)
                        val az = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        compassAzimuth = (az + 360) % 360
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        magSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val deviceList = bleDevices.values.sortedByDescending { it.rssi }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 40.dp, bottom = 24.dp)
        ) {
            when (selectedTab) {
                BleTab.RADAR -> {
                    item {
                        Text(
                            text = "📡 BLE 5.3 Radar (${deviceList.size} Found)",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        // 2D Circular BLE Radar Viewfinder
                        Box(
                            modifier = Modifier
                                .size(135.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF07141E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val maxR = size.width / 2f - 6.dp.toPx()

                                // Concentric Radar Rings
                                drawCircle(Color(0x3300E5FF), radius = maxR, center = center, style = Stroke(width = 1.5.dp.toPx()))
                                drawCircle(Color(0x2200E5FF), radius = maxR * 0.66f, center = center, style = Stroke(width = 1.dp.toPx()))
                                drawCircle(Color(0x1100E5FF), radius = maxR * 0.33f, center = center, style = Stroke(width = 1.dp.toPx()))

                                // Center Self Blip
                                drawCircle(Color(0xFF00E5FF), radius = 4.dp.toPx(), center = center)

                                // Render BLE Device Dots based on RSSI & Magnetometer Angle
                                deviceList.take(8).forEachIndexed { idx, dev ->
                                    val angleRad = Math.toRadians(((idx * 45f) - compassAzimuth).toDouble())
                                    val distNorm = ((dev.rssi + 100).coerceIn(10, 80) / 80f)
                                    val r = maxR * (1f - distNorm * 0.75f)

                                    val x = center.x + (r * Math.sin(angleRad)).toFloat()
                                    val y = center.y - (r * Math.cos(angleRad)).toFloat()

                                    drawCircle(
                                        color = if (dev.rssi > -65) Color(0xFF00E676) else Color(0xFFFFB300),
                                        radius = 5.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${deviceList.size}",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("DEVICES", color = Color(0xFF81D4FA), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }

                    if (deviceList.isEmpty()) {
                        item {
                            Text("Scanning nearby BLE beacons...", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        items(deviceList.take(5)) { dev ->
                            BleDeviceRow(dev) { selectedDevice = dev }
                        }
                    }
                }

                BleTab.GATT_LIST -> {
                    item {
                        Text(
                            text = "🔍 GATT Device List",
                            color = Color(0xFF81D4FA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (deviceList.isEmpty()) {
                        item { Text("No BLE devices detected", color = Color.Gray, fontSize = 11.sp) }
                    } else {
                        items(deviceList) { dev ->
                            BleDeviceRow(dev) { selectedDevice = dev }
                        }
                    }
                }

                BleTab.FINDER -> {
                    val target = selectedDevice ?: deviceList.firstOrNull()
                    item {
                        Text(
                            text = "🧭 RSSI Signal Finder",
                            color = Color(0xFFFFB300),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (target == null) {
                        item { Text("Select a device to track", color = Color.Gray, fontSize = 11.sp) }
                    } else {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(target.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(target.address, color = Color.Gray, fontSize = 9.sp)

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("${target.rssi} dBm", color = Color(0xFF00E5FF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("Est. Distance: ~${String.format(Locale.US, "%.1f", target.distanceEstMeters)} meters", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(8.dp))

                                SignalStrengthBar(target.rssi)
                            }
                        }
                    }
                }

                BleTab.ABOUT -> {
                    item {
                        Text(
                            text = "⚙️ About App",
                            color = Color(0xFFFFB300),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📡 WearBLEScanner v1.0.0", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("By Aju George", color = Color.Gray, fontSize = 9.5.sp, modifier = Modifier.padding(bottom = 6.dp))

                            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                Text("• Bluetooth 5.3 BLE Radar & Beacon Scanner", color = Color.LightGray, fontSize = 8.5.sp)
                                Text("• AK09918C Magnetometer Directional Radar", color = Color.LightGray, fontSize = 8.5.sp)
                                Text("• GATT UUID & Payload Inspector", color = Color.LightGray, fontSize = 8.5.sp)
                                Text("• Target: Samsung Galaxy Watch 6", color = Color(0xFFFFB300), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Curved Bezel Top Navigation Bar
        CurvedLayout(
            anchor = 270f,
            modifier = Modifier.fillMaxSize()
        ) {
            curvedComposable {
                BezelTabPill("📡 Radar", selected = selectedTab == BleTab.RADAR) { selectedTab = BleTab.RADAR }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                BezelTabPill("🔍 GATT", selected = selectedTab == BleTab.GATT_LIST) { selectedTab = BleTab.GATT_LIST }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                BezelTabPill("🧭 Finder", selected = selectedTab == BleTab.FINDER) { selectedTab = BleTab.FINDER }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                BezelTabPill("⚙️ About", selected = selectedTab == BleTab.ABOUT) { selectedTab = BleTab.ABOUT }
            }
        }
    }
}

@Composable
fun BleDeviceRow(dev: BleDeviceItem, onClick: () -> Unit) {
    val rssiColor = when {
        dev.rssi > -65 -> Color(0xFF00E676)
        dev.rssi > -80 -> Color(0xFFFFB300)
        else -> Color(0xFFD32F2F)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dev.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${dev.address} • ~${String.format(Locale.US, "%.1f", dev.distanceEstMeters)}m", color = Color.Gray, fontSize = 8.5.sp)
            }
            Text("${dev.rssi} dBm", color = rssiColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SignalStrengthBar(rssi: Int) {
    val pct = ((rssi + 100).coerceIn(0, 60) / 60f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF333336))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(pct)
                .background(if (pct > 0.6f) Color(0xFF00E676) else if (pct > 0.3f) Color(0xFFFFB300) else Color(0xFFD32F2F))
        )
    }
}

@Composable
fun BezelTabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF00E5FF) else Color(0xFF2C2C2E))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
