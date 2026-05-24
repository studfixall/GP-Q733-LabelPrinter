package com.gp.q733.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.PrinterDevice
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanProductUiState(
    val productInfo: ProductInfo = ProductInfo(),
    val isLoading: Boolean = false,
    val isPrinting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastScannedCode: String? = null
)

@HiltViewModel
class ScanProductViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val gpPrinterService: GpPrinterService,
    private val settingsDataStore: SettingsDataStore,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanProductUiState())
    val uiState: StateFlow<ScanProductUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = bluetoothRepository.getConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.Disconnected
        )

    fun onBarcodeScanned(barcode: String) {
        _uiState.value = _uiState.value.copy(
            lastScannedCode = barcode,
            isLoading = true,
            errorMessage = null
        )
        viewModelScope.launch {
            // TODO: Replace with actual API call to SQL Server
            val product = lookupProduct(barcode)
            _uiState.value = _uiState.value.copy(
                productInfo = product,
                isLoading = false
            )
        }
    }

    private suspend fun lookupProduct(barcode: String): ProductInfo {
        // First query local database
        val dbProduct = productRepository.getProductByBarcode(barcode)
        if (dbProduct != null) {
            return dbProduct
        }
        // Not found in database — return placeholder, user can fill in and save
        return ProductInfo(
            barcode = barcode,
            name = "",
            price = 0.0,
            spec = "",
            unit = "",
            origin = ""
        )
    }

    fun updateProductInfo(info: ProductInfo) {
        _uiState.value = _uiState.value.copy(productInfo = info)
    }

    /**
     * Print a filled label - generate commands, send via BluetoothRepository socket
     * FIX: No longer use SDK RTPrinter connection (unstable, disconnects after 2-3s)
     */
    fun printFilledLabel() {
        viewModelScope.launch {
            val info = _uiState.value.productInfo
            if (info.barcode.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "\u8bf7\u5148\u626b\u7801\u83b7\u53d6\u5546\u54c1\u4fe1\u606f"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isPrinting = true, errorMessage = null)

            // Check connection state
            val connectionState = bluetoothRepository.getConnectionState().first()
            if (connectionState != ConnectionState.Connected) {
                val mac = gpPrinterService.getLastConnectedMac()
                if (mac != null) {
                    val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
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
                                errorMessage = "\u91cd\u8fde\u5931\u8d25: ${connectResult.exceptionOrNull()?.message}"
                            )
                            return@launch
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isPrinting = false,
                            errorMessage = "\u6253\u5370\u673a\u672a\u8fde\u63a5"
                        )
                        return@launch
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        errorMessage = "\u6253\u5370\u673a\u672a\u8fde\u63a5"
                    )
                    return@launch
                }
            }

            // Build label from product info
            val settings = settingsDataStore.settingsFlow.first()
            val label = Label(
                id = "scan_${System.currentTimeMillis()}",
                elements = buildProductElements(info),
                widthMm = settings.labelWidth,
                heightMm = settings.labelHeight
            )

            // Generate commands via SDK, send via socket
            android.util.Log.d("PrintDebug", "ScanProduct print - generate commands, send via socket")
            val cmdBytes = gpPrinterService.generatePrintCommands(label)
            val writeResult = bluetoothRepository.write(cmdBytes)
            writeResult.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    successMessage = "\u6253\u5370\u6210\u529f"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    errorMessage = "\u6253\u5370\u5931\u8d25: ${error.message}"
                )
                bluetoothRepository.disconnect()
            }
        }
    }

    private fun buildProductElements(info: ProductInfo): List<LabelElement> {
        val elements = mutableListOf<LabelElement>()
        var yOffset = 3f

        if (info.name.isNotBlank()) {
            elements.add(LabelElement.Text(x = 3f, y = yOffset, text = info.name, fontSize = 6f, isBold = true))
            yOffset += 7f
        }
        if (info.price > 0) {
            elements.add(LabelElement.Text(x = 3f, y = yOffset, text = "\u00a5${String.format("%.2f", info.price)}/${info.unit}", fontSize = 5f, isBold = false))
            yOffset += 6f
        }
        if (info.spec.isNotBlank()) {
            elements.add(LabelElement.Text(x = 3f, y = yOffset, text = "\u89c4\u683c: ${info.spec}", fontSize = 4f, isBold = false))
            yOffset += 5f
        }
        if (info.origin.isNotBlank()) {
            elements.add(LabelElement.Text(x = 3f, y = yOffset, text = "\u4ea7\u5730: ${info.origin}", fontSize = 4f, isBold = false))
            yOffset += 5f
        }
        if (info.barcode.isNotBlank()) {
            elements.add(LabelElement.Barcode(x = 3f, y = yOffset, content = info.barcode, format = com.gp.q733.domain.model.BarcodeFormat.CODE128, height = 8f))
        }
        return elements
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
}
