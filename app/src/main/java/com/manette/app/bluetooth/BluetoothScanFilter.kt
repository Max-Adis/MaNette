package com.manette.app.bluetooth

import android.bluetooth.BluetoothDevice

/**
 * Scan list policy: keep every result, including HID hosts (PCs), HID
 * peripherals / gamepads, unnamed devices, BLE, and dual-mode adapters.
 */
object BluetoothScanFilter {
    fun accepts(device: BluetoothDevice): Boolean {
        val major = try {
            device.bluetoothClass?.majorDeviceClass
        } catch (_: SecurityException) {
            null
        }
        return BluetoothPermissions.acceptsMajorDeviceClass(major)
    }
}
