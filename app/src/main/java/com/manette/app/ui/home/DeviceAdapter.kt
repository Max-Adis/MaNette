package com.manette.app.ui.home

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manette.app.databinding.ItemDeviceBinding

/**
 * RecyclerView adapter for the Bluetooth device list on the Home screen.
 */
class DeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : ListAdapter<BluetoothDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiff()) {

    private var selectedAddress: String? = null

    inner class DeviceViewHolder(val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("MissingPermission")
        fun bind(device: BluetoothDevice, isSelected: Boolean) {
            binding.tvDeviceName.text = device.name ?: "Appareil inconnu"
            val bonded = device.bondState == BluetoothDevice.BOND_BONDED
            binding.tvDeviceAddress.text = if (bonded) {
                "Associé · ${device.address}"
            } else {
                device.address
            }
            binding.tvDeviceType.text = typeLabel(device)
            binding.root.isSelected = isSelected
            binding.root.setOnClickListener {
                selectedAddress = device.address
                notifyDataSetChanged()
                onDeviceClick(device)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = getItem(position)
        holder.bind(device, device.address == selectedAddress)
    }

    @SuppressLint("MissingPermission")
    private fun typeLabel(device: BluetoothDevice): String {
        val kind = when (device.bluetoothClass?.majorDeviceClass) {
            BluetoothClass.Device.Major.COMPUTER -> "PC"
            BluetoothClass.Device.Major.PHONE -> "TEL"
            BluetoothClass.Device.Major.PERIPHERAL -> "HID"
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "AV"
            BluetoothClass.Device.Major.WEARABLE -> "WEAR"
            else -> when (device.type) {
                BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "DUAL"
                else -> "BT"
            }
        }
        return if (device.bondState == BluetoothDevice.BOND_BONDED) "✓ $kind" else kind
    }

    class DeviceDiff : DiffUtil.ItemCallback<BluetoothDevice>() {
        @SuppressLint("MissingPermission")
        override fun areItemsTheSame(a: BluetoothDevice, b: BluetoothDevice) = a.address == b.address

        @SuppressLint("MissingPermission")
        override fun areContentsTheSame(a: BluetoothDevice, b: BluetoothDevice): Boolean {
            return a.address == b.address &&
                a.bondState == b.bondState &&
                a.name == b.name &&
                a.type == b.type
        }
    }
}
