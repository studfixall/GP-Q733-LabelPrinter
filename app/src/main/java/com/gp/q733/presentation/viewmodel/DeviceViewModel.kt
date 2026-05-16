package com.gp.q733.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.PrinterDevice
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.print.PrintProtocol
import com.gp.q733.domain.print.PrintService
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
    private val printService: PrintService,
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
    private var connectedDevice: BluetoothDevice? = null

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

    fun connect() {
        val device = _uiState.value.selectedDevice ?: return

        viewModelScope.launch {
            stopScan()
            _uiState.value = _uiState.value.copy(errorMessage = null)

            val btDevice = device.device
            connectedDevice = btDevice

            // Try GpPrinterService first (official SDK)
            val gpResult = gpPrinterService.connect(btDevice)
            if (gpResult.isSuccess) {
                // SDK connected successfully — also update repository state for UI
                bluetoothRepository.connect(device)
                android.util.Log.d("PrintDebug", "Connected via GpPrinterService (SDK)")
            } else {
                // SDK failed, try legacy connection
                val legacyResult = bluetoothRepository.connect(device)
                if (legacyResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = legacyResult.exceptionOrNull()?.message ?: "Connection failed"
                    )
                    connectedDevice = null
                } else {
                    android.util.Log.d("PrintDebug", "Connected via legacy BluetoothRepository")
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            gpPrinterService.disconnect()
            bluetoothRepository.disconnect()
            connectedDevice = null
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
        val device = _uiState.value.selectedDevice ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPrinting = true,
                printResult = null
            )

            val settings = settingsDataStore.settingsFlow.first()
            android.util.Log.d("PrintDebug", "Current protocol from settings: ${settings.printProtocol}")
            android.util.Log.d("PrintDebug", "Density: ${settings.printDensity}, Speed: ${settings.printSpeed}, Gap: ${settings.gapMm}")

            // Check if GpPrinterService is connected first
            val gpConnected = gpPrinterService.isConnected()
            android.util.Log.d("PrintDebug", "GP connected: $gpConnected")

            val result = if (gpConnected) {
                // GpPrinterService is already connected, use it directly
                android.util.Log.d("PrintDebug", "Using GpPrinterService (already connected)")
                gpPrinterService.printTestPage(device.name)
            } else {
                // Try to get device and connect
                val btDevice = connectedDevice ?: bluetoothRepository.getConnectedDevice()
                android.util.Log.d("PrintDebug", "BT Device: $btDevice, local connectedDevice: $connectedDevice")

                if (btDevice != null) {
                    android.util.Log.d("PrintDebug", "Connecting GpPrinterService then printing")
                    val connectResult = gpPrinterService.connect(btDevice)
                    if (connectResult.isSuccess) {
                        gpPrinterService.printTestPage(device.name)
                    } else {
                        Result.failure(connectResult.exceptionOrNull() ?: Exception("Connection failed"))
                    }
                } else {
                    android.util.Log.d("PrintDebug", "No device available, using legacy PrintService")
                    printService.printTestPage(device.name)
                }
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
