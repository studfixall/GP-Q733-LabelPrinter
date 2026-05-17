package com.gp.q733.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.LabelDataStore
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.BarcodeFormat
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.model.TemplateFields
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.domain.repository.ProductRepository
import com.gp.q733.domain.util.LabelTemplateFiller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanProductUiState(
    val barcode: String = "",
    val product: ProductInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val filledLabel: Label? = null,
    val showFillPreview: Boolean = false,
    val printStatus: PrintStatus = PrintStatus.Idle
)

enum class PrintStatus { Idle, Printing, Success, Failed }

@HiltViewModel
class ScanProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val labelDataStore: LabelDataStore,
    private val settingsDataStore: SettingsDataStore,
    private val bluetoothRepository: BluetoothRepository,
    private val gpPrinterService: com.gp.q733.domain.print.GpPrinterService
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
            barcode = barcode,
            isLoading = true,
            error = null,
            product = null,
            filledLabel = null
        )
        queryProduct(barcode)
    }

    private fun queryProduct(barcode: String) {
        viewModelScope.launch {
            try {
                val product = productRepository.getProductByBarcode(barcode)
                if (product != null) {
                    _uiState.value = _uiState.value.copy(
                        product = product,
                        isLoading = false,
                        showFillPreview = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "\u672a\u627e\u5230\u5546\u54c1\u4fe1\u606f\uff0c\u8bf7\u68c0\u67e5\u6761\u7801\u662f\u5426\u6b63\u786e"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "\u67e5\u8be2\u5931\u8d25: ${e.message}"
                )
            }
        }
    }

    fun fillTemplateAndPreview(template: LabelTemplate) {
        val product = _uiState.value.product ?: return
        val filledLabel = LabelTemplateFiller.fillTemplate(template.label, product)
        _uiState.value = _uiState.value.copy(
            filledLabel = filledLabel,
            showFillPreview = true
        )
    }

    /**
     * Print filled label - GpPrinterService handles reconnection internally
     * FIX: No longer checks isConnected() + getConnectedDevice() separately
     * GpPrinterService.print() now auto-reconnects using lastConnectedMac
     */
    fun printFilledLabel() {
        val filledLabel = _uiState.value.filledLabel ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(printStatus = PrintStatus.Printing)
            try {
                labelDataStore.saveLabel(filledLabel)

                // GpPrinterService.print() handles reconnection internally
                val result = gpPrinterService.print(filledLabel)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(printStatus = PrintStatus.Success)
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        printStatus = PrintStatus.Failed,
                        error = "\u6253\u5370\u5931\u8d25: ${error.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    printStatus = PrintStatus.Failed,
                    error = "\u6253\u5370\u5f02\u5e38: ${e.message}"
                )
            }
        }
    }

    fun reset() {
        _uiState.value = ScanProductUiState()
    }

    fun onManualBarcodeEntered(barcode: String) {
        onBarcodeScanned(barcode)
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun getProductTemplates(): List<LabelTemplate> {
        return listOf(
            LabelTemplate(
                id = "product_scan",
                name = "\u5546\u54c1\u6807\u7b7e",
                description = "\u626b\u7801\u5546\u54c1\u6807\u7b7e\u6a21\u677f",
                label = Label(
                    id = "template_product_scan",
                    widthMm = 50f,
                    heightMm = 30f,
                    elements = listOf(
                        LabelElement.Text(x = 2f, y = 2f, text = TemplateFields.PRODUCT_NAME, fontSize = 10f, isBold = true),
                        LabelElement.Text(x = 2f, y = 10f, text = "\u4ef7\u683c: ${TemplateFields.PRICE}", fontSize = 8f, isBold = false),
                        LabelElement.Barcode(x = 2f, y = 18f, content = TemplateFields.BARCODE, format = BarcodeFormat.CODE128, height = 8f)
                    )
                )
            ),
            LabelTemplate(
                id = "price_scan",
                name = "\u4ef7\u683c\u6807\u7b7e",
                description = "\u626b\u7801\u4ef7\u683c\u6807\u7b7e\u6a21\u677f",
                label = Label(
                    id = "template_price_scan",
                    widthMm = 40f,
                    heightMm = 20f,
                    elements = listOf(
                        LabelElement.Text(x = 2f, y = 2f, text = TemplateFields.PRODUCT_NAME, fontSize = 8f, isBold = true),
                        LabelElement.Text(x = 2f, y = 10f, text = TemplateFields.PRICE, fontSize = 12f, isBold = true),
                        LabelElement.Barcode(x = 2f, y = 16f, content = TemplateFields.BARCODE, format = BarcodeFormat.EAN13, height = 3f)
                    )
                )
            )
        )
    }
}
