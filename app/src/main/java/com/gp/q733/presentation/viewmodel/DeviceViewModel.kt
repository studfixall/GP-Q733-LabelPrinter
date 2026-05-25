package com.gp.q733.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.PrinterDevice
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.print.PrintProtocol
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val ctx: Context
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
     * 连接打印机 - 统一走 BluetoothRepository socket 直连
     * 不再使用 SDK 的 RTPrinter 连接（日志证实 SDK 连接 2-3 秒后自动断开）
     */
    fun connect(device: PrinterDevice) {
        viewModelScope.launch {
            stopScan()
            _uiState.value = _uiState.value.copy(
                selectedDevice = device,
                errorMessage = null,
                isConnecting = true
            )
            // 保存 MAC 用于重连
            gpPrinterService.setLastConnectedMac(device.address)

            val result = bluetoothRepository.connect(device)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "\u8fde\u63a5\u5931\u8d25"
                )
            } else {
                android.util.Log.d("PrintDebug", "Connected via BluetoothRepository (socket)")
            }
            _uiState.value = _uiState.value.copy(isConnecting = false)
        }
    }

    fun connect() {
        val device = _uiState.value.selectedDevice ?: return
        connect(device)
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothRepository.disconnect()
            _uiState.value = _uiState.value.copy(
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 打印测试页 - 生成命令 + 通过 socket 发送
     * 架构：GpPrinterService 只生成命令，BluetoothRepository.write() 发送
     */
    fun printTestPage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPrinting = true,
                printResult = null
            )

            val deviceName = _uiState.value.selectedDevice?.name ?: "Unknown"

            // 检查是否已连接
            val connectionState = bluetoothRepository.getConnectionState().first()
            if (connectionState != ConnectionState.Connected) {
                // 尝试重连
                val mac = gpPrinterService.getLastConnectedMac()
                if (mac != null) {
                    val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                    val btDevice = adapter?.getRemoteDevice(mac)
                    if (btDevice != null) {
                        val printerDevice = PrinterDevice(
                            device = btDevice,
                            name = btDevice.name ?: "Unknown",
                            address = mac
                        )
                        val connectResult = bluetoothRepository.connect(printerDevice)
                        if (connectResult.isFailure) {
                            _uiState.value = _uiState.value.copy(
                                isPrinting = false,
                                printResult = "\u6253\u5370\u5931\u8d25: \u91cd\u8fde\u5931\u8d25 - ${connectResult.exceptionOrNull()?.message}"
                            )
                            return@launch
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printResult = "\u6253\u5370\u5931\u8d25: \u6253\u5370\u673a\u672a\u8fde\u63a5"
                    )
                    return@launch
                }
            }

            try {
                val cmdBytes = gpPrinterService.generateTestPageCommands(deviceName)
                val writeResult = bluetoothRepository.write(cmdBytes)
                writeResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printResult = "\u6d4b\u8bd5\u9875\u6253\u5370\u6210\u529f"
                    )
                    android.util.Log.d("PrintDebug", "Test page sent via socket, ${cmdBytes.size} bytes")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printResult = "\u6253\u5370\u5931\u8d25: ${error.message}"
                    )
                    bluetoothRepository.disconnect()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = "\u6253\u5370\u9519\u8bef: ${e.message}"
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
    }
}
