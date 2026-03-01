package com.ytt.pos

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AndroidBluetoothDeviceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : BluetoothDeviceRepository {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val devices = linkedMapOf<String, DiscoveredDevice>()
    private val discoveredDevicesFlow = MutableStateFlow<List<DiscoveredDevice>>(emptyList())

    override val discoveredDevices: Flow<List<DiscoveredDevice>> = discoveredDevicesFlow.asStateFlow()

    private var receiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                    val rssi = if (intent.hasExtra(BluetoothDevice.EXTRA_RSSI)) {
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    } else {
                        null
                    }
                    val updated = DiscoveredDevice(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        rssi = rssi,
                    )
                    devices[updated.address] = updated
                    discoveredDevicesFlow.value = devices.values.toList().sortedBy { it.name }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        val adapter = bluetoothAdapter ?: return
        registerReceiverIfNeeded()
        devices.clear()
        discoveredDevicesFlow.value = emptyList()
        adapter.cancelDiscovery()
        adapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        bluetoothAdapter?.cancelDiscovery()
        unregisterReceiverIfNeeded()
    }

    private fun registerReceiverIfNeeded() {
        if (receiverRegistered) return
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(discoveryReceiver, filter)
        receiverRegistered = true
    }

    private fun unregisterReceiverIfNeeded() {
        if (!receiverRegistered) return
        runCatching { context.unregisterReceiver(discoveryReceiver) }
        receiverRegistered = false
    }
}
