package com.manette.app.ui.gamepad

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.manette.app.R
import com.manette.app.bluetooth.HidDeviceService
import com.manette.app.databinding.ActivityGamepadBinding
import com.manette.app.hid.ButtonMask
import com.manette.app.hid.DPadDirection
import com.manette.app.hid.GamepadReport
import com.manette.app.views.DPadView
import com.manette.app.views.VirtualJoystickView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * GamepadActivity – full-screen landscape gamepad interface.
 * Collects inputs from all controls and sends HID reports at 60Hz.
 */
@SuppressLint("MissingPermission", "ClickableViewAccessibility")
class GamepadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGamepadBinding
    private var hidService: HidDeviceService? = null
    private var isBound = false
    private var debugVisible = false

    // Current gamepad state
    private val report = GamepadReport()

    // HID polling job (60Hz = 16ms interval)
    private var reportJob: Job? = null

    private val vibrator by lazy { getSystemService(Vibrator::class.java) }

    // ── Service connection ─────────────────────────────────────────────────────
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            hidService = (service as HidDeviceService.LocalBinder).getService()
            isBound = true
            startReportLoop()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            hidService = null
            isBound = false
            reportJob?.cancel()
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGamepadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupImmersive()
        setupJoysticks()
        setupDPad()
        setupButtons()
        setupTopBar()

        // Bind to HID service
        val serviceIntent = Intent(this, HidDeviceService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Start report loop in simulation mode even without service
        startReportLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        reportJob?.cancel()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    // ── Setup ──────────────────────────────────────────────────────────────────
    private fun setupImmersive() {
        window.decorView.windowInsetsController?.apply {
            hide(android.view.WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Barre d'état en mode clair (icônes sombres sur fond blanc)
            setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = 0xFFF0F5FA.toInt()
    }

    private fun setupJoysticks() {
        binding.leftJoystick.listener = object : VirtualJoystickView.JoystickListener {
            override fun onMove(x: Int, y: Int) {
                report.leftX = x
                report.leftY = y
                updateDebugOverlay()
            }
        }
        binding.rightJoystick.listener = object : VirtualJoystickView.JoystickListener {
            override fun onMove(x: Int, y: Int) {
                report.rightX = x
                report.rightY = y
                updateDebugOverlay()
            }
        }
    }

    private fun setupDPad() {
        binding.dpadView.listener = object : DPadView.DPadListener {
            override fun onDirection(direction: Byte) {
                report.dpad = direction
                if (direction != DPadDirection.CENTER) vibrate(12)
                updateDebugOverlay()
            }
        }
    }

    private fun setupButtons() {
        // ABXY + L + R + SELECT + START
        setupGamepadButton(binding.btnA, ButtonMask.A)
        setupGamepadButton(binding.btnB, ButtonMask.B)
        setupGamepadButton(binding.btnX, ButtonMask.X)
        setupGamepadButton(binding.btnY, ButtonMask.Y)
        setupGamepadButton(binding.btnL, ButtonMask.LB)
        setupGamepadButton(binding.btnR, ButtonMask.RB)
        setupGamepadButton(binding.btnSelect, ButtonMask.SELECT)
        setupGamepadButton(binding.btnStart, ButtonMask.START)
    }

    private fun setupGamepadButton(button: View, mask: Int) {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    report.pressButton(mask)
                    button.animate().scaleX(0.92f).scaleY(0.92f).setDuration(60).start()
                    vibrate(18)
                    updateDebugOverlay()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    report.releaseButton(mask)
                    button.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    updateDebugOverlay()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTopBar() {
        binding.btnDebug.setOnClickListener {
            debugVisible = !debugVisible
            binding.cardDebug.visibility = if (debugVisible) View.VISIBLE else View.GONE
            binding.cardDebug.animate().alpha(if (debugVisible) 1f else 0f).setDuration(200).start()
        }

        binding.btnHome.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_up_out)
        }
    }

    // ── HID Report Loop ────────────────────────────────────────────────────────
    /**
     * Sends HID reports at ~60Hz (16ms interval).
     * Works in simulation mode when not connected (shows debug overlay).
     */
    private fun startReportLoop() {
        reportJob?.cancel()
        reportJob = lifecycleScope.launch {
            while (isActive) {
                // Send report to connected device (if any)
                hidService?.sendReport(report)
                // Always update debug overlay if visible
                if (debugVisible) {
                    binding.tvHidReport.post {
                        binding.tvHidReport.text = report.toDebugString()
                    }
                }
                delay(16) // ~60Hz
            }
        }
    }

    private fun updateDebugOverlay() {
        if (debugVisible) {
            binding.tvHidReport.text = report.toDebugString()
        }
    }

    // ── Haptic feedback ────────────────────────────────────────────────────────
    private fun vibrate(ms: Long) {
        vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ── Status display ─────────────────────────────────────────────────────────
    private fun observeStatus() {
        lifecycleScope.launch {
            hidService?.state?.collect { state ->
                val (text, color) = when (state) {
                    HidDeviceService.ConnectionState.CONNECTED -> "Connecté" to 0xFF22C55E.toInt()
                    HidDeviceService.ConnectionState.READY -> "En attente" to 0xFF3B82F6.toInt()
                    else -> "Déconnecté" to 0xFFEF4444.toInt()
                }
                runOnUiThread {
                    binding.tvGpStatus.text = text
                    binding.gpStatusDot.setBackgroundColor(color)
                }
            }
        }
    }
}
