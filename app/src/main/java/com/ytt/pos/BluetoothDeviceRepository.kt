package com.ytt.pos

import kotlinx.coroutines.flow.Flow

interface BluetoothDeviceRepository {
    val discoveredDevices: Flow<List<DiscoveredDevice>>

    fun startScan()
    fun stopScan()
}

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int? = null,
)
