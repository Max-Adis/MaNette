package com.manette.app.ui.home

import android.annotation.SuppressLint
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

    private var selectedPosition = -1

    inner class DeviceViewHolder(val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("MissingPermission")
        fun bind(device: BluetoothDevice, isSelected: Boolean) {
            binding.tvDeviceName.text = device.name ?: "Appareil inconnu"
            binding.tvDeviceAddress.text = device.address
            binding.tvDeviceType.text = when (device.type) {
                BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "DUAL"
                else -> "BT"
            }
            binding.root.isSelected = isSelected
            binding.root.setOnClickListener {
                val prev = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPosition)
                onDeviceClick(device)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    class DeviceDiff : DiffUtil.ItemCallback<BluetoothDevice>() {
        override fun areItemsTheSame(a: BluetoothDevice, b: BluetoothDevice) = a.address == b.address
        override fun areContentsTheSame(a: BluetoothDevice, b: BluetoothDevice) = a.address == b.address
    }
}
