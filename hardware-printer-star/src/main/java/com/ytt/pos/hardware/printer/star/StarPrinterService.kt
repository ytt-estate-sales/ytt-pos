package com.ytt.pos.hardware.printer.star

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.ytt.pos.domain.receipt.ReceiptContent
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface StarPrinterStatus {
    data object Ready : StarPrinterStatus
    data object Offline : StarPrinterStatus
    data object PaperOut : StarPrinterStatus
    data class Error(val message: String) : StarPrinterStatus
}

@Singleton
class StarPrinterService @Inject constructor(
    private val receiptTextRenderer: StarReceiptTextRenderer,
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var currentStatus: StarPrinterStatus = StarPrinterStatus.Offline

    fun isAvailable(): Boolean = bluetoothAdapter != null

    @SuppressLint("MissingPermission")
    fun connect(deviceId: String): Result<Unit> {
        val adapter = bluetoothAdapter ?: return Result.failure(
            IllegalStateException("Bluetooth is not available on this device"),
        )

        return runCatching {
            val device = adapter.bondedDevices.firstOrNull { it.address == deviceId || it.name == deviceId }
                ?: throw IllegalStateException("Printer device not paired: $deviceId")

            adapter.cancelDiscovery()
            bluetoothSocket?.close()
            bluetoothSocket = createSocket(device).also { it.connect() }
            currentStatus = StarPrinterStatus.Ready
        }.onFailure {
            currentStatus = StarPrinterStatus.Error(it.message ?: "Unable to connect to printer")
            disconnect()
        }
    }

    fun disconnect() {
        runCatching { bluetoothSocket?.close() }
        bluetoothSocket = null
        currentStatus = StarPrinterStatus.Offline
    }

    fun status(): StarPrinterStatus = currentStatus

    fun printReceipt(receipt: ReceiptContent): Result<Unit> {
        val socket = bluetoothSocket ?: return Result.failure(
            IllegalStateException("Printer is not connected"),
        )

        return runCatching {
            val renderedText = receiptTextRenderer.render(receipt)
            // TODO: Replace plain-text payload with StarPRNT command builder output.
            socket.outputStream.write(renderedText.toByteArray(Charsets.UTF_8))
            socket.outputStream.flush()
            currentStatus = StarPrinterStatus.Ready
        }.onFailure {
            currentStatus = StarPrinterStatus.Error(it.message ?: "Failed to print receipt")
        }
    }

    fun openCashDrawer(): Result<Unit> {
        val socket = bluetoothSocket ?: return Result.failure(
            IllegalStateException("Printer is not connected"),
        )

        return runCatching {
            // TODO: Replace stub pulse command with StarPRNT cash drawer kick command.
            val stubDrawerKickCommand = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
            socket.outputStream.write(stubDrawerKickCommand)
            socket.outputStream.flush()
            currentStatus = StarPrinterStatus.Ready
        }.onFailure {
            currentStatus = StarPrinterStatus.Error(it.message ?: "Failed to open cash drawer")
        }
    }

    private fun createSocket(device: BluetoothDevice): BluetoothSocket {
        val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        return device.createRfcommSocketToServiceRecord(sppUuid)
    }
}
