package com.ytt.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RegisterScreen(
    onNavigateToHardware: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Register")

        uiState.customerName?.let {
            Text(text = "Customer: $it")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.lines, key = { it.sku }) { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "${line.name} x${line.qty}")
                    Text(text = formatMinor(line.lineTotalMinor))
                }
            }
        }

        Divider()
        Text(text = "Subtotal: ${formatMinor(uiState.subtotalMinor)}")
        Text(text = "Tax: ${formatMinor(uiState.taxMinor)} (${uiState.taxStatus})")
        Text(text = "Total: ${formatMinor(uiState.totalMinor)}")
        uiState.message?.let { Text(text = it) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.onEvent(CartEvent.AddTestItem) }) {
                Text(text = "Add test item")
            }
            Button(onClick = { viewModel.onEvent(CartEvent.ToggleTaxExemption) }) {
                Text(text = "Toggle tax exemption")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onNavigateToHardware) {
                Text(text = "Configure Hardware")
            }
            Button(onClick = onNavigateToCheckout) {
                Text(text = "Checkout")
            }
        }
    }
}
