# MaNette – proguard rules
-keep class com.manette.app.** { *; }
-keepclassmembers class * extends android.bluetooth.BluetoothProfile$ServiceListener { *; }
