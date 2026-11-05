package com.manette.app.bluetooth

/**
 * Runtime Bluetooth permissions by API level.
 *
 * Android 12+ uses Nearby Devices (BLUETOOTH_SCAN/CONNECT/ADVERTISE). Scan must
 * declare neverForLocation in the manifest so results are not tied to location.
 * Android 9–11 still require location for classic discovery.
 *
 * API levels are numeric so this object can be unit-tested on the JVM.
 */
object BluetoothPermissions {
    const val BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"
    const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    const val BLUETOOTH_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE"
    const val ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    const val API_S = 31
    const val API_TIRAMISU = 33

    /** Permissions that must be granted before scan/connect. */
    fun requiredForScan(sdkInt: Int): Array<String> {
        return if (sdkInt >= API_S) {
            arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE)
        } else {
            arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
        }
    }

    /** Scan/connect permissions plus optional Android 13+ notification access. */
    fun requestedAtRuntime(sdkInt: Int): Array<String> {
        val perms = requiredForScan(sdkInt).toMutableList()
        if (sdkInt >= API_TIRAMISU) {
            perms += POST_NOTIFICATIONS
        }
        return perms.toTypedArray()
    }

    fun isScanPermission(permission: String): Boolean {
        return permission != POST_NOTIFICATIONS
    }

    fun acceptsMajorDeviceClass(major: Int?): Boolean {
        // COMPUTER 0x0100, PHONE 0x0200, PERIPHERAL/HID 0x0500, UNCATEGORIZED 0x1F00, …
        return true
    }
}
