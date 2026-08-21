package com.manette.app.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.manette.app.R
import com.manette.app.bluetooth.HidDeviceService
import com.manette.app.databinding.ActivityHomeBinding
import com.manette.app.ui.gamepad.GamepadActivity
import kotlinx.coroutines.launch

/**
 * Home screen – device pairing and connection management.
 * Also serves as the main entry point after the splash screen.
 */
@SuppressLint("MissingPermission")
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var hidService: HidDeviceService? = null
    private var isBound = false
    private var selectedDevice: BluetoothDevice? = null

    private val deviceAdapter = DeviceAdapter { device ->
        selectedDevice = device
        showToast("Sélectionné : ${device.name ?: device.address}")
    }

    // ── Bluetooth ─────────────────────────────────────────────────────────────
    private val bluetoothManager by lazy { getSystemService(BluetoothManager::class.java) }
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { addDevice(it) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    binding.btnScan.text = getString(R.string.btn_scan)
                    binding.btnScan.isEnabled = true
                    if (discoveredDevices.isEmpty()) {
                        binding.tvNoDevices.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // ── Service connection ─────────────────────────────────────────────────────
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            hidService = (service as HidDeviceService.LocalBinder).getService()
            isBound = true
            observeServiceState()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            hidService = null
            isBound = false
        }
    }

    // ── Permission launcher ────────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startHidService()
            startDiscovery()
        } else {
            showToast(getString(R.string.perm_denied))
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupImmersive()
        setupRecyclerView()
        setupClickListeners()
        animateEntrance()

        // Register BT discovery receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(discoveryReceiver, filter)
    }

    override fun onStart() {
        super.onStart()
        // Only bind if service is already running (started after permissions granted)
        // Use 0 flags — don't auto-create the service here
        val serviceIntent = Intent(this, HidDeviceService::class.java)
        bindService(serviceIntent, serviceConnection, 0)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothAdapter?.cancelDiscovery()
        unregisterReceiver(discoveryReceiver)
    }

    // ── Setup ──────────────────────────────────────────────────────────────────
    private fun setupImmersive() {
        window.decorView.windowInsetsController?.apply {
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.statusBarColor = Color.TRANSPARENT
    }

    private fun setupRecyclerView() {
        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = deviceAdapter
            itemAnimator = null
        }
    }

    private fun setupClickListeners() {
        binding.btnScan.setOnClickListener {
            if (hasBluetoothPermissions()) {
                startHidService()
                startDiscovery()
            } else {
                requestPermissions()
            }
        }

        binding.btnStartGamepad.setOnClickListener {
            startActivity(Intent(this, GamepadActivity::class.java))
            overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out)
        }
    }

    private fun animateEntrance() {
        // Stagger child views for smooth entrance
        listOf(binding.imgLogo, binding.cardStatus, binding.btnScan).forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 40f
            v.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(i * 120L + 200L)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    // ── Service ────────────────────────────────────────────────────────────────
    private fun startHidService() {
        val serviceIntent = Intent(this, HidDeviceService::class.java).apply {
            action = HidDeviceService.ACTION_START
        }
        startForegroundService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeServiceState() {
        lifecycleScope.launch {
            hidService?.state?.collect { state ->
                updateStatusUI(state)
            }
        }
        lifecycleScope.launch {
            hidService?.connectedDevice?.collect { device ->
                if (device != null) {
                    binding.tvDeviceName.text = "PC: ${device.name ?: device.address}"
                    binding.tvDeviceName.visibility = View.VISIBLE
                    binding.btnStartGamepad.visibility = View.VISIBLE
                    binding.btnStartGamepad.animate().alpha(1f).setDuration(400).start()
                } else {
                    binding.tvDeviceName.visibility = View.GONE
                    binding.btnStartGamepad.visibility = View.GONE
                }
            }
        }
    }

    private fun updateStatusUI(state: HidDeviceService.ConnectionState) {
        val (statusText, dotColor) = when (state) {
            HidDeviceService.ConnectionState.IDLE -> getString(R.string.status_disconnected) to 0xFFEF4444.toInt()
            HidDeviceService.ConnectionState.REGISTERING -> getString(R.string.status_connecting) to 0xFFF59E0B.toInt()
            HidDeviceService.ConnectionState.READY -> "En attente de connexion" to 0xFF3B82F6.toInt()
            HidDeviceService.ConnectionState.CONNECTED -> getString(R.string.status_connected) to 0xFF22C55E.toInt()
            HidDeviceService.ConnectionState.ERROR -> "Erreur Bluetooth HID" to 0xFFEF4444.toInt()
        }
        binding.tvStatus.text = statusText
        binding.statusDot.setBackgroundColor(dotColor)
        // Pulse animation on status dot
        binding.statusDot.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
            .withEndAction { binding.statusDot.animate().scaleX(1f).scaleY(1f).setDuration(300).start() }
            .start()
    }

    // ── Bluetooth Discovery ────────────────────────────────────────────────────
    private fun startDiscovery() {
        discoveredDevices.clear()
        deviceAdapter.submitList(emptyList())
        binding.tvNoDevices.visibility = View.GONE
        binding.cardDevices.visibility = View.VISIBLE
        binding.cardDevices.alpha = 0f
        binding.cardDevices.animate().alpha(1f).setDuration(400).start()

        binding.btnScan.text = "Scan en cours…"
        binding.btnScan.isEnabled = false

        bluetoothAdapter?.cancelDiscovery()
        val started = bluetoothAdapter?.startDiscovery() == true
        if (!started) {
            showToast("Impossible de démarrer la découverte")
            binding.btnScan.text = getString(R.string.btn_scan)
            binding.btnScan.isEnabled = true
        }
    }

    private fun addDevice(device: BluetoothDevice) {
        if (discoveredDevices.add(device)) {
            deviceAdapter.submitList(discoveredDevices.toList())
        }
    }

    // ── Permissions ────────────────────────────────────────────────────────────
    private fun hasBluetoothPermissions(): Boolean {
        val perms = mutableListOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
        return perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        )
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
