package com.ytt.pos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HardwareViewModel @Inject constructor(
    private val hardwareManager: HardwareManager,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository,
) : ViewModel() {
    val uiState: StateFlow<HardwareUiState> = hardwareManager.uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HardwareUiState(),
    )

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = bluetoothDeviceRepository.discoveredDevices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun reconnectAll() {
        viewModelScope.launch(Dispatchers.IO) { hardwareManager.reconnectAll() }
    }

    fun setSelectedPrinterId(printerId: String?) {
        viewModelScope.launch(Dispatchers.IO) { hardwareManager.setSelectedPrinterId(printerId) }
    }

    fun setSelectedReaderId(readerId: String?) {
        viewModelScope.launch(Dispatchers.IO) { hardwareManager.setSelectedReaderId(readerId) }
    }

    fun setDrawerConnected(connected: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { hardwareManager.setDrawerConnected(connected) }
    }

    fun testPrint() {
        viewModelScope.launch(Dispatchers.IO) { hardwareManager.testPrint() }
    }

    fun testReader() {
        viewModelScope.launch(Dispatchers.IO) { hardwareManager.testReader() }
    }

    fun startScan() {
        bluetoothDeviceRepository.startScan()
    }

    fun stopScan() {
        bluetoothDeviceRepository.stopScan()
    }

    fun selectPrinter(device: DiscoveredDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            hardwareManager.connectPrinter(device.address)
        }
    }

    override fun onCleared() {
        bluetoothDeviceRepository.stopScan()
        super.onCleared()
    }
}

@Composable
fun HardwareScreen(
    onNavigateBack: () -> Unit,
    viewModel: HardwareViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permissionHelper = remember { PermissionHelper() }
    val uiState by viewModel.uiState.collectAsState()
    val devices by viewModel.discoveredDevices.collectAsState()
    var printerIdDraft by remember(uiState.selectedPrinterId) { mutableStateOf(uiState.selectedPrinterId.orEmpty()) }
    var readerIdDraft by remember(uiState.selectedReaderId) { mutableStateOf(uiState.selectedReaderId.orEmpty()) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            permissionMessage = null
            viewModel.startScan()
        } else {
            permissionMessage = "Bluetooth scan permissions denied"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reconnectAll()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopScan() }
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

        Button(onClick = {
            val missingPermissions = permissionHelper.missingScanPermissions(context)
            if (missingPermissions.isEmpty()) {
                permissionMessage = null
                viewModel.startScan()
            } else {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }) {
            Text("Scan Printers")
        }
        permissionMessage?.let { Text(it) }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = devices, key = { it.address }) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectPrinter(device) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = device.name)
                        Text(text = device.address)
                        Text(text = "RSSI: ${device.rssi?.toString() ?: "N/A"}")
                    }
                }
            }
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
