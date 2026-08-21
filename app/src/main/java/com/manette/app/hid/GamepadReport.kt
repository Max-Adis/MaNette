package com.manette.app.hid

/**
 * Gamepad HID report state.
 * Encapsulates the full state of the gamepad and serializes it to a byte array
 * for transmission over Bluetooth HID.
 */
data class GamepadReport(
    var buttons: Int = 0,           // Bitmask: A|B|X|Y|LB|RB|SELECT|START
    var dpad: Byte = DPadDirection.CENTER, // HAT switch value
    var leftX: Int = 0,             // Left joystick X (-127..127)
    var leftY: Int = 0,             // Left joystick Y (-127..127)
    var rightX: Int = 0,            // Right joystick X (-127..127)
    var rightY: Int = 0             // Right joystick Y (-127..127)
) {
    /**
     * Serialize to 6-byte HID report.
     */
    fun toByteArray(): ByteArray {
        return byteArrayOf(
            buttons.toByte(),               // Byte 0: button mask
            (dpad.toInt() and 0x0F).toByte(), // Byte 1: HAT (lower nibble) + 0 padding (upper)
            leftX.clampAxis().toByte(),     // Byte 2: Left JS X
            leftY.clampAxis().toByte(),     // Byte 3: Left JS Y
            rightX.clampAxis().toByte(),    // Byte 4: Right JS X
            rightY.clampAxis().toByte()     // Byte 5: Right JS Y
        )
    }

    /** Human-readable debug string */
    fun toDebugString(): String {
        val btns = buildList {
            if (buttons and ButtonMask.A != 0) add("A")
            if (buttons and ButtonMask.B != 0) add("B")
            if (buttons and ButtonMask.X != 0) add("X")
            if (buttons and ButtonMask.Y != 0) add("Y")
            if (buttons and ButtonMask.LB != 0) add("LB")
            if (buttons and ButtonMask.RB != 0) add("RB")
            if (buttons and ButtonMask.SELECT != 0) add("SEL")
            if (buttons and ButtonMask.START != 0) add("STA")
        }
        val dpadStr = when (dpad) {
            DPadDirection.UP -> "↑"
            DPadDirection.DOWN -> "↓"
            DPadDirection.LEFT -> "←"
            DPadDirection.RIGHT -> "→"
            DPadDirection.UP_LEFT -> "↖"
            DPadDirection.UP_RIGHT -> "↗"
            DPadDirection.DOWN_LEFT -> "↙"
            DPadDirection.DOWN_RIGHT -> "↘"
            else -> "·"
        }
        return "Buttons: [${btns.joinToString(",")}]  DPad: $dpadStr\n" +
               "L.JS: (${leftX.clampAxis()}, ${leftY.clampAxis()})  " +
               "R.JS: (${rightX.clampAxis()}, ${rightY.clampAxis()})"
    }

    /** Press a button */
    fun pressButton(mask: Int) { buttons = buttons or mask }

    /** Release a button */
    fun releaseButton(mask: Int) { buttons = buttons and mask.inv() }

    /** Reset all inputs to neutral */
    fun reset() {
        buttons = 0
        dpad = DPadDirection.CENTER
        leftX = 0; leftY = 0
        rightX = 0; rightY = 0
    }

    private fun Int.clampAxis(): Int = this.coerceIn(-127, 127)
}
