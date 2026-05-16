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

/**
 * 扫码商品打印 ViewModel
 * 处理扫码 → 查询商品 → 填充模板 → 打印的流程
 */
data class ScanProductUiState(
    val barcode: String = "",
    val product: ProductInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val filledLabel: Label? = null,
    val showFillPreview: Boolean = false,
    val printStatus: PrintStatus = PrintStatus.Idle
)

enum class PrintStatus {
    Idle,
    Printing,
    Success,
    Failed
}

@HiltViewModel
class ScanProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val labelDataStore: LabelDataStore,
    private val settingsDataStore: SettingsDataStore,
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanProductUiState())
    val uiState: StateFlow<ScanProductUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = bluetoothRepository.getConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.Disconnected
        )

    /**
     * 处理扫码结果
     */
    fun onBarcodeScanned(barcode: String) {
        _uiState.value = _uiState.value.copy(
            barcode = barcode,
            isLoading = true,
            error = null,
            product = null,
            filledLabel = null
        )
        
        // 查询商品信息
        queryProduct(barcode)
    }

    /**
     * 查询商品信息
     */
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
                        error = "未找到商品信息，请检查条码是否正确"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "查询失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 填充模板并预览
     */
    fun fillTemplateAndPreview(template: LabelTemplate) {
        val product = _uiState.value.product ?: return
        
        // 使用模板填充器填充标签
        val filledLabel = LabelTemplateFiller.fillTemplate(template.label, product)
        
        _uiState.value = _uiState.value.copy(
            filledLabel = filledLabel,
            showFillPreview = true
        )
    }

    /**
     * 打印填充后的标签
     */
    fun printFilledLabel() {
        val filledLabel = _uiState.value.filledLabel ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(printStatus = PrintStatus.Printing)
            
            try {
                // 保存打印记录
                labelDataStore.saveLabel(filledLabel)
                
                // 通过蓝牙发送打印
                // TODO: 调用打印服务
                // printService.print(filledLabel)
                
                _uiState.value = _uiState.value.copy(printStatus = PrintStatus.Success)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(printStatus = PrintStatus.Failed)
            }
        }
    }

    /**
     * 重置状态，准备下一次扫码
     */
    fun reset() {
        _uiState.value = ScanProductUiState()
    }

    /**
     * 手动输入条码
     */
    fun onManualBarcodeEntered(barcode: String) {
        onBarcodeScanned(barcode)
    }

    /**
     * 获取可用的商品模板
     */
    fun getProductTemplates(): List<LabelTemplate> {
        return listOf(
            LabelTemplate(
                id = "product_scan",
                name = "商品标签",
                description = "扫码商品标签模板",
                label = Label(
                    id = "template_product_scan",
                    widthMm = 50f,
                    heightMm = 30f,
                    elements = listOf(
                        LabelElement.Text(x = 2f, y = 2f, text = TemplateFields.PRODUCT_NAME, fontSize = 10f, isBold = true),
                        LabelElement.Text(x = 2f, y = 10f, text = "价格: ${TemplateFields.PRICE}", fontSize = 8f, isBold = false),
                        LabelElement.Barcode(x = 2f, y = 18f, content = TemplateFields.BARCODE, format = BarcodeFormat.CODE128, height = 8f)
                    )
                )
            ),
            LabelTemplate(
                id = "price_scan",
                name = "价格标签",
                description = "扫码价格标签模板",
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
