package com.gp.q733.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.PrinterDevice
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.print.PrintProtocol
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceUiState(
    val isScanning: Boolean = false,
    val selectedDevice: PrinterDevice? = null,
    val errorMessage: String? = null,
    val isPrinting: Boolean = false,
    val printResult: String? = null,
    val currentProtocol: PrintProtocol = PrintProtocol.CPCL,
    val isConnecting: Boolean = false
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val gpPrinterService: GpPrinterService,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = bluetoothRepository.getConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.Disconnected
        )

    val connectedDeviceFlow: StateFlow<BluetoothDevice?> = bluetoothRepository.getConnectedDeviceFlow()

    val discoveredDevices: StateFlow<List<PrinterDevice>> = bluetoothRepository.getDiscoveredDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var scanTimeoutJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            _uiState.value = _uiState.value.copy(currentProtocol = settings.printProtocol)
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                errorMessage = null
            )
            bluetoothRepository.startScan()
            scanTimeoutJob?.cancel()
            scanTimeoutJob = viewModelScope.launch {
                delay(12000L)
                bluetoothRepository.stopScan()
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    fun stopScan() {
        scanTimeoutJob?.cancel()
        viewModelScope.launch {
            bluetoothRepository.stopScan()
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
    }

    fun selectDevice(device: PrinterDevice) {
        _uiState.value = _uiState.value.copy(selectedDevice = device)
    }

    /**
     * Connect to a specific device directly.
     * FIX: Use SDK (GpPrinterService) as primary connection method.
     * After SDK connects successfully, notify repository to update UI state
     * WITHOUT creating a second socket connection.
     */
    fun connect(device: PrinterDevice) {
        viewModelScope.launch {
            stopScan()
            _uiState.value = _uiState.value.copy(
                selectedDevice = device,
                errorMessage = null,
                isConnecting = true
            )

            // Notify repository: connecting state
            bluetoothRepository.notifyConnectionStateChanged(ConnectionState.Connecting, device.device)

            // Connect via SDK
            val gpResult = gpPrinterService.connect(device.device)
            if (gpResult.isSuccess) {
                // SDK connected successfully — notify repository WITHOUT creating another socket
                bluetoothRepository.notifyConnectionStateChanged(ConnectionState.Connected, device.device)
                android.util.Log.d("PrintDebug", "Connected via GpPrinterService (SDK) — state synced to repository")
            } else {
                // SDK failed — try legacy BluetoothRepository (creates its own socket)
                android.util.Log.d("PrintDebug", "SDK connection failed: ${gpResult.exceptionOrNull()?.message}, trying legacy...")
                val legacyResult = bluetoothRepository.connect(device)
                if (legacyResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = legacyResult.exceptionOrNull()?.message ?: "Connection failed"
                    )
                } else {
                    android.util.Log.d("PrintDebug", "Connected via legacy BluetoothRepository")
                }
            }

            _uiState.value = _uiState.value.copy(isConnecting = false)
        }
    }

    /**
     * Legacy connect() using selectedDevice from UI state.
     */
    fun connect() {
        val device = _uiState.value.selectedDevice ?: return
        connect(device)
    }

    /**
     * Disconnect from printer.
     * FIX: Keep selectedDevice so printTestPage() can still get device name.
     * Only clear the connection state.
     */
    fun disconnect() {
        viewModelScope.launch {
            gpPrinterService.disconnect()
            bluetoothRepository.notifyDisconnected()
            // NOTE: Do NOT clear selectedDevice — needed for test print device name
            _uiState.value = _uiState.value.copy(
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Print test page - GpPrinterService handles reconnection internally.
     * FIX: Use selectedDevice.name (preserved across disconnect) as fallback for device name.
     */
    fun printTestPage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPrinting = true,
                printResult = null
            )

            // Get device name from selectedDevice (preserved across disconnect)
            val deviceName = _uiState.value.selectedDevice?.name ?: "Unknown"

            // Update repository state to Connecting during reconnection
            val btDevice = gpPrinterService.getCurrentDevice()
            if (btDevice != null && !gpPrinterService.isConnected()) {
                bluetoothRepository.notifyConnectionStateChanged(ConnectionState.Connecting, btDevice)
            }

            // GpPrinterService.printTestPage() auto-reconnects if needed
            val result = gpPrinterService.printTestPage(deviceName)

            // Update repository state based on result
            if (gpPrinterService.isConnected()) {
                val currentDevice = gpPrinterService.getCurrentDevice()
                bluetoothRepository.notifyConnectionStateChanged(ConnectionState.Connected, currentDevice)
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = "\u6d4b\u8bd5\u9875\u6253\u5370\u6210\u529f"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = "\u6253\u5370\u5931\u8d25: ${error.message}"
                )
                // If print failed, check if we're still connected
                if (!gpPrinterService.isConnected()) {
                    bluetoothRepository.notifyDisconnected()
                }
            }
        }
    }

    fun clearPrintResult() {
        _uiState.value = _uiState.value.copy(printResult = null)
    }

    override fun onCleared() {
        super.onCleared()
        scanTimeoutJob?.cancel()
        bluetoothRepository.stopScan()
        gpPrinterService.disconnect()
    }
}
