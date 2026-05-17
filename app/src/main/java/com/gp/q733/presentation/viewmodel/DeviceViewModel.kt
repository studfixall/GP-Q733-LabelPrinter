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
    val currentProtocol: PrintProtocol = PrintProtocol.CPCL
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
     * FIX: Accept device as parameter instead of reading from _uiState.value.selectedDevice
     * to avoid StateFlow race condition (first click reads null, second click reads value).
     */
    fun connect(device: PrinterDevice) {
        viewModelScope.launch {
            stopScan()
            _uiState.value = _uiState.value.copy(
                selectedDevice = device,
                errorMessage = null
            )
            val btDevice = device.device

            // Try SDK connection first
            val gpResult = gpPrinterService.connect(btDevice)
            if (gpResult.isSuccess) {
                // Also update BluetoothRepository state for UI consistency
                bluetoothRepository.connect(device)
                android.util.Log.d("PrintDebug", "Connected via GpPrinterService (SDK)")
            } else {
                // Fallback to legacy BluetoothRepository
                val legacyResult = bluetoothRepository.connect(device)
                if (legacyResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = legacyResult.exceptionOrNull()?.message ?: "Connection failed"
                    )
                } else {
                    android.util.Log.d("PrintDebug", "Connected via legacy BluetoothRepository")
                }
            }
        }
    }

    /**
     * Legacy connect() that uses selectedDevice from UI state.
     * Kept for backward compatibility but prefer connect(device) overload.
     */
    fun connect() {
        val device = _uiState.value.selectedDevice ?: return
        connect(device)
    }

    fun disconnect() {
        viewModelScope.launch {
            gpPrinterService.disconnect()
            bluetoothRepository.disconnect()
            _uiState.value = _uiState.value.copy(
                selectedDevice = null,
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun printTestPage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPrinting = true,
                printResult = null
            )

            // FIX: Get device from gpPrinterService first (SDK path),
            // then fallback to BluetoothRepository (legacy path)
            val btDevice = gpPrinterService.getCurrentDevice()
                ?: bluetoothRepository.getConnectedDevice()

            val deviceName = _uiState.value.selectedDevice?.name ?: "Unknown"

            val result = if (btDevice != null) {
                android.util.Log.d("PrintDebug", "Connecting GpPrinterService for test print")
                gpPrinterService.disconnect()
                val connectResult = gpPrinterService.connect(btDevice)
                if (connectResult.isSuccess) {
                    val printResult = gpPrinterService.printTestPage(deviceName)
                    gpPrinterService.disconnect()
                    printResult
                } else {
                    Result.failure(connectResult.exceptionOrNull() ?: Exception("Connection failed"))
                }
            } else {
                Result.failure(Exception("No Bluetooth device connected"))
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = "测试页打印成功"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = "打印失败: ${error.message}"
                )
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
