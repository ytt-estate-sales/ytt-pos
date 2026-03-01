package com.ytt.pos

import com.ytt.pos.domain.receipt.ReceiptContent
import com.ytt.pos.domain.repository.SettingsRepository
import com.ytt.pos.hardware.payments.mock.PaymentService
import com.ytt.pos.hardware.printer.star.StarPrinterService
import com.ytt.pos.hardware.printer.star.StarPrinterStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

@Singleton
class HardwareManager @Inject constructor(
    private val fakePrinterGateway: FakePrinterGateway,
    private val starPrinterService: StarPrinterService,
    private val paymentService: PaymentService,
    private val settingsRepository: SettingsRepository,
) : PrinterGateway {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val printerStatusFlow = MutableStateFlow(PrinterStatus.OFFLINE)
    private val readerStatusFlow = MutableStateFlow(ReaderStatus.OFFLINE)

    val uiState: StateFlow<HardwareUiState> = combine(
        settingsRepository.selectedPrinterId,
        settingsRepository.selectedReaderId,
        settingsRepository.drawerConnected,
        printerStatusFlow,
        readerStatusFlow,
    ) { selectedPrinterId, selectedReaderId, drawerConnected, printerStatus, readerStatus ->
        HardwareUiState(
            printerStatus = printerStatus,
            readerStatus = readerStatus,
            selectedPrinterId = selectedPrinterId,
            selectedReaderId = selectedReaderId,
            drawerConnected = drawerConnected,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = HardwareUiState(),
    )

    override suspend fun printReceipt(transactionId: String): Result<Unit> {
        if (!starPrinterService.isAvailable()) {
            return fakePrinterGateway.printReceipt(transactionId)
        }

        connectStarPrinterIfSelected()
        val receipt = ReceiptContent(lines = listOf("TRANSACTION $transactionId"))
        val result = starPrinterService.printReceipt(receipt)
        printerStatusFlow.value = mapStarStatus(starPrinterService.status())
        return result
    }

    override suspend fun openCashDrawer(): Result<Unit> {
        if (!starPrinterService.isAvailable()) {
            return fakePrinterGateway.openCashDrawer()
        }

        connectStarPrinterIfSelected()
        val result = starPrinterService.openCashDrawer()
        printerStatusFlow.value = mapStarStatus(starPrinterService.status())
        return result
    }

    override suspend fun status(): PrinterStatus {
        val status = if (starPrinterService.isAvailable()) {
            mapStarStatus(starPrinterService.status())
        } else {
            fakePrinterGateway.status()
        }
        printerStatusFlow.value = status
        return status
    }

    suspend fun setSelectedPrinterId(printerId: String?) = settingsRepository.setSelectedPrinterId(printerId)

    suspend fun setSelectedReaderId(readerId: String?) = settingsRepository.setSelectedReaderId(readerId)

    suspend fun setDrawerConnected(connected: Boolean) = settingsRepository.setDrawerConnected(connected)

    suspend fun connectPrinter(deviceId: String): Result<Unit> {
        val result = if (starPrinterService.isAvailable()) {
            starPrinterService.connect(deviceId)
        } else {
            Result.failure(IllegalStateException("Bluetooth printer is not available"))
        }
        settingsRepository.setSelectedPrinterId(deviceId)
        printerStatusFlow.value = mapStarStatus(starPrinterService.status())
        return result
    }

    suspend fun reconnectAll() {
        val selectedPrinterId = settingsRepository.selectedPrinterId.first()
        if (selectedPrinterId != null) {
            if (starPrinterService.isAvailable()) {
                starPrinterService.connect(selectedPrinterId)
                printerStatusFlow.value = mapStarStatus(starPrinterService.status())
            } else {
                printerStatusFlow.value = status()
            }
        } else {
            printerStatusFlow.value = status()
        }
        paymentService.reconnect()
        readerStatusFlow.value = paymentService.status()
    }

    suspend fun testPrint(): Result<Unit> = printReceipt("hardware-test")

    suspend fun testReader(): Result<Unit> = paymentService.testReader()

    private suspend fun connectStarPrinterIfSelected() {
        val selectedPrinterId = settingsRepository.selectedPrinterId.first() ?: return
        if (starPrinterService.status() == StarPrinterStatus.Ready) return
        starPrinterService.connect(selectedPrinterId)
    }

    private fun mapStarStatus(status: StarPrinterStatus): PrinterStatus = when (status) {
        StarPrinterStatus.Ready -> PrinterStatus.READY
        StarPrinterStatus.Offline -> PrinterStatus.OFFLINE
        StarPrinterStatus.PaperOut -> PrinterStatus.ERROR
        is StarPrinterStatus.Error -> PrinterStatus.ERROR
    }
}

data class HardwareUiState(
    val printerStatus: PrinterStatus = PrinterStatus.OFFLINE,
    val readerStatus: ReaderStatus = ReaderStatus.OFFLINE,
    val selectedPrinterId: String? = null,
    val selectedReaderId: String? = null,
    val drawerConnected: Boolean = false,
)
