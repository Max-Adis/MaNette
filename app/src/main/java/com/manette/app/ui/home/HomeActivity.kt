package com.manette.app.ui.home

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
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.manette.app.R
import com.manette.app.bluetooth.BluetoothPermissions
import com.manette.app.bluetooth.BluetoothScanFilter
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
    private var pendingConnect: BluetoothDevice? = null
    private var receiverRegistered = false

    private val deviceAdapter = DeviceAdapter { device ->
        selectedDevice = device
        connectToSelected(device)
    }

    // ── Bluetooth ─────────────────────────────────────────────────────────────
    private val bluetoothManager by lazy { getSystemService(BluetoothManager::class.java) }
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private val discoveredDevices = LinkedHashMap<String, BluetoothDevice>()

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    extraDevice(intent)?.let { addDevice(it) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    binding.btnScan.text = "Scan en cours…"
                    binding.btnScan.isEnabled = false
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    binding.btnScan.text = getString(R.string.btn_scan)
                    binding.btnScan.isEnabled = true
                    publishDeviceList()
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = extraDevice(intent) ?: return
                    addDevice(device)
                    val bond = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.BOND_NONE
                    )
                    if (bond == BluetoothDevice.BOND_BONDED &&
                        device.address == pendingConnect?.address
                    ) {
                        attemptPendingConnect()
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    if (state == BluetoothAdapter.STATE_ON && hasBluetoothPermissions()) {
                        startHidService()
                        startDiscovery()
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
            attemptPendingConnect()
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
        val scanOk = BluetoothPermissions.requiredForScan(Build.VERSION.SDK_INT).all { perm ->
            results[perm] == true || hasPermission(perm)
        }
        if (scanOk) {
            startHidService()
            startDiscovery()
        } else {
            showToast(getString(R.string.perm_denied))
        }
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (bluetoothAdapter?.isEnabled == true) {
            startHidService()
            startDiscovery()
        } else {
            showToast(getString(R.string.bt_enable_failed))
            binding.btnScan.text = getString(R.string.btn_scan)
            binding.btnScan.isEnabled = true
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
        registerDiscoveryReceiver()
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, HidDeviceService::class.java)
        bindService(serviceIntent, serviceConnection, 0)
    }

    override fun onResume() {
        super.onResume()
        binding.switchHaptic.isChecked = com.manette.app.util.HapticManager.isHapticEnabled(this)
        if (hasBluetoothPermissions() && bluetoothAdapter?.isEnabled == true) {
            addBondedDevices()
            if (discoveredDevices.isNotEmpty()) {
                binding.cardDevices.visibility = View.VISIBLE
                publishDeviceList()
            }
        }
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
        if (receiverRegistered) {
            unregisterReceiver(discoveryReceiver)
            receiverRegistered = false
        }
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

        binding.switchHaptic.isChecked = com.manette.app.util.HapticManager.isHapticEnabled(this)
        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            com.manette.app.util.HapticManager.setHapticEnabled(this, isChecked)
            if (isChecked) {
                com.manette.app.util.HapticManager.vibrateButtonPress(this)
            }
        }

        binding.cardSettings.setOnClickListener {
            com.manette.app.util.HapticManager.showSettingsDialog(this) { enabled ->
                binding.switchHaptic.isChecked = enabled
            }
        }

        binding.cardAbout.setOnClickListener {
            com.manette.app.util.HapticManager.showAboutDialog(this)
        }
    }

    private fun animateEntrance() {
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
        try {
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            startService(serviceIntent)
        }
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
                } else {
                    binding.tvDeviceName.visibility = View.GONE
                }
                binding.btnStartGamepad.visibility = View.VISIBLE
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
        binding.statusDot.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
            .withEndAction { binding.statusDot.animate().scaleX(1f).scaleY(1f).setDuration(300).start() }
            .start()
    }

    // ── Bluetooth Discovery ────────────────────────────────────────────────────
    private fun registerDiscoveryReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        // ACTION_FOUND is sent by the Bluetooth app (not this UID). NOT_EXPORTED
        // drops every result; Android 13+ also requires an explicit flag.
        ContextCompat.registerReceiver(
            this,
            discoveryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    private fun extraDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun startDiscovery() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            showToast(getString(R.string.bluetooth_not_supported))
            return
        }
        if (!adapter.isEnabled) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val lm = getSystemService(LocationManager::class.java)
            if (lm != null && !lm.isLocationEnabled) {
                showToast(getString(R.string.location_required_for_scan))
            }
        }

        discoveredDevices.clear()
        addBondedDevices()
        binding.tvNoDevices.visibility = View.GONE
        binding.cardDevices.visibility = View.VISIBLE
        binding.cardDevices.alpha = 0f
        binding.cardDevices.animate().alpha(1f).setDuration(400).start()
        publishDeviceList()

        binding.btnScan.text = "Scan en cours…"
        binding.btnScan.isEnabled = false

        adapter.cancelDiscovery()
        val started = adapter.startDiscovery()
        if (!started) {
            Log.w(TAG, "startDiscovery() returned false")
            showToast("Impossible de démarrer la découverte")
            binding.btnScan.text = getString(R.string.btn_scan)
            binding.btnScan.isEnabled = true
            publishDeviceList()
        }
    }

    private fun addBondedDevices() {
        val bonded = bluetoothAdapter?.bondedDevices ?: return
        for (device in bonded) {
            addDevice(device)
        }
    }

    private fun addDevice(device: BluetoothDevice) {
        if (!BluetoothScanFilter.accepts(device)) return
        Log.d(
            TAG,
            "device ${device.address} name=${device.name} class=${device.bluetoothClass} " +
                "type=${device.type} bond=${device.bondState}"
        )
        discoveredDevices[device.address] = device
        publishDeviceList()
    }

    private fun publishDeviceList() {
        val items = discoveredDevices.values.sortedWith(
            compareByDescending<BluetoothDevice> { it.bondState == BluetoothDevice.BOND_BONDED }
                .thenBy { it.name ?: it.address }
        )
        deviceAdapter.submitList(items)
        binding.tvNoDevices.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun connectToSelected(device: BluetoothDevice) {
        pendingConnect = device
        bluetoothAdapter?.cancelDiscovery()
        startHidService()
        val label = device.name ?: device.address
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            showToast(getString(R.string.bonding_device, label))
            device.createBond()
            return
        }
        showToast(getString(R.string.connecting_to, label))
        attemptPendingConnect()
    }

    private fun attemptPendingConnect() {
        val device = pendingConnect ?: return
        hidService?.connectToDevice(device)
    }

    // ── Permissions ────────────────────────────────────────────────────────────
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBluetoothPermissions(): Boolean {
        return BluetoothPermissions.requiredForScan(Build.VERSION.SDK_INT).all { hasPermission(it) }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(BluetoothPermissions.requestedAtRuntime(Build.VERSION.SDK_INT))
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "MaNetteScan"
    }
}
