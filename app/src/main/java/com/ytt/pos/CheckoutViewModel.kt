package com.ytt.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytt.pos.data.db.PrintJobDao
import com.ytt.pos.data.db.PrintJobEntity
import com.ytt.pos.domain.model.Payment
import com.ytt.pos.domain.model.PaymentMethod
import com.ytt.pos.domain.model.PrintStatus
import com.ytt.pos.domain.model.TaxStatus
import com.ytt.pos.domain.model.Transaction
import com.ytt.pos.domain.model.TransactionStatus
import com.ytt.pos.domain.repository.CartRepository
import com.ytt.pos.domain.repository.PaymentRepository
import com.ytt.pos.domain.repository.SettingsRepository
import com.ytt.pos.domain.repository.TransactionRepository
import com.ytt.pos.hardware.payments.mock.PaymentResult
import com.ytt.pos.hardware.payments.mock.PaymentService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAX_RATE = 0.0825
private const val DEFAULT_CURRENCY = "USD"

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository,
    private val paymentRepository: PaymentRepository,
    private val settingsRepository: SettingsRepository,
    private val printerGateway: PrinterGateway,
    private val managerAuthService: ManagerAuthService,
    private val printJobDao: PrintJobDao,
    private val paymentService: PaymentService,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val viewState = MutableStateFlow(CheckoutViewState())

    val uiState: StateFlow<CheckoutUiState> = combine(
        cartRepository.observeCart(),
        settingsRepository.drawerConnected,
        networkMonitor.isOnline,
        viewState,
    ) { cart, drawerConnected, isOnline, state ->
        val subtotal = cart.lines.sumOf { it.lineTotalMinor }
        val tax = if (cart.taxStatus == TaxStatus.EXEMPT_RESALE) 0 else (subtotal * TAX_RATE).toLong()
        val total = subtotal + tax
        CheckoutUiState(
            subtotalMinor = subtotal,
            taxMinor = tax,
            totalMinor = total,
            taxStatus = cart.taxStatus,
            drawerConnected = drawerConnected,
            isOnline = isOnline,
            isProcessing = state.isProcessing,
            isCardPaymentInProgress = state.isCardPaymentInProgress,
            printError = state.printError,
            paymentError = state.paymentError,
            paymentMessage = state.paymentMessage,
            showManagerPinDialog = state.showManagerPinDialog,
            managerPinError = state.managerPinError,
            receiptPrinted = state.receiptPrinted,
            transactionId = state.transactionId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CheckoutUiState(),
    )

    fun onCashClicked() {
        if (viewState.value.isProcessing) return

        viewModelScope.launch {
            val cart = cartRepository.observeCart().first()
            val subtotal = cart.lines.sumOf { it.lineTotalMinor }
            val tax = if (cart.taxStatus == TaxStatus.EXEMPT_RESALE) 0 else (subtotal * TAX_RATE).toLong()
            val total = subtotal + tax

            val transactionId = UUID.randomUUID().toString()
            val paymentId = UUID.randomUUID().toString()
            val transaction = createPendingTransaction(
                cart = cart,
                transactionId = transactionId,
                paymentId = paymentId,
                subtotal = subtotal,
                tax = tax,
                total = total,
            )
            val payment = Payment(
                id = paymentId,
                method = PaymentMethod.CASH,
                amountMinor = total,
                currency = DEFAULT_CURRENCY,
                status = TransactionStatus.PENDING,
            )

            paymentRepository.upsertPayment(payment, transactionId)
            transactionRepository.upsertTransaction(transaction)

            viewState.update {
                it.copy(
                    isProcessing = true,
                    transactionId = transactionId,
                    printError = null,
                    paymentError = null,
                    paymentMessage = null,
                    receiptPrinted = false,
                )
            }

            tryPrint(transaction)
        }
    }

    fun onCardClicked() {
        if (viewState.value.isProcessing || !uiState.value.isOnline) return

        viewModelScope.launch {
            val cart = cartRepository.observeCart().first()
            val subtotal = cart.lines.sumOf { it.lineTotalMinor }
            val tax = if (cart.taxStatus == TaxStatus.EXEMPT_RESALE) 0 else (subtotal * TAX_RATE).toLong()
            val total = subtotal + tax
            val transactionId = UUID.randomUUID().toString()
            val paymentId = UUID.randomUUID().toString()

            viewState.update {
                it.copy(
                    isCardPaymentInProgress = true,
                    paymentError = null,
                    paymentMessage = null,
                    printError = null,
                    receiptPrinted = false,
                )
            }

            when (val result = paymentService.startCardPayment(total, DEFAULT_CURRENCY, transactionId)) {
                is PaymentResult.Approved -> {
                    val transaction = createPendingTransaction(
                        cart = cart,
                        transactionId = transactionId,
                        paymentId = paymentId,
                        subtotal = subtotal,
                        tax = tax,
                        total = total,
                    )
                    val payment = Payment(
                        id = paymentId,
                        method = PaymentMethod.CARD,
                        amountMinor = total,
                        currency = DEFAULT_CURRENCY,
                        providerRef = result.providerRef,
                        status = TransactionStatus.PENDING,
                    )

                    paymentRepository.upsertPayment(payment, transactionId)
                    transactionRepository.upsertTransaction(transaction)

                    viewState.update {
                        it.copy(
                            isCardPaymentInProgress = false,
                            isProcessing = true,
                            transactionId = transactionId,
                        )
                    }

                    tryPrint(transaction)
                }

                is PaymentResult.Declined -> {
                    viewState.update {
                        it.copy(
                            isCardPaymentInProgress = false,
                            paymentError = result.reason,
                        )
                    }
                }

                is PaymentResult.Cancelled -> {
                    viewState.update {
                        it.copy(
                            isCardPaymentInProgress = false,
                            paymentMessage = result.message,
                        )
                    }
                }

                is PaymentResult.Failed -> {
                    viewState.update {
                        it.copy(
                            isCardPaymentInProgress = false,
                            paymentError = result.error,
                        )
                    }
                }
            }
        }
    }

    fun dismissPaymentError() {
        viewState.update { it.copy(paymentError = null) }
    }

    fun dismissPaymentMessage() {
        viewState.update { it.copy(paymentMessage = null) }
    }

    fun onRetryPrint() {
        val transactionId = viewState.value.transactionId ?: return
        if (viewState.value.isProcessing) return

        viewModelScope.launch {
            val transaction = transactionRepository.observeTransaction(transactionId).first() ?: return@launch

            viewState.update { it.copy(isProcessing = true, printError = null) }
            tryPrint(transaction)
        }
    }

    fun onSkipPrintRequested() {
        viewState.update {
            it.copy(
                showManagerPinDialog = true,
                managerPinError = null,
            )
        }
    }

    fun onManagerPinDismissed() {
        viewState.update {
            it.copy(showManagerPinDialog = false, managerPinError = null)
        }
    }

    fun onManagerPinSubmitted(pin: String) {
        if (!managerAuthService.validatePin(pin)) {
            viewState.update { it.copy(managerPinError = "Invalid manager PIN") }
            return
        }

        val transactionId = viewState.value.transactionId ?: return
        viewModelScope.launch {
            val existing = transactionRepository.observeTransaction(transactionId).first() ?: return@launch
            val skippedTransaction = existing.copy(
                status = TransactionStatus.COMPLETED,
                printStatus = PrintStatus.SKIPPED_BY_MANAGER,
            )
            transactionRepository.upsertTransaction(skippedTransaction)
            transactionRepository.updateStatus(transactionId, TransactionStatus.SYNC_PENDING)
            viewState.update {
                it.copy(
                    showManagerPinDialog = false,
                    managerPinError = null,
                    printError = null,
                    isProcessing = false,
                    receiptPrinted = false,
                )
            }
        }
    }

    private fun createPendingTransaction(
        cart: com.ytt.pos.domain.model.Cart,
        transactionId: String,
        paymentId: String,
        subtotal: Long,
        tax: Long,
        total: Long,
    ): Transaction = Transaction(
        id = transactionId,
        createdAtEpochMs = System.currentTimeMillis(),
        lines = cart.lines,
        subtotalMinor = subtotal,
        taxMinor = tax,
        totalMinor = total,
        taxStatus = cart.taxStatus,
        permitSnapshot = cart.permitSnapshot,
        customerId = cart.customerId,
        paymentId = paymentId,
        status = TransactionStatus.PENDING,
        printStatus = PrintStatus.NOT_PRINTED,
    )

    private fun tryPrint(transaction: Transaction) {
        viewModelScope.launch {
            val attempt = (viewState.value.printAttempts + 1)
            val printResult = printerGateway.printReceipt(transaction.id)
            printJobDao.upsert(
                PrintJobEntity(
                    id = "${transaction.id}-$attempt",
                    transactionId = transaction.id,
                    status = if (printResult.isSuccess) "SUCCESS" else "FAILED",
                    attempts = attempt,
                    lastError = printResult.exceptionOrNull()?.message,
                    createdAtEpochMs = System.currentTimeMillis(),
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )

            if (printResult.isSuccess) {
                val printedTransaction = transaction.copy(
                    printStatus = PrintStatus.PRINTED,
                    status = TransactionStatus.COMPLETED,
                )
                transactionRepository.upsertTransaction(printedTransaction)
                transactionRepository.updateStatus(transaction.id, TransactionStatus.SYNC_PENDING)

                if (uiState.value.drawerConnected) {
                    printerGateway.openCashDrawer()
                }

                viewState.update {
                    it.copy(
                        isProcessing = false,
                        printError = null,
                        receiptPrinted = true,
                        printAttempts = attempt,
                    )
                }
            } else {
                val failedTransaction = transaction.copy(printStatus = PrintStatus.FAILED)
                transactionRepository.upsertTransaction(failedTransaction)
                viewState.update {
                    it.copy(
                        isProcessing = false,
                        printError = printResult.exceptionOrNull()?.message ?: "Unable to print receipt",
                        printAttempts = attempt,
                    )
                }
            }
        }
    }
}

data class CheckoutUiState(
    val subtotalMinor: Long = 0,
    val taxMinor: Long = 0,
    val totalMinor: Long = 0,
    val taxStatus: TaxStatus = TaxStatus.TAXABLE,
    val drawerConnected: Boolean = false,
    val isOnline: Boolean = false,
    val isProcessing: Boolean = false,
    val isCardPaymentInProgress: Boolean = false,
    val printError: String? = null,
    val paymentError: String? = null,
    val paymentMessage: String? = null,
    val showManagerPinDialog: Boolean = false,
    val managerPinError: String? = null,
    val receiptPrinted: Boolean = false,
    val transactionId: String? = null,
)

private data class CheckoutViewState(
    val isProcessing: Boolean = false,
    val isCardPaymentInProgress: Boolean = false,
    val printError: String? = null,
    val paymentError: String? = null,
    val paymentMessage: String? = null,
    val showManagerPinDialog: Boolean = false,
    val managerPinError: String? = null,
    val receiptPrinted: Boolean = false,
    val transactionId: String? = null,
    val printAttempts: Int = 0,
)
