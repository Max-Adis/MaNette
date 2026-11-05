package com.manette.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.manette.app.hid.ButtonMask
import com.manette.app.hid.DPadDirection
import com.manette.app.hid.GamepadReport
import com.manette.app.hid.HidDescriptors
import com.manette.app.util.HapticManager
import com.manette.app.views.DPadView
import com.manette.app.views.GamepadButtonView
import com.manette.app.views.GamepadOverlayLayout
import com.manette.app.views.VirtualJoystickView
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

  @Test
  fun `test gamepad report serialization and haptic preferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertTrue(HapticManager.isHapticEnabled(context))
    HapticManager.setHapticEnabled(context, false)
    assertFalse(HapticManager.isHapticEnabled(context))
    HapticManager.setHapticEnabled(context, true)
    assertTrue(HapticManager.isHapticEnabled(context))

    val report = GamepadReport(
      buttons = ButtonMask.A or ButtonMask.START,
      dpad = DPadDirection.UP,
      leftX = 127,
      leftY = -127,
      rightX = -64,
      rightY = 64
    )
    val bytes = report.toByteArray()
    assertEquals(6, bytes.size)
    assertTrue(HidDescriptors.GAMEPAD_DESCRIPTOR.isNotEmpty())

    // Verify Version 2.0 and Concepteur MaxAdis strings
    assertTrue(context.getString(R.string.app_version_badge).contains("2.0"))
    assertTrue(context.getString(R.string.app_version_badge).contains("MaxAdis"))
    assertTrue(context.getString(R.string.about_version_label).contains("v2.0"))
    assertTrue(context.getString(R.string.about_designer_label).contains("MaxAdis"))
  }

  @Test
  fun `test authentic enlarged buttons, invisible-at-rest views, sunken effects, haptics and record video`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val width = 1024
    val height = 571

    val root = FrameLayout(context)
    val bgImage = ImageView(context).apply {
      scaleType = ImageView.ScaleType.FIT_CENTER
      val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.manette)
      setImageBitmap(bmp)
    }
    root.addView(bgImage, FrameLayout.LayoutParams(width, height))

    val overlay = GamepadOverlayLayout(context)

    val btnL = GamepadButtonView(context).apply {
      tag = "0.0420,0.1289,0.2539,0.2312"
      buttonStyle = GamepadButtonView.Style.SHOULDER_L
      buttonLabel = "L"
    }
    val btnR = GamepadButtonView(context).apply {
      tag = "0.0420,0.6172,0.2539,0.2312"
      buttonStyle = GamepadButtonView.Style.SHOULDER_R
      buttonLabel = "R"
    }
    val dpadView = DPadView(context).apply {
      tag = "0.3012,0.1191,0.1758,0.3152"
    }
    val btnY = GamepadButtonView(context).apply {
      tag = "0.2382,0.7480,0.0898,0.1611"
      buttonStyle = GamepadButtonView.Style.ACTION_CIRCLE
      buttonLabel = "Y"
    }
    val btnX = GamepadButtonView(context).apply {
      tag = "0.3678,0.6699,0.0898,0.1611"
      buttonStyle = GamepadButtonView.Style.ACTION_CIRCLE
      buttonLabel = "X"
    }
    val btnB = GamepadButtonView(context).apply {
      tag = "0.3678,0.8223,0.0898,0.1611"
      buttonStyle = GamepadButtonView.Style.ACTION_CIRCLE
      buttonLabel = "B"
    }
    val btnA = GamepadButtonView(context).apply {
      tag = "0.4904,0.7461,0.0898,0.1611"
      buttonStyle = GamepadButtonView.Style.ACTION_CIRCLE
      buttonLabel = "A"
    }
    val btnSelect = GamepadButtonView(context).apply {
      tag = "0.3958,0.4102,0.0645,0.0595"
      buttonStyle = GamepadButtonView.Style.PILL_CENTER
      buttonLabel = "SELECT"
    }
    val btnStart = GamepadButtonView(context).apply {
      tag = "0.3958,0.5117,0.0645,0.0595"
      buttonStyle = GamepadButtonView.Style.PILL_CENTER
      buttonLabel = "START"
    }
    val leftJoystick = VirtualJoystickView(context).apply {
      tag = "0.4650,0.3050,0.1750,0.3100"
    }
    val rightJoystick = VirtualJoystickView(context).apply {
      tag = "0.4650,0.5200,0.1750,0.3100"
    }

    listOf(
      btnL, btnR, dpadView, btnY, btnX, btnB, btnA,
      btnSelect, btnStart, leftJoystick, rightJoystick
    ).forEach { overlay.addView(it) }

    root.addView(overlay, FrameLayout.LayoutParams(width, height))

    root.measure(
      View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )
    root.layout(0, 0, width, height)

    val outDir = File("/tmp/manette_frames")
    outDir.mkdirs()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val hudBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xCC0A182E.toInt()
      style = Paint.Style.FILL
    }
    val hudStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFF4772A8.toInt()
      style = Paint.Style.STROKE
      strokeWidth = 2f
    }
    val hudTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFF00FF7F.toInt()
      textSize = 16f
      isFakeBoldText = true
    }

    val lCx = leftJoystick.width / 2f
    val lCy = leftJoystick.height / 2f
    val rCx = rightJoystick.width / 2f
    val rCy = rightJoystick.height / 2f
    val maxR = leftJoystick.width * 0.18f

    fun pressView(v: View, down: Boolean, x: Float = 20f, y: Float = 20f) {
      val now = SystemClock.uptimeMillis()
      val action = if (down) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP
      v.dispatchTouchEvent(MotionEvent.obtain(now, now, action, x, y, 0))
    }

    val totalFrames = 120
    for (frame in 0 until totalFrames) {
      val now = SystemClock.uptimeMillis() + frame * 16L
      var activeLabel = "REPOS"

      // 1. Analog sticks 360° + diagonal movement + spring back
      when {
        frame in 0..7 -> {
          activeLabel = "REPOS (Boutons photoréalistes agrandis)"
        }
        frame in 8..55 -> {
          val t = (frame - 8) / 47f
          val angleL = t * Math.PI * 2.0
          val radiusL = if (frame < 16) ((frame - 8) / 8f) * maxR else maxR
          val lx = lCx + (cos(angleL) * radiusL).toFloat()
          val ly = lCy + (sin(angleL) * radiusL).toFloat()

          val angleR = -t * Math.PI * 2.0 + Math.PI / 3.0
          val radiusR = if (frame < 16) ((frame - 8) / 8f) * maxR else maxR
          val rx = rCx + (cos(angleR) * radiusR).toFloat()
          val ry = rCy + (sin(angleR) * radiusR).toFloat()

          val action = if (frame == 8) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_MOVE
          leftJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, action, lx, ly, 0))
          rightJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, action, rx, ry, 0))
        }
        frame in 56..68 -> {
          val p = (frame - 56) / 10f
          if (p <= 1f) {
            val tension = 1.6f
            val t = p - 1f
            val overshoot = t * t * ((tension + 1f) * t + tension) + 1f
            val sxL = lCx + maxR * (1f - overshoot)
            val sxR = rCx + (cos(Math.PI / 3.0) * maxR * (1f - overshoot)).toFloat()
            val syR = rCy + (sin(Math.PI / 3.0) * maxR * (1f - overshoot)).toFloat()
            leftJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, sxL, lCy, 0))
            rightJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, sxR, syR, 0))
          } else if (frame == 67) {
            leftJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, lCx, lCy, 0))
            leftJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, lCx, lCy, 0))
            rightJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, rCx, rCy, 0))
            rightJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, rCx, rCy, 0))
          }
        }
        frame in 69..105 -> {
          val p = (frame - 69) / 36f
          val lx = lCx + (sin(p * Math.PI * 4.0) * maxR).toFloat()
          val ly = lCy - (cos(p * Math.PI * 2.0) * maxR).toFloat()
          val rx = rCx - (cos(p * Math.PI * 3.0) * maxR).toFloat()
          val ry = rCy + (sin(p * Math.PI * 3.0) * maxR).toFloat()
          val action = if (frame == 69) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_MOVE
          leftJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, action, lx, ly, 0))
          rightJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, action, rx, ry, 0))
        }
        else -> {
          leftJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, lCx, lCy, 0))
          leftJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, lCx, lCy, 0))
          rightJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, rCx, rCy, 0))
          rightJoystick.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, rCx, rCy, 0))
        }
      }

      // 2. Sequential & combined button presses showing the visible 3D sunken ("enfoncé") effect + haptics
      when (frame) {
        10 -> { pressView(btnY, true); pressView(dpadView, true, dpadView.width / 2f, dpadView.height * 0.12f) }
        22 -> { pressView(btnY, false); pressView(btnB, true); pressView(dpadView, true, dpadView.width * 0.88f, dpadView.height / 2f) }
        34 -> { pressView(btnB, false); pressView(btnA, true); pressView(dpadView, true, dpadView.width / 2f, dpadView.height * 0.88f) }
        46 -> { pressView(btnA, false); pressView(btnX, true); pressView(dpadView, true, dpadView.width * 0.12f, dpadView.height / 2f) }
        58 -> { pressView(btnX, false); pressView(dpadView, false); pressView(btnL, true); pressView(btnR, true) }
        72 -> { pressView(btnSelect, true); pressView(btnStart, true); pressView(btnA, true); pressView(dpadView, true, dpadView.width / 2f, dpadView.height * 0.12f) }
        95 -> {
          pressView(btnL, false); pressView(btnR, false)
          pressView(btnSelect, false); pressView(btnStart, false)
          pressView(btnA, false); pressView(dpadView, false)
        }
      }

      val pressedNames = mutableListOf<String>()
      if (btnL.isCurrentlyPressed()) pressedNames.add("L")
      if (btnR.isCurrentlyPressed()) pressedNames.add("R")
      if (btnY.isCurrentlyPressed()) pressedNames.add("Y")
      if (btnX.isCurrentlyPressed()) pressedNames.add("X")
      if (btnB.isCurrentlyPressed()) pressedNames.add("B")
      if (btnA.isCurrentlyPressed()) pressedNames.add("A")
      if (btnSelect.isCurrentlyPressed()) pressedNames.add("SELECT")
      if (btnStart.isCurrentlyPressed()) pressedNames.add("START")
      if (dpadView.getCurrentDirection() != DPadDirection.CENTER) pressedNames.add("DPAD")
      if (pressedNames.isNotEmpty()) {
        activeLabel = "APPUI ENFONCÉ + VIBRATION : " + pressedNames.joinToString("+")
      } else if (frame in 8..105) {
        activeLabel = "STICKS 3D + VIBRATION HAPTIQUE"
      }

      canvas.drawColor(Color.WHITE)
      root.draw(canvas)

      // Draw live HUD telemetry banner at bottom center for the video
      val hudRect = RectF(width / 2f - 310f, height - 52f, width / 2f + 310f, height - 10f)
      canvas.drawRoundRect(hudRect, 14f, 14f, hudBgPaint)
      canvas.drawRoundRect(hudRect, 14f, 14f, hudStrokePaint)
      val statusText = String.format(
        "L.JS:(%+4d,%+4d) R.JS:(%+4d,%+4d) | %s",
        leftJoystick.getValueX(), leftJoystick.getValueY(),
        rightJoystick.getValueX(), rightJoystick.getValueY(),
        activeLabel
      )
      canvas.drawText(statusText, width / 2f - 292f, height - 25f, hudTextPaint)

      val frameFile = File(outDir, String.format("frame_%03d.png", frame))
      FileOutputStream(frameFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
      }
    }
  }
}
