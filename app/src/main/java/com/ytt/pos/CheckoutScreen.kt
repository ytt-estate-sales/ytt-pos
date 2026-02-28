package com.ytt.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CheckoutScreen(
    onNavigateBack: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Checkout", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Tax status: ${uiState.taxStatus}")
        Text(text = "Subtotal: ${formatMinor(uiState.subtotalMinor)}")
        Text(text = "Tax: ${formatMinor(uiState.taxMinor)}")
        Text(text = "Total: ${formatMinor(uiState.totalMinor)}")

        if (uiState.printError == null) {
            Button(
                onClick = viewModel::onCashClicked,
                enabled = !uiState.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            ) {
                Text(text = if (uiState.isProcessing) "Processing..." else "Cash")
            }

            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            ) {
                Text(text = "Card (Coming soon)")
            }
        }

        if (uiState.receiptPrinted) {
            Text(text = "Receipt printed. Transaction queued for sync.")
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
                        Button(onClick = viewModel::onRetryPrint) {
                            Text(text = "Retry print")
                        }
                        Button(onClick = viewModel::onSkipPrintRequested) {
                            Text(text = "Skip print")
                        }
                    }
                }
            }
        }

        Button(onClick = onNavigateBack) {
            Text(text = "Back to register")
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

@Composable
fun ManagerPinDialog(
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Manager override") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Enter manager PIN to skip receipt printing.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter(Char::isDigit).take(8)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    label = { Text("PIN") },
                )
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(pin) }, enabled = pin.isNotBlank()) {
                Text(text = "Authorize")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}
