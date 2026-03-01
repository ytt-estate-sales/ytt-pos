package com.ytt.pos.hardware.printer.star

import android.content.Context
import com.ytt.pos.domain.receipt.ReceiptContent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface StarPrinterStatus {
    data object Ready : StarPrinterStatus
    data object Offline : StarPrinterStatus
    data object PaperOut : StarPrinterStatus
    data object CoverOpen : StarPrinterStatus
    data class Error(val message: String) : StarPrinterStatus
}

@Singleton
class StarPrinterService @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private var managerHandle: StarManagerHandle? = null
    private var currentStatus: StarPrinterStatus = StarPrinterStatus.Offline

    fun isAvailable(): Boolean = runCatching {
        Class.forName("com.starmicronics.starioextension.StarIoExtManager")
    }.isSuccess

    fun connect(deviceId: String): Result<Unit> = runCatching {
        disconnect()
        val handle = StarManagerHandle.create(
            context = appContext,
            portName = if (deviceId.startsWith("BT:")) deviceId else "BT:$deviceId",
        )
        handle.connect()
        managerHandle = handle
        currentStatus = mapStatus(handle.status())
        if (currentStatus !is StarPrinterStatus.Ready) {
            throw IllegalStateException(readableStatusMessage(currentStatus))
        }
    }.onFailure {
        currentStatus = StarPrinterStatus.Error(it.message ?: "Unable to connect to Star printer")
        disconnect()
    }.mapError("Unable to connect to Star printer")

    fun disconnect() {
        runCatching { managerHandle?.disconnect() }
        managerHandle = null
        currentStatus = StarPrinterStatus.Offline
    }

    fun status(): StarPrinterStatus {
        val handle = managerHandle ?: return StarPrinterStatus.Offline
        return runCatching {
            mapStatus(handle.status())
        }.onSuccess {
            currentStatus = it
        }.getOrElse {
            currentStatus = StarPrinterStatus.Error("Printer status unavailable")
            currentStatus
        }
    }

    fun printReceipt(receipt: ReceiptContent): Result<Unit> {
        val handle = managerHandle ?: return Result.failure(
            IllegalStateException("Printer is not connected"),
        )

        return runCatching {
            val payload = StarReceiptCommandBuilder.build(receipt)
            handle.sendCommands(payload)
            currentStatus = mapStatus(handle.status())
        }.onFailure {
            currentStatus = StarPrinterStatus.Error(it.message ?: "Failed to print receipt")
        }.mapError("Failed to print receipt")
    }

    fun openCashDrawer(drawerConnected: Boolean): Result<Unit> {
        if (!drawerConnected) return Result.success(Unit)

        val handle = managerHandle ?: return Result.failure(
            IllegalStateException("Printer is not connected"),
        )

        return runCatching {
            val payload = StarReceiptCommandBuilder.buildDrawerKick()
            handle.sendCommands(payload)
            currentStatus = mapStatus(handle.status())
        }.onFailure {
            currentStatus = StarPrinterStatus.Error(it.message ?: "Failed to open cash drawer")
        }.mapError("Failed to open cash drawer")
    }

    private fun mapStatus(status: StarStatus): StarPrinterStatus = when {
        status.offline -> StarPrinterStatus.Offline
        status.paperOut -> StarPrinterStatus.PaperOut
        status.coverOpen -> StarPrinterStatus.CoverOpen
        status.errorMessage != null -> StarPrinterStatus.Error(status.errorMessage)
        else -> StarPrinterStatus.Ready
    }

    private fun readableStatusMessage(status: StarPrinterStatus): String = when (status) {
        StarPrinterStatus.Ready -> "Printer ready"
        StarPrinterStatus.Offline -> "Printer is offline"
        StarPrinterStatus.PaperOut -> "Printer is out of paper"
        StarPrinterStatus.CoverOpen -> "Printer cover is open"
        is StarPrinterStatus.Error -> status.message
    }
}

private data class StarStatus(
    val offline: Boolean,
    val paperOut: Boolean,
    val coverOpen: Boolean,
    val errorMessage: String? = null,
)

private class StarManagerHandle private constructor(
    private val manager: Any,
) {
    fun connect() {
        manager.javaClass.getMethod("connect").invoke(manager)
    }

    fun disconnect() {
        runCatching { manager.javaClass.getMethod("disconnect").invoke(manager) }
    }

    fun status(): StarStatus {
        val statusObj = manager.javaClass.getMethod("getPortStatus").invoke(manager)
        val statusClass = statusObj.javaClass
        return StarStatus(
            offline = statusClass.getField("offline").getBoolean(statusObj),
            paperOut = statusClass.getField("receiptPaperEmpty").getBoolean(statusObj),
            coverOpen = statusClass.getField("coverOpen").getBoolean(statusObj),
            errorMessage = null,
        )
    }

    fun sendCommands(commands: ByteArray) {
        val port = manager.javaClass.getMethod("getPort").invoke(manager)
            ?: throw IllegalStateException("Printer port is unavailable")
        val method = port.javaClass.methods.firstOrNull {
            it.name == "writePort" && it.parameterTypes.size >= 3
        } ?: throw IllegalStateException("StarPRNT writePort API unavailable")

        method.invoke(port, commands, 0, commands.size)
    }

    companion object {
        fun create(context: Context, portName: String): StarManagerHandle {
            val managerClass = Class.forName("com.starmicronics.starioextension.StarIoExtManager")
            val typeClass = Class.forName("com.starmicronics.starioextension.StarIoExtManager\$Type")
            val typeEnum = typeClass.enumConstants.first { (it as Enum<*>).name == "Standard" }
            val ctor = managerClass.getConstructor(typeClass, String::class.java, String::class.java, Int::class.javaPrimitiveType, Context::class.java)
            val manager = ctor.newInstance(typeEnum, portName, "", 10_000, context)
            return StarManagerHandle(manager)
        }
    }
}

private fun <T> Result<T>.mapError(userMessage: String): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = {
        val details = it.message?.takeIf(String::isNotBlank)?.let { message -> ": $message" } ?: ""
        Result.failure(IllegalStateException(userMessage + details, it))
    },
)
