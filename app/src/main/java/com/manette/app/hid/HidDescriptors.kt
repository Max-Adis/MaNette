package com.manette.app.hid

/**
 * HID Gamepad Report Descriptor
 * Compatible with standard HID gamepad / Xbox-like controllers.
 * Report format (6 bytes total):
 *   Byte 0:   Buttons bitmask (A=bit0, B=bit1, X=bit2, Y=bit3, LB=bit4, RB=bit5, SELECT=bit6, START=bit7)
 *   Byte 1:   D-Pad HAT switch (bits 0-3) + padding (bits 4-7)
 *   Byte 2:   Left Joystick X  (-127 to 127)
 *   Byte 3:   Left Joystick Y  (-127 to 127)
 *   Byte 4:   Right Joystick X (-127 to 127)
 *   Byte 5:   Right Joystick Y (-127 to 127)
 */
object HidDescriptors {

    val GAMEPAD_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop Ctrls)
        0x09.toByte(), 0x05.toByte(),  // Usage (Gamepad)
        0xA1.toByte(), 0x01.toByte(),  // Collection (Application)

        // ── Buttons ──────────────────────────────────────────────
        0x05.toByte(), 0x09.toByte(),  // Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),  // Usage Minimum (Button 1 = A)
        0x29.toByte(), 0x08.toByte(),  // Usage Maximum (Button 8 = START)
        0x15.toByte(), 0x00.toByte(),  // Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),  // Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),  // Report Size (1 bit)
        0x95.toByte(), 0x08.toByte(),  // Report Count (8 buttons)
        0x81.toByte(), 0x02.toByte(),  // Input (Data, Variable, Absolute)

        // ── D-Pad HAT switch ─────────────────────────────────────
        0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
        0x09.toByte(), 0x39.toByte(),  // Usage (Hat switch)
        0x15.toByte(), 0x01.toByte(),  // Logical Minimum (1)
        0x25.toByte(), 0x08.toByte(),  // Logical Maximum (8)
        0x35.toByte(), 0x00.toByte(),  // Physical Minimum (0)
        0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // Physical Maximum (315)
        0x65.toByte(), 0x14.toByte(),  // Unit (Degree)
        0x75.toByte(), 0x04.toByte(),  // Report Size (4 bits)
        0x95.toByte(), 0x01.toByte(),  // Report Count (1)
        0x81.toByte(), 0x42.toByte(),  // Input (Data, Variable, Absolute, Null State)

        // Padding to align to byte
        0x65.toByte(), 0x00.toByte(),  // Unit (None)
        0x75.toByte(), 0x04.toByte(),  // Report Size (4 bits)
        0x95.toByte(), 0x01.toByte(),  // Report Count (1)
        0x81.toByte(), 0x03.toByte(),  // Input (Constant) – padding

        // ── Analog axes ──────────────────────────────────────────
        0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),  // Usage (X)   – Left JS X
        0x09.toByte(), 0x31.toByte(),  // Usage (Y)   – Left JS Y
        0x09.toByte(), 0x32.toByte(),  // Usage (Z)   – Right JS X
        0x09.toByte(), 0x35.toByte(),  // Usage (Rz)  – Right JS Y
        0x15.toByte(), 0x81.toByte(),  // Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),  // Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),  // Report Size (8 bits)
        0x95.toByte(), 0x04.toByte(),  // Report Count (4 axes)
        0x81.toByte(), 0x02.toByte(),  // Input (Data, Variable, Absolute)

        0xC0.toByte()                  // End Collection
    )

    // SDP Record for Bluetooth Classic HID
    const val REPORT_MAP_SIZE = 6 // bytes per report
}

/**
 * D-Pad direction constants (HAT switch values)
 */
object DPadDirection {
    const val CENTER: Byte = 0x00  // No direction (null state in HAT)
    const val UP: Byte = 0x01
    const val UP_RIGHT: Byte = 0x02
    const val RIGHT: Byte = 0x03
    const val DOWN_RIGHT: Byte = 0x04
    const val DOWN: Byte = 0x05
    const val DOWN_LEFT: Byte = 0x06
    const val LEFT: Byte = 0x07
    const val UP_LEFT: Byte = 0x08
}

/**
 * Button bitmask constants
 */
object ButtonMask {
    const val A: Int = 0x01
    const val B: Int = 0x02
    const val X: Int = 0x04
    const val Y: Int = 0x08
    const val LB: Int = 0x10
    const val RB: Int = 0x20
    const val SELECT: Int = 0x40
    const val START: Int = 0x80
}
