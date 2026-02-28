package com.ytt.pos

import com.ytt.pos.domain.repository.SettingsRepository
import com.ytt.pos.hardware.payments.mock.PaymentService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Singleton
class HardwareManager @Inject constructor(
    private val printerDelegate: FakePrinterGateway,
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

    override suspend fun printReceipt(transactionId: String): Result<Unit> =
        printerDelegate.printReceipt(transactionId)

    override suspend fun openCashDrawer(): Result<Unit> =
        printerDelegate.openCashDrawer()

    override suspend fun status(): PrinterStatus = printerDelegate.status()

    suspend fun setSelectedPrinterId(printerId: String?) = settingsRepository.setSelectedPrinterId(printerId)

    suspend fun setSelectedReaderId(readerId: String?) = settingsRepository.setSelectedReaderId(readerId)

    suspend fun setDrawerConnected(connected: Boolean) = settingsRepository.setDrawerConnected(connected)

    suspend fun reconnectAll() {
        printerStatusFlow.value = printerDelegate.status()
        paymentService.reconnect()
        readerStatusFlow.value = paymentService.status()
    }

    suspend fun testPrint(): Result<Unit> = printReceipt("hardware-test")

    suspend fun testReader(): Result<Unit> = paymentService.testReader()
}

data class HardwareUiState(
    val printerStatus: PrinterStatus = PrinterStatus.OFFLINE,
    val readerStatus: ReaderStatus = ReaderStatus.OFFLINE,
    val selectedPrinterId: String? = null,
    val selectedReaderId: String? = null,
    val drawerConnected: Boolean = false,
)
