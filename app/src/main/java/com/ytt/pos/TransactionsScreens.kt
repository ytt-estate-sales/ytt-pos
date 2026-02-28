package com.ytt.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
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
import com.ytt.pos.domain.repository.PaymentRepository
import com.ytt.pos.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@HiltViewModel
class TransactionsListViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {
    val uiState: StateFlow<TransactionsListUiState> = transactionRepository.observeTransactions()
        .mapLatest { transactions ->
            val items = transactions.map { transaction ->
                val paymentMethod = transaction.paymentId
                    ?.let { paymentRepository.observePayment(it).first()?.method }
                    ?: paymentRepository.observePayments(transaction.id).first().lastOrNull()?.method
                TransactionsListItem(
                    id = transaction.id,
                    createdAtLabel = formatter.format(
                        Instant.ofEpochMilli(transaction.createdAtEpochMs).atZone(ZoneId.systemDefault()),
                    ),
                    totalMinor = transaction.totalMinor,
                    paymentMethod = paymentMethod,
                    taxStatus = transaction.taxStatus,
                    printStatus = transaction.printStatus,
                )
            }
            TransactionsListUiState(items = items)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionsListUiState(),
        )
}

@Composable
fun TransactionsListScreen(
    onNavigateBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    viewModel: TransactionsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Transactions", style = MaterialTheme.typography.headlineSmall)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTransaction(item.id) },
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = item.createdAtLabel, fontWeight = FontWeight.SemiBold)
                        Text(text = "Total: ${formatMinor(item.totalMinor)}")
                        Text(text = "Payment: ${item.paymentMethod ?: "UNKNOWN"}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.taxStatus == TaxStatus.EXEMPT_RESALE) {
                                AssistChip(onClick = {}, label = { Text("EXEMPT") })
                            }
                            AssistChip(onClick = {}, label = { Text(item.printStatus.name) })
                        }
                    }
                }
            }
        }
        Button(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}

data class TransactionsListUiState(
    val items: List<TransactionsListItem> = emptyList(),
)

data class TransactionsListItem(
    val id: String,
    val createdAtLabel: String,
    val totalMinor: Long,
    val paymentMethod: PaymentMethod?,
    val taxStatus: TaxStatus,
    val printStatus: PrintStatus,
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    paymentRepository: PaymentRepository,
    private val printerGateway: PrinterGateway,
    private val managerAuthService: ManagerAuthService,
    private val printJobDao: PrintJobDao,
) : ViewModel() {
    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])
    private val viewState = MutableStateFlow(TransactionDetailViewState())

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        transactionRepository.observeTransaction(transactionId),
        paymentRepository.observePayments(transactionId),
        viewState,
    ) { transaction, payments, localState ->
        TransactionDetailUiState(
            transaction = transaction,
            payment = payments.lastOrNull(),
            isReprinting = localState.isReprinting,
            printError = localState.printError,
            showManagerPinDialog = localState.showManagerPinDialog,
            managerPinError = localState.managerPinError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionDetailUiState(),
    )

    fun onReprintReceipt() {
        if (viewState.value.isReprinting) return

        viewModelScope.launch {
            val transaction = uiState.value.transaction ?: return@launch
            viewState.update { it.copy(isReprinting = true, printError = null) }
            val attempt = viewState.value.printAttempts + 1
            val printResult = printerGateway.printReceipt(transaction.id)
            printJobDao.upsert(
                PrintJobEntity(
                    id = "${transaction.id}-reprint-$attempt-${System.currentTimeMillis()}",
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
                    status = TransactionStatus.COMPLETED,
                    printStatus = PrintStatus.PRINTED,
                )
                transactionRepository.upsertTransaction(printedTransaction)
                transactionRepository.updateStatus(transaction.id, TransactionStatus.SYNC_PENDING)
                viewState.update { it.copy(isReprinting = false, printError = null, printAttempts = attempt) }
            } else {
                val failedTransaction = transaction.copy(printStatus = PrintStatus.FAILED)
                transactionRepository.upsertTransaction(failedTransaction)
                viewState.update {
                    it.copy(
                        isReprinting = false,
                        printError = printResult.exceptionOrNull()?.message ?: "Unable to print receipt",
                        printAttempts = attempt,
                    )
                }
            }
        }
    }

    fun onSkipPrintRequested() {
        viewState.update { it.copy(showManagerPinDialog = true, managerPinError = null) }
    }

    fun onManagerPinDismissed() {
        viewState.update { it.copy(showManagerPinDialog = false, managerPinError = null) }
    }

    fun onManagerPinSubmitted(pin: String) {
        if (!managerAuthService.validatePin(pin)) {
            viewState.update { it.copy(managerPinError = "Invalid manager PIN") }
            return
        }

        viewModelScope.launch {
            val existing = uiState.value.transaction ?: return@launch
            val skippedTransaction = existing.copy(
                status = TransactionStatus.COMPLETED,
                printStatus = PrintStatus.SKIPPED_BY_MANAGER,
            )
            transactionRepository.upsertTransaction(skippedTransaction)
            transactionRepository.updateStatus(existing.id, TransactionStatus.SYNC_PENDING)
            viewState.update {
                it.copy(
                    showManagerPinDialog = false,
                    managerPinError = null,
                    printError = null,
                    isReprinting = false,
                )
            }
        }
    }
}

