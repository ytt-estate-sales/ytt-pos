package com.ytt.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HardwareViewModel @Inject constructor(
    private val hardwareManager: HardwareManager,
) : ViewModel() {
    val uiState: StateFlow<HardwareUiState> = hardwareManager.uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HardwareUiState(),
    )

    fun reconnectAll() {
        viewModelScope.launch { hardwareManager.reconnectAll() }
    }

    fun setSelectedPrinterId(printerId: String?) {
        viewModelScope.launch { hardwareManager.setSelectedPrinterId(printerId) }
    }

    fun setSelectedReaderId(readerId: String?) {
        viewModelScope.launch { hardwareManager.setSelectedReaderId(readerId) }
    }

    fun setDrawerConnected(connected: Boolean) {
        viewModelScope.launch { hardwareManager.setDrawerConnected(connected) }
    }

    fun testPrint() {
        viewModelScope.launch { hardwareManager.testPrint() }
    }

    fun testReader() {
        viewModelScope.launch { hardwareManager.testReader() }
    }
}

@Composable
fun HardwareScreen(
    onNavigateBack: () -> Unit,
    viewModel: HardwareViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var printerIdDraft by remember(uiState.selectedPrinterId) { mutableStateOf(uiState.selectedPrinterId.orEmpty()) }
    var readerIdDraft by remember(uiState.selectedReaderId) { mutableStateOf(uiState.selectedReaderId.orEmpty()) }

    LaunchedEffect(Unit) {
        viewModel.reconnectAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Hardware")
        Text(text = "Selected Printer: ${uiState.selectedPrinterId ?: "None"}")
        Text(text = "Selected Reader: ${uiState.selectedReaderId ?: "None"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("Printer: ${uiState.printerStatus.name}") })
            AssistChip(onClick = {}, label = { Text("Reader: ${uiState.readerStatus.name}") })
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Drawer connected")
            Switch(checked = uiState.drawerConnected, onCheckedChange = viewModel::setDrawerConnected)
        }

        OutlinedTextField(
            value = printerIdDraft,
            onValueChange = { printerIdDraft = it },
            label = { Text("Printer ID") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            viewModel.setSelectedPrinterId(printerIdDraft.ifBlank { null })
        }) {
            Text("Save Printer")
        }

        OutlinedTextField(
            value = readerIdDraft,
            onValueChange = { readerIdDraft = it },
            label = { Text("Reader ID") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            viewModel.setSelectedReaderId(readerIdDraft.ifBlank { null })
        }) {
            Text("Save Reader")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::reconnectAll) { Text("Reconnect All") }
            Button(onClick = viewModel::testPrint) { Text("Test Print") }
            Button(onClick = viewModel::testReader) { Text("Test Reader") }
        }

        Button(onClick = onNavigateBack) {
            Text(text = "Back to Register")
        }
    }
}
