package com.manette.app.util

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat

/**
 * Centralized manager for Android Vibration API haptic feedback and user preferences.
 * Reinforces the physical 3D sunken ("enfoncé") feel on all buttons (Y, B, A, X, L, R,
 * SELECT, START, D-Pad) and 3D analog sticks, and can be toggled on/off in Settings.
 */
@SuppressLint("MissingPermission")
object HapticManager {

    private const val PREFS_NAME = "manette_settings_prefs"
    private const val KEY_HAPTIC_ENABLED = "haptic_feedback_enabled"

    /** Returns true if haptic vibration feedback is enabled (default: true). */
    fun isHapticEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    /** Updates the haptic vibration feedback preference. */
    fun setHapticEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }

    /** Crisp mechanical depression vibration when pressing a button (Y, B, A, X, L, R, SELECT, START). */
    fun vibrateButtonPress(context: Context) {
        if (!isHapticEnabled(context)) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(22L, 210))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(22L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** Subtle mechanical spring release tick when releasing a button or analog stick. */
    fun vibrateButtonRelease(context: Context) {
        if (!isHapticEnabled(context)) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(10L, 95))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** Tactile directional click when pressing an arm of the 3D D-Pad. */
    fun vibrateDPadPress(context: Context) {
        if (!isHapticEnabled(context)) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(18L, 185))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** Tactile thumb contact vibration when grabbing/pressing a 3D analog stick. */
    fun vibrateStickGrab(context: Context) {
        if (!isHapticEnabled(context)) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(16L, 165))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(16L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** Physical bump vibration when the 3D analog stick hits the outer rim of its socket cavity. */
    fun vibrateStickRimHit(context: Context) {
        if (!isHapticEnabled(context)) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(20L, 235))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /**
     * Displays a sleek MaNette Settings dialog allowing the user to enable or disable
     * Android Vibration API haptic feedback for buttons and analog sticks.
     */
    fun showSettingsDialog(context: Context, onPreferenceChanged: ((Boolean) -> Unit)? = null) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(22), dp(24), dp(18))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(Color.parseColor("#0B192E"))
                setStroke(dp(1), Color.parseColor("#3B6998"))
            }
        }

        val titleView = TextView(context).apply {
            text = "⚙  Paramètres de la manette"
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        container.addView(titleView)

        val subtitleView = TextView(context).apply {
            text = "Personnalisez le comportement tactile de MaNette"
            textSize = 13f
            setTextColor(Color.parseColor("#94B8E0"))
            setPadding(0, dp(4), 0, dp(16))
        }
        container.addView(subtitleView)

        val rowCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#152C4F"))
                setStroke(dp(1), Color.parseColor("#2D5488"))
            }
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(12)
            }
        }

        val optionTitle = TextView(context).apply {
            text = "Retour haptique (Vibrations)"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        val optionDesc = TextView(context).apply {
            text = "Vibrer lors de l'appui enfoncé sur les boutons (Y, B, A, X, L, R, D-Pad) et sur les sticks analogiques 3D"
            textSize = 12f
            setTextColor(Color.parseColor("#B0CBE9"))
            setPadding(0, dp(4), 0, 0)
        }
        textColumn.addView(optionTitle)
        textColumn.addView(optionDesc)

        val hapticSwitch = SwitchCompat(context).apply {
            isChecked = isHapticEnabled(context)
            setOnCheckedChangeListener { _, isChecked ->
                setHapticEnabled(context, isChecked)
                if (isChecked) {
                    vibrateButtonPress(context)
                }
                onPreferenceChanged?.invoke(isChecked)
            }
        }

        rowCard.addView(textColumn)
        rowCard.addView(hapticSwitch)
        container.addView(rowCard)

        val closeBtn = TextView(context).apply {
            text = "FERMER"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                colors = intArrayOf(Color.parseColor("#2563EB"), Color.parseColor("#1D4ED8"))
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        }
        container.addView(closeBtn)

        val dialog = AlertDialog.Builder(context)
            .setView(container)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        closeBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * Displays the "À propos" dialog only when the user taps on the "À propos" button.
     * Shows Version 2.0, Concepteur MaxAdis, Contact details, and the official link https://manette-free.vercel.app/.
     */
    fun showAboutDialog(context: Context) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(22), dp(24), dp(18))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(Color.parseColor("#0B192E"))
                setStroke(dp(1), Color.parseColor("#3B6998"))
            }
        }

        val titleView = TextView(context).apply {
            text = "ℹ  À propos de MaNette"
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        container.addView(titleView)

        val infoCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#132847"))
                setStroke(dp(1), Color.parseColor("#2A4B7C"))
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        }

        fun addInfoRow(label: String, value: String, valueColor: String = "#FFFFFF", onClick: (() -> Unit)? = null) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(6), 0, dp(6))
                if (onClick != null) {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onClick() }
                }
            }
            val lblTv = TextView(context).apply {
                text = label
                textSize = 11f
                setTextColor(Color.parseColor("#8AB0DA"))
            }
            val valTv = TextView(context).apply {
                text = value
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(valueColor))
                setPadding(0, dp(2), 0, 0)
            }
            row.addView(lblTv)
            row.addView(valTv)
            infoCard.addView(row)
        }

        addInfoRow("VERSION DE L'APPLICATION", "MaNette — Version 2.0")
        addInfoRow("CONCEPTEUR", "MaxAdis", "#93C5FD")
        addInfoRow(
            "CONTACT",
            "maxadisorg@gmail.com  •  +229 01 54 11 64 25",
            "#E2E8F0"
        ) {
            try {
                val emailIntent = android.content.Intent(
                    android.content.Intent.ACTION_SENDTO,
                    android.net.Uri.parse("mailto:maxadisorg@gmail.com")
                )
                context.startActivity(emailIntent)
            } catch (_: Exception) {}
        }
        addInfoRow(
            "SITE WEB OFFICIEL",
            "https://manette-free.vercel.app/",
            "#60A5FA"
        ) {
            try {
                val webIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://manette-free.vercel.app/")
                )
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }

        container.addView(infoCard)

        val closeBtn = TextView(context).apply {
            text = "FERMER"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                colors = intArrayOf(Color.parseColor("#2563EB"), Color.parseColor("#1D4ED8"))
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        }
        container.addView(closeBtn)

        val dialog = AlertDialog.Builder(context)
            .setView(container)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        closeBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