@Composable
fun TransactionDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val transaction = uiState.transaction

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Transaction Detail", style = MaterialTheme.typography.headlineSmall)
        if (transaction == null) {
            Text("Transaction not found")
        } else {
            Text("Date: ${formatter.format(Instant.ofEpochMilli(transaction.createdAtEpochMs).atZone(ZoneId.systemDefault()))}")
            Text("Print status: ${transaction.printStatus}")
            Text("Line items:", fontWeight = FontWeight.SemiBold)
            transaction.lines.forEach { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${line.name} x${line.qty}")
                    Text(formatMinor(line.lineTotalMinor))
                }
            }
            Text("Subtotal: ${formatMinor(transaction.subtotalMinor)}")
            Text("Tax: ${formatMinor(transaction.taxMinor)}")
            Text("Total: ${formatMinor(transaction.totalMinor)}")
            Text("Payment method: ${uiState.payment?.method ?: "UNKNOWN"}")
            if (uiState.payment?.method == PaymentMethod.CARD && uiState.payment.providerRef != null) {
                Text("Provider reference: ${uiState.payment.providerRef}")
            }
            if (transaction.taxStatus == TaxStatus.EXEMPT_RESALE && transaction.permitSnapshot != null) {
                Text("Tax exempt permit:", fontWeight = FontWeight.SemiBold)
                Text("${transaction.permitSnapshot.businessName} (${transaction.permitSnapshot.state})")
                Text("Permit #: ${transaction.permitSnapshot.permitNumber}")
            }

            Button(onClick = viewModel::onReprintReceipt, enabled = !uiState.isReprinting) {
                Text(if (uiState.isReprinting) "Reprinting..." else "Reprint Receipt")
            }

            uiState.printError?.let { error ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "Printing failed", style = MaterialTheme.typography.titleMedium)
                        Text(text = error)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::onReprintReceipt) {
                                Text(text = "Retry print")
                            }
                            Button(onClick = viewModel::onSkipPrintRequested) {
                                Text(text = "Skip print")
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = onNavigateBack) {
            Text("Back")
        }
    }

    if (uiState.showManagerPinDialog) {
        ManagerPinDialog(
            error = uiState.managerPinError,
            onDismiss = viewModel::onManagerPinDismissed,
            onSubmit = viewModel::onManagerPinSubmitted,
        )
    }
}

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val payment: Payment? = null,
    val isReprinting: Boolean = false,
    val printError: String? = null,
    val showManagerPinDialog: Boolean = false,
    val managerPinError: String? = null,
)

private data class TransactionDetailViewState(
    val isReprinting: Boolean = false,
    val printError: String? = null,
    val showManagerPinDialog: Boolean = false,
    val managerPinError: String? = null,
    val printAttempts: Int = 0,
)
