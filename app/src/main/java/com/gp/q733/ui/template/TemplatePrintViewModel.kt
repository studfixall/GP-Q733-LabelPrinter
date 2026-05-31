package com.gp.q733.ui.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.db.ProductDao
import com.gp.q733.data.local.db.toDomain
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.domain.util.BarsoftTemplateParser
import com.gp.q733.domain.util.BarsoftFieldName
import com.gp.q733.domain.util.LabelTemplateFiller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplatePrintUiState(
    val label: Label? = null,
    val filledLabel: Label? = null,
    val productName: String = "",
    val productPrice: String = "",
    val productSpec: String = "",
    val productUnit: String = "",
    val productOrigin: String = "",
    val productBarcode: String = "",
    val productMprice: String = "",
    val fieldHints: Map<String, String> = emptyMap(), // textName -> label
    val isPrinting: Boolean = false,
    val printCopies: Int = 1,
    val printResult: String? = null, // null=未打印, "success"=成功, "error:msg"=失败
    val showProductPicker: Boolean = false,
    val productSearchResults: List<ProductInfo> = emptyList(),
    val productSearchQuery: String = ""
)

@dagger.hilt.android.lifecycle.HiltViewModel
class TemplatePrintViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val gpPrinterService: GpPrinterService,
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplatePrintUiState())
    val uiState: StateFlow<TemplatePrintUiState> = _uiState.asStateFlow()
    private var templateLabel: Label? = null
    fun setTemplate(label: Label) {
        templateLabel = label
        val hints = mutableMapOf<String, String>()
        label.elements.forEach { element ->
            when (element) {
                is LabelElement.Text -> {
                    if (element.textName.isNotBlank() && BarsoftTemplateParser.isDataBindingField(element.textName)) {
                        val hint = when (BarsoftTemplateParser.mapTextNameToField(element.textName)) {
                            BarsoftFieldName.NAME -> "品名"
                            BarsoftFieldName.PRICE -> "价格"
                            BarsoftFieldName.MPRICE -> "会员价"
                            BarsoftFieldName.SPEC -> "规格"
                            BarsoftFieldName.UNIT -> "单位"
                            BarsoftFieldName.AREA -> "产地"
                            BarsoftFieldName.BARCODE -> "条码"
                            null -> ""
                        }
                        if (hint.isNotBlank()) hints[element.textName] = hint
                    }
                }
                is LabelElement.Barcode -> {
                    if (element.textName == "barcode") hints["barcode"] = "条码"
                }
                else -> {}
            }
        }
        _uiState.value = _uiState.value.copy(label = label, fieldHints = hints)
        updatePreview()
    }
    fun updateProductName(value: String) { _uiState.value = _uiState.value.copy(productName = value); updatePreview() }
    fun updateProductPrice(value: String) { _uiState.value = _uiState.value.copy(productPrice = value); updatePreview() }
    fun updateProductMprice(value: String) { _uiState.value = _uiState.value.copy(productMprice = value); updatePreview() }
fun updateProductSpec(value: String) { _uiState.value = _uiState.value.copy(productSpec = value); updatePreview() }
    fun updateProductUnit(value: String) { _uiState.value = _uiState.value.copy(productUnit = value); updatePreview() }
    fun updateProductOrigin(value: String) { _uiState.value = _uiState.value.copy(productOrigin = value); updatePreview() }
    fun updateProductBarcode(value: String) { _uiState.value = _uiState.value.copy(productBarcode = value); updatePreview() }
    fun fillFromProduct(product: ProductInfo) {
        _uiState.value = _uiState.value.copy(
            productName = product.name,
            productPrice = String.format("%.2f", product.price),
            productMprice = String.format("%.2f", product.mprice),
    productSpec = product.spec,
            productUnit = product.unit,
            productOrigin = product.origin,
            productBarcode = product.barcode,
            showProductPicker = false
        )
        updatePreview()
    }
    fun toggleProductPicker(show: Boolean) {
        _uiState.value = _uiState.value.copy(showProductPicker = show, productSearchQuery = "")
        if (show) searchProducts("")
    }
    fun searchProducts(query: String) {
        _uiState.value = _uiState.value.copy(productSearchQuery = query)
        viewModelScope.launch {
            val results = if (query.isBlank()) {
                productDao.getAllProducts().first()
            } else {
                productDao.searchProducts("%$query%").first()
            }
            val products = results.map { it.toDomain() }
            _uiState.value = _uiState.value.copy(productSearchResults = products)
        }
    }
    fun print() {
        val label = _uiState.value.filledLabel ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrinting = true, printResult = null)
            try {
                val connectionState = bluetoothRepository.getConnectionState().first()
                if (connectionState != ConnectionState.Connected) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printResult = "error:打印机未连接"
                    )
                    return@launch
                }
                val copies = _uiState.value.printCopies
                val cmdBytes = gpPrinterService.generatePrintCommands(label, copies)
                val writeResult = bluetoothRepository.write(cmdBytes)
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = if (writeResult.isSuccess) "success" else "error:${writeResult.exceptionOrNull()?.message}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = "error:${e.message}"
                )
            }
        }
    }
    fun updatePrintCopies(copies: Int) {
        _uiState.value = _uiState.value.copy(printCopies = copies.coerceIn(1, 999))
    }
    fun clearPrintResult() {
        _uiState.value = _uiState.value.copy(printResult = null)
    }
    private fun updatePreview() {
        val template = templateLabel ?: return
        val state = _uiState.value
        val product = ProductInfo(
            barcode = state.productBarcode,
            name = state.productName,
            price = state.productPrice.toDoubleOrNull() ?: 0.0,
            mprice = state.productMprice.toDoubleOrNull() ?: 0.0,
            spec = state.productSpec,
            unit = state.productUnit,
            origin = state.productOrigin
        )
        val filled = LabelTemplateFiller.fillTemplate(template, product)
        _uiState.value = state.copy(filledLabel = filled)
    }
}
