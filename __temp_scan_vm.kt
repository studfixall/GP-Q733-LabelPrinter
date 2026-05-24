package com.gp.q733.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.LabelDataStore
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.BarcodeFormat
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.model.TemplateFields
import com.gp.q733.domain.print.GpPrinterService
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
    val selectedTemplate: LabelTemplate? = null,
    val showFillPreview: Boolean = false,
    val printStatus: PrintStatus = PrintStatus.Idle,
    val printMessage: String? = null
)

enum class PrintStatus { Idle, Printing, Success, Failed }

data class LabelTemplate(
    val id: String,
    val name: String,
    val description: String,
    val label: Label
)

@HiltViewModel
class ScanProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val labelDataStore: LabelDataStore,
    private val settingsDataStore: SettingsDataStore,
    private val bluetoothRepository: BluetoothRepository,
    private val gpPrinterService: GpPrinterService
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
     * 最近连接的打印机MAC，用于扫码前自动重连
     */
    var lastConnectedMac: String? = null

    /**
     * 处理扫码结果（来自扫码枪或手动输入）
     */
    fun onBarcodeScanned(barcode: String) {
        _uiState.value = _uiState.value.copy(
            barcode = barcode,
            isLoading = true,
            error = null,
            product = null,
            filledLabel = null,
            selectedTemplate = null,
            printStatus = PrintStatus.Idle,
            printMessage = null
        )
        queryProduct(barcode)
    }

    /**
     * 查询商品信息（Room本地数据库）
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
                        error = "未找到条码 $barcode 对应的商品，请先在商品管理中录入"
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
     * 选择模板并填充商品数据
     */
    fun fillTemplateAndPreview(template: LabelTemplate) {
        val product = _uiState.value.product ?: return
        val filledLabel = LabelTemplateFiller.fillTemplate(template.label, product)
        _uiState.value = _uiState.value.copy(
            filledLabel = filledLabel,
            selectedTemplate = template,
            showFillPreview = true
        )
    }

    /**
     * 打印填充后的标签 — 联调核心
     * 流程: 检查连接 → (自动重连) → 调GpPrinterService.print()
     */
    fun printFilledLabel() {
        val filledLabel = _uiState.value.filledLabel ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                printStatus = PrintStatus.Printing,
                printMessage = "正在打印..."
            )

            try {
                // 检查连接状态，未连接则尝试自动重连
                if (!gpPrinterService.isConnected()) {
                    val mac = lastConnectedMac ?: GpPrinterService.lastConnectedMac
                    if (mac != null) {
                        val device = bluetoothRepository.getRemoteDevice(mac)
                        if (device != null) {
                            val connectResult = gpPrinterService.connect(device)
                            if (connectResult.isFailure) {
                                _uiState.value = _uiState.value.copy(
                                    printStatus = PrintStatus.Failed,
                                    printMessage = "自动重连失败: ${connectResult.exceptionOrNull()?.message}"
                                )
                                return@launch
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(
                                printStatus = PrintStatus.Failed,
                                printMessage = "未找到打印机设备，请先在设备页连接"
                            )
                            return@launch
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            printStatus = PrintStatus.Failed,
                            printMessage = "未连接打印机，请先在设备页连接"
                        )
                        return@launch
                    }
                }

                // 更新设置中的标签尺寸（GpPrinterService从settings读尺寸）
                settingsDataStore.updateLabelSize(filledLabel.widthMm, filledLabel.heightMm)

                // 调用SDK打印
                val result = gpPrinterService.print(filledLabel)
                if (result.isSuccess) {
                    // 保存打印记录
                    labelDataStore.saveLabel(filledLabel)
                    _uiState.value = _uiState.value.copy(
                        printStatus = PrintStatus.Success,
                        printMessage = "打印成功 ✅"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        printStatus = PrintStatus.Failed,
                        printMessage = "打印失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    printStatus = PrintStatus.Failed,
                    printMessage = "打印异常: ${e.message}"
                )
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
     * 获取可用的商品标签模板
     */
    fun getProductTemplates(): List<LabelTemplate> {
        return listOf(
            LabelTemplate(
                id = "product_scan_5030",
                name = "商品标签 50x30",
                description = "名称+规格+价格+条码",
                label = Label(
                    id = "tpl_product_5030",
                    widthMm = 50f,
                    heightMm = 30f,
                    elements = listOf(
                        LabelElement.Text(x = 2f, y = 1f, text = TemplateFields.PRODUCT_NAME, fontSize = 10f, isBold = true),
                        LabelElement.Text(x = 2f, y = 6f, text = TemplateFields.SPEC, fontSize = 7f),
                        LabelElement.Text(x = 2f, y = 11f, text = "¥${TemplateFields.PRICE}", fontSize = 10f, isBold = true),
                        LabelElement.Barcode(x = 2f, y = 17f, content = TemplateFields.BARCODE, format = BarcodeFormat.CODE128, height = 8f, widthMm = 46f)
                    )
                )
            ),
            LabelTemplate(
                id = "price_scan_4030",
                name = "价格标签 40x30",
                description = "名称+价格（大字）+条码",
                label = Label(
                    id = "tpl_price_4030",
                    widthMm = 40f,
                    heightMm = 30f,
                    elements = listOf(
                        LabelElement.Text(x = 2f, y = 1f, text = TemplateFields.PRODUCT_NAME, fontSize = 8f, isBold = true),
                        LabelElement.Text(x = 2f, y = 7f, text = TemplateFields.PRICE, fontSize = 14f, isBold = true),
                        LabelElement.Barcode(x = 2f, y = 17f, content = TemplateFields.BARCODE, format = BarcodeFormat.CODE128, height = 8f, widthMm = 36f)
                    )
                )
            ),
            LabelTemplate(
                id = "product_scan_4020",
                name = "紧凑标签 40x20",
                description = "名称+价格+条码（小标签）",
                label = Label(
                    id = "tpl_product_4020",
                    widthMm = 40f,
                    heightMm = 20f,
                    elements = listOf(
                        LabelElement.Text(x = 2f, y = 1f, text = TemplateFields.PRODUCT_NAME, fontSize = 7f, isBold = true),
                        LabelElement.Text(x = 2f, y = 6f, text = "¥${TemplateFields.PRICE}", fontSize = 8f, isBold = true),
                        LabelElement.Barcode(x = 2f, y = 11f, content = TemplateFields.BARCODE, format = BarcodeFormat.CODE128, height = 5f, widthMm = 36f)
                    )
                )
            )
        )
    }

    companion object {
        /**
         * GpPrinterService记录的最后一次连接MAC
         * 供外部设置（如DeviceViewModel连接成功后）
         */
        var lastConnectedMac: String? = null
            private set
    }
}
