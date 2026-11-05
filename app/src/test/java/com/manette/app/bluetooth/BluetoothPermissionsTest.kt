package com.manette.app.bluetooth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothPermissionsTest {

    @Test
    fun android9RequiresLocationNotNearbyDevices() {
        val perms = BluetoothPermissions.requiredForScan(28)
        assertTrue(perms.contains(BluetoothPermissions.ACCESS_FINE_LOCATION))
        assertTrue(perms.contains(BluetoothPermissions.ACCESS_COARSE_LOCATION))
        assertFalse(perms.contains(BluetoothPermissions.BLUETOOTH_SCAN))
        assertFalse(perms.contains(BluetoothPermissions.BLUETOOTH_CONNECT))
    }

    @Test
    fun android12RequiresScanConnectAdvertiseWithoutLocation() {
        val perms = BluetoothPermissions.requiredForScan(31).toList()
        assertTrue(perms.contains(BluetoothPermissions.BLUETOOTH_SCAN))
        assertTrue(perms.contains(BluetoothPermissions.BLUETOOTH_CONNECT))
        assertTrue(perms.contains(BluetoothPermissions.BLUETOOTH_ADVERTISE))
        assertFalse(perms.contains(BluetoothPermissions.ACCESS_FINE_LOCATION))
        assertFalse(BluetoothPermissions.requestedAtRuntime(31).contains(BluetoothPermissions.POST_NOTIFICATIONS))
    }

    @Test
    fun android13AlsoRequestsNotificationsButScanDoesNotRequireThem() {
        val requested = BluetoothPermissions.requestedAtRuntime(33).toList()
        val required = BluetoothPermissions.requiredForScan(33).toList()
        assertTrue(requested.contains(BluetoothPermissions.POST_NOTIFICATIONS))
        assertFalse(required.contains(BluetoothPermissions.POST_NOTIFICATIONS))
        assertFalse(BluetoothPermissions.isScanPermission(BluetoothPermissions.POST_NOTIFICATIONS))
    }

    @Test
    fun scanFilterKeepsHidGamepadComputerAndUnknown() {
        assertTrue(BluetoothPermissions.acceptsMajorDeviceClass(null))
        assertTrue(BluetoothPermissions.acceptsMajorDeviceClass(0x0100)) // COMPUTER
        assertTrue(BluetoothPermissions.acceptsMajorDeviceClass(0x0200)) // PHONE
        assertTrue(BluetoothPermissions.acceptsMajorDeviceClass(0x0500)) // PERIPHERAL (HID / gamepad)
        assertTrue(BluetoothPermissions.acceptsMajorDeviceClass(0x0400)) // AUDIO_VIDEO
        assertTrue(BluetoothPermissions.acceptsMajorDeviceClass(0x1F00)) // UNCATEGORIZED
        assertTrue(BluetoothPermissions.acceptsMajorDeviceClass(0x1234)) // unknown OEM class
    }
}
