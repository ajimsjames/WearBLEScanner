# 📡 WearBLEScanner (v1.0.0)

[![Wear OS](https://img.shields.io/badge/Wear%20OS-5.0-blue.svg)](https://developer.android.com/wear)
[![Device](https://img.shields.io/badge/Target-Samsung%20Galaxy%20Watch%206-black.svg)](https://www.samsung.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**WearBLEScanner** is a standalone Bluetooth 5.3 Low Energy (BLE) radar, iBeacon finder, and GATT inspector for **Samsung Wear OS Smartwatches** (optimized for **Galaxy Watch 6**). It integrates the hardware **AK09918C 3-Axis Magnetometer** and BLE RSSI signal mapping to help track nearby BLE beacons, AirTags, and smart devices directly from your wrist.

Developed by **Aju George** ([@ajimsjames](https://github.com/ajimsjames)).

---

## ✨ Features

- 📡 **2D Circular BLE Radar**: Visualizes nearby BLE devices as glowing dots on a 2D radar view aligned with watch heading.
- 🧭 **AK09918C Magnetometer Direction**: Rotates radar sweep based on hardware geomagnetic compass azimuth.
- 🔍 **GATT Device Inspector**: Lists device MAC addresses, RSSI signal levels (`dBm`), and advertised GATT UUIDs.
- 📏 **RSSI Proximity Finder**: Live signal strength bar & estimated distance in meters (`~0.5m` - `20m`).
- ⭕ **Samsung One UI Bezel Layout**: Curved top navigation bar (`CurvedLayout`) with 40.dp top clearance.

---

## 🛠️ Technology Stack

* **Platform**: Android Wear OS (Min SDK 30 / Target SDK 33 / Wear OS 5)
* **Language**: Kotlin 1.9
* **Radio API**: Android `BluetoothLeScanner` + BLE Scanning Filters
* **Hardware Sensors**: AK09918C Magnetometer Compass + LSM6DSV Accelerometer
* **UI**: Jetpack Compose for Wear OS + Custom Radar Canvas

---

## 🚀 Installation via Wireless ADB

```bash
adb connect <WATCH_IP_ADDRESS>:<PORT>
adb install -r WearBLEScanner-v1.0.0.apk
```

---

## 👨‍💻 Author

Developed by **Aju George**  
GitHub: [@ajimsjames](https://github.com/ajimsjames)
