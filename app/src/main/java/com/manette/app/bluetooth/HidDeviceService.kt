package com.manette.app.bluetooth

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.manette.app.R
import com.manette.app.hid.GamepadReport
import com.manette.app.hid.HidDescriptors
import com.manette.app.ui.home.HomeActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

/**
 * Foreground service that manages the Bluetooth HID Device profile.
 * Registers the phone as a Bluetooth HID gamepad and sends reports to the connected host.
 */
@SuppressLint("MissingPermission")
class HidDeviceService : Service() {

    companion object {
        private const val TAG = "HidDeviceService"
        private const val NOTIF_ID = 1001
        private const val CHANNEL_ID = "manette_hid_service"

        // Intent actions
        const val ACTION_START = "com.manette.app.START_HID"
        const val ACTION_STOP = "com.manette.app.STOP_HID"
    }

    // ── State ─────────────────────────────────────────────────────────────────
    enum class ConnectionState { IDLE, REGISTERING, READY, CONNECTED, ERROR }

    private val _state = MutableStateFlow(ConnectionState.IDLE)
    val state: StateFlow<ConnectionState> = _state

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice

    // ── Bluetooth ─────────────────────────────────────────────────────────────
    private var bluetoothHidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null
    private val executor = Executors.newSingleThreadExecutor()

    // ── Current gamepad report ─────────────────────────────────────────────────
    private val currentReport = GamepadReport()

    // ── Binder ────────────────────────────────────────────────────────────────
    inner class LocalBinder : Binder() {
        fun getService(): HidDeviceService = this@HidDeviceService
    }
    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder = binder

    // ── SDP Settings (appears as HID gamepad to the PC) ───────────────────────
    private val sdpSettings = BluetoothHidDeviceAppSdpSettings(
        "MaNette Gamepad",
        "Android HID Gamepad by MaNette",
        "MaNette Inc.",
        BluetoothHidDevice.SUBCLASS1_NONE,
        HidDescriptors.GAMEPAD_DESCRIPTOR
    )

    private val qosSettings = BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
        800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
    )

    // ── HID Device callbacks ───────────────────────────────────────────────────
    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "App status changed: registered=$registered, device=$pluggedDevice")
            if (registered) {
                _state.value = ConnectionState.READY
            } else {
                _state.value = ConnectionState.IDLE
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.d(TAG, "Connection state changed: device=${device.name}, state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    hostDevice = device
                    _connectedDevice.value = device
                    _state.value = ConnectionState.CONNECTED
                    Log.i(TAG, "HID connected to ${device.name}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    hostDevice = null
                    _connectedDevice.value = null
                    _state.value = ConnectionState.READY
                    currentReport.reset()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _state.value = ConnectionState.REGISTERING
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            bluetoothHidDevice?.replyReport(device, type, id, currentReport.toByteArray())
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            bluetoothHidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            // Host → device data (LEDs etc.), ignore for gamepad
        }
    }

    // ── Bluetooth profile listener ─────────────────────────────────────────────
    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = proxy as BluetoothHidDevice
                Log.d(TAG, "BluetoothHidDevice profile connected, registering app...")
                _state.value = ConnectionState.REGISTERING
                bluetoothHidDevice?.registerApp(sdpSettings, qosSettings, qosSettings, executor, hidCallback)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            Log.d(TAG, "BluetoothHidDevice profile disconnected")
            bluetoothHidDevice = null
            _state.value = ConnectionState.IDLE
        }
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            startForeground(NOTIF_ID, buildNotification())
        } catch (e: SecurityException) {
            // Bluetooth runtime permissions not yet granted — service will work in non-foreground mode
            Log.w(TAG, "Could not start foreground: ${e.message}")
        }
        initBluetooth()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHidDevice?.unregisterApp()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, bluetoothHidDevice)
        executor.shutdown()
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    private fun initBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available or not enabled")
            _state.value = ConnectionState.ERROR
            return
        }
        adapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
    }

    // ── Public API ────────────────────────────────────────────────────────────
    /**
     * Connect to a specific Bluetooth device as HID host.
     */
    fun connectToDevice(device: BluetoothDevice) {
        bluetoothHidDevice?.connect(device)
    }

    /**
     * Disconnect from current host.
     */
    fun disconnect() {
        hostDevice?.let { bluetoothHidDevice?.disconnect(it) }
    }

    /**
     * Send a gamepad HID report to the connected host.
     */
    fun sendReport(report: GamepadReport): Boolean {
        val device = hostDevice ?: return false
        val hidDev = bluetoothHidDevice ?: return false
        return try {
            hidDev.sendReport(device, 0, report.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send HID report: ${e.message}")
            false
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Service MaNette HID actif"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = Intent(this, HidDeviceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "Arrêter", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
