package com.gp.q733.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.print.PrintProtocol
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
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
    private val settingsDataStore: SettingsDataStore
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
            // For now, use mock data
            val product = lookupProduct(barcode)
            _uiState.value = _uiState.value.copy(
                productInfo = product,
                isLoading = false
            )
        }
    }

    private fun lookupProduct(barcode: String): ProductInfo {
        // Mock implementation - replace with SQL Server API
        return ProductInfo(
            barcode = barcode,
            name = "\u5546\u54c1$barcode",
            price = "0.00",
            spec = "",
            unit = "\u4e2a",
            origin = ""
        )
    }

    fun updateProductInfo(info: ProductInfo) {
        _uiState.value = _uiState.value.copy(productInfo = info)
    }

    /**
     * Print a filled label - GpPrinterService handles reconnection internally.
     * FIX: No longer depends on bluetoothRepository.getConnectedDevice() which
     * returns null after short-connection disconnect. Instead, GpPrinterService
     * uses lastConnectedMac to auto-reconnect.
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

            // Notify repository: we're about to reconnect
            val currentDevice = gpPrinterService.getCurrentDevice()
            if (currentDevice != null) {
                bluetoothRepository.notifyConnectionStateChanged(ConnectionState.Connecting, currentDevice)
            }

            // Build label from product info
            val settings = settingsDataStore.settingsFlow.first()
            val label = Label(
                id = "scan_${System.currentTimeMillis()}",
                elements = buildProductElements(info),
                widthMm = settings.labelWidthMm,
                heightMm = settings.labelHeightMm
            )

            // GpPrinterService.print() handles reconnection internally
            val result = gpPrinterService.print(label)

            // Update repository state based on result
            if (gpPrinterService.isConnected()) {
                val device = gpPrinterService.getCurrentDevice()
                bluetoothRepository.notifyConnectionStateChanged(ConnectionState.Connected, device)
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    successMessage = "\u6253\u5370\u6210\u529f"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    errorMessage = "\u6253\u5370\u5931\u8d25: ${error.message}"
                )
                // If print failed, check if we're still connected
                if (!gpPrinterService.isConnected()) {
                    bluetoothRepository.notifyDisconnected()
                }
            }
        }
    }

    private fun buildProductElements(info: ProductInfo): List<LabelElement> {
        val elements = mutableListOf<LabelElement>()
        var yOffset = 3f

        // Product name
        if (info.name.isNotBlank()) {
            elements.add(LabelElement.Text(
                x = 3f, y = yOffset,
                text = info.name,
                fontSize = 6f,
                isBold = true
            ))
            yOffset += 7f
        }

        // Price
        if (info.price.isNotBlank()) {
            elements.add(LabelElement.Text(
                x = 3f, y = yOffset,
                text = "\u00a5${info.price}/${info.unit}",
                fontSize = 5f,
                isBold = false
            ))
            yOffset += 6f
        }

        // Specification
        if (info.spec.isNotBlank()) {
            elements.add(LabelElement.Text(
                x = 3f, y = yOffset,
                text = "\u89c4\u683c: ${info.spec}",
                fontSize = 4f,
                isBold = false
            ))
            yOffset += 5f
        }

        // Origin
        if (info.origin.isNotBlank()) {
            elements.add(LabelElement.Text(
                x = 3f, y = yOffset,
                text = "\u4ea7\u5730: ${info.origin}",
                fontSize = 4f,
                isBold = false
            ))
            yOffset += 5f
        }

        // Barcode
        if (info.barcode.isNotBlank()) {
            elements.add(LabelElement.Barcode(
                x = 3f, y = yOffset,
                content = info.barcode,
                format = com.gp.q733.domain.model.BarcodeFormat.CODE128,
                height = 8f
            ))
        }

        return elements
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
}
