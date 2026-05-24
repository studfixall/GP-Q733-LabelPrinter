package com.gp.q733.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.data.local.db.ProductDao
import com.gp.q733.data.local.db.toDomain
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.PrinterDevice
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.domain.util.BarsoftTemplateParser
import com.gp.q733.domain.util.LabelTemplateFiller
import com.gp.q733.domain.util.TemplateJsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 扫码打印页面的模板选项
 */
data class ScanTemplateOption(
    val id: String,
    val name: String,
    val label: Label
)

data class ScanProductUiState(
    // 扫码结果
    val productInfo: ProductInfo = ProductInfo(),
    val productExistsInDb: Boolean = false,
    val isLoading: Boolean = false,

    // 未找到商品提示
    val showNotFoundDialog: Boolean = false,

    // 维护商品资料弹窗
    val showProductDialog: Boolean = false,
    val dialogName: String = "",
    val dialogPrice: String = "",

    // 模板选择
    val templates: List<ScanTemplateOption> = emptyList(),
    val selectedTemplateId: String = "",

    // 打印状态
    val isPrinting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastScannedCode: String? = null
)

@HiltViewModel
class ScanProductViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothRepository: BluetoothRepository,
    private val gpPrinterService: GpPrinterService,
    private val settingsDataStore: SettingsDataStore,
    private val productDao: ProductDao,
    private val customTemplateDao: com.gp.q733.data.local.db.CustomTemplateDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanProductUiState())
    val uiState: StateFlow<ScanProductUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    /**
     * 加载模板列表：内置模板 + 自定义模板（Room）+ Barsoft XML
     */
    private fun loadTemplates() {
        viewModelScope.launch {
            val templates = mutableListOf<ScanTemplateOption>()

            // 内置模板（硬编码）
            templates.add(ScanTemplateOption(
                id = "default_50x30",
                name = "商品标签 50×30",
                label = buildDefaultLabel(50f, 30f)
            ))
            templates.add(ScanTemplateOption(
                id = "default_40x30",
                name = "价格标签 40×30",
                label = buildDefaultLabel(40f, 30f)
            ))
            templates.add(ScanTemplateOption(
                id = "default_40x20",
                name = "紧凑标签 40×20",
                label = buildDefaultLabel(40f, 20f)
            ))

            // 从 Room 加载自定义模板
            try {
                val customs = customTemplateDao.getAll().first()
                for (entity in customs) {
                    val elements = TemplateJsonParser.fromJson(entity.elementsJson)
                    templates.add(ScanTemplateOption(
                        id = "custom_${entity.id}",
                        name = "★ ${entity.name} (${entity.widthMm.toInt()}×${entity.heightMm.toInt()})",
                        label = Label(
                            id = "custom_${entity.id}",
                            elements = elements,
                            widthMm = entity.widthMm,
                            heightMm = entity.heightMm
                        )
                    ))
                }
            } catch (_: Exception) {}

            // 从assets加载Barsoft XML模板
            try {
                val topDirs = context.assets.list("templates") ?: emptyArray()
                for (topDir in topDirs) {
                    val categories = context.assets.list("templates/$topDir") ?: emptyArray()
                    for (category in categories) {
                        val files = context.assets.list("templates/$topDir/$category") ?: emptyArray()
                        for (file in files) {
                            if (file.endsWith(".xml")) {
                                try {
                                    val inputStream = context.assets.open("templates/$topDir/$category/$file")
                                    val label = BarsoftTemplateParser.parse(inputStream)
                                    inputStream.close()
                                    val sizeStr = "${label.widthMm.toInt()}x${label.heightMm.toInt()}"
                                    templates.add(ScanTemplateOption(
                                        id = "barsoft_${category}_$file",
                                        name = "$sizeStr $file",
                                        label = label
                                    ))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // 默认选中第一个自定义模板（如果有的话），否则选内置
            val selectedId = templates.find { it.id.startsWith("custom_") }?.id
                ?: templates.firstOrNull()?.id
                ?: ""
            _uiState.value = _uiState.value.copy(
                templates = templates,
                selectedTemplateId = selectedId
            )
        }
    }

    /**
     * 扫码后查询商品库
     */
    fun onBarcodeScanned(barcode: String) {
        _uiState.value = _uiState.value.copy(
            lastScannedCode = barcode,
            isLoading = true,
            errorMessage = null,
            showProductDialog = false
        )
        viewModelScope.launch {
            val dbProduct = productDao.getProductByBarcode(barcode)
            if (dbProduct != null) {
                // 找到了，直接显示商品信息
                _uiState.value = _uiState.value.copy(
                    productInfo = dbProduct.toDomain(),
                    productExistsInDb = true,
                    isLoading = false
                )
            } else {
                // 未找到，弹出维护商品资料框
                _uiState.value = _uiState.value.copy(
                    productInfo = ProductInfo(barcode = barcode),
                    productExistsInDb = false,
                    isLoading = false,
                    showProductDialog = true,
                    dialogName = "",
                    dialogPrice = ""
                )
            }
        }
    }

    /**
     * 维护弹窗：更新名称
     */
    fun updateDialogName(name: String) {
        _uiState.value = _uiState.value.copy(dialogName = name)
    }

    /**
     * 维护弹窗：更新价格
     */
    fun updateDialogPrice(price: String) {
        _uiState.value = _uiState.value.copy(dialogPrice = price)
    }

    /**
     * 维护弹窗：保存商品到数据库
     */
    fun saveProductFromDialog() {
        val state = _uiState.value
        val name = state.dialogName.trim()
        val price = state.dialogPrice.toDoubleOrNull() ?: 0.0

        if (name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入商品名称")
            return
        }
        if (price <= 0) {
            _uiState.value = state.copy(errorMessage = "请输入有效价格")
            return
        }

        viewModelScope.launch {
            try {
                val product = ProductInfo(
                    barcode = state.productInfo.barcode,
                    name = name,
                    price = price
                )
                productDao.insert(com.gp.q733.data.local.db.ProductEntity(
                    barcode = product.barcode,
                    name = product.name,
                    price = product.price
                ))
                // 保存成功，更新状态
                _uiState.value = _uiState.value.copy(
                    productInfo = product,
                    productExistsInDb = true,
                    showProductDialog = false,
                    successMessage = "商品已保存"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 关闭维护弹窗
     */
    /**
     * 未找到提示：用户确认去维护
     */
    fun confirmNotFound() {
        _uiState.value = _uiState.value.copy(
            showNotFoundDialog = false,
            showProductDialog = true,
            dialogName = "",
            dialogPrice = ""
        )
    }

    /**
     * 未找到提示：用户取消
     */
    fun dismissNotFound() {
        _uiState.value = _uiState.value.copy(showNotFoundDialog = false)
    }

    fun dismissProductDialog() {
        _uiState.value = _uiState.value.copy(showProductDialog = false)
    }

    /**
     * 选择模板
     */
    fun selectTemplate(templateId: String) {
        _uiState.value = _uiState.value.copy(selectedTemplateId = templateId)
    }

    /**
     * 获取当前选中的模板
     */
    fun getSelectedTemplate(): ScanTemplateOption? {
        return _uiState.value.templates.find { it.id == _uiState.value.selectedTemplateId }
    }

    /**
     * 打印：用选中模板 + 商品数据填充 → 打印
     */
    fun print() {
        val state = _uiState.value
        val product = state.productInfo
        if (product.barcode.isBlank() || product.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请先扫码并确认商品信息")
            return
        }

        val template = state.templates.find { it.id == state.selectedTemplateId }
        if (template == null) {
            _uiState.value = state.copy(errorMessage = "请选择打印模板")
            return
        }

        _uiState.value = state.copy(isPrinting = true, errorMessage = null)

        viewModelScope.launch {
            try {
                // 检查蓝牙连接
                val connState = bluetoothRepository.getConnectionState().first()
                if (connState != ConnectionState.Connected) {
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
                                    errorMessage = "连接失败: ${connectResult.exceptionOrNull()?.message}"
                                )
                                return@launch
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(isPrinting = false, errorMessage = "打印机未连接")
                            return@launch
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(isPrinting = false, errorMessage = "打印机未连接")
                        return@launch
                    }
                }

                // 用模板填充商品数据
                val filledLabel = LabelTemplateFiller.fillTemplate(template.label, product)

                // 生成打印指令并发送
                val cmdBytes = gpPrinterService.generatePrintCommands(filledLabel)
                val writeResult = bluetoothRepository.write(cmdBytes)

                writeResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        successMessage = "打印成功"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        errorMessage = "打印失败: ${error.message}"
                    )
                    bluetoothRepository.disconnect()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    errorMessage = "打印失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 将当前选中模板保存为自定义模板
     */
    fun saveCurrentAsTemplate(name: String) {
        val state = _uiState.value
        val template = state.templates.find { it.id == state.selectedTemplateId } ?: return
        if (name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入模板名称")
            return
        }
        viewModelScope.launch {
            try {
                val json = com.gp.q733.domain.util.TemplateJsonParser.toJson(template.label.elements)
                customTemplateDao.insert(
                    com.gp.q733.data.local.db.CustomTemplateEntity(
                        name = name,
                        widthMm = template.label.widthMm,
                        heightMm = template.label.heightMm,
                        elementsJson = json
                    )
                )
                // Reload templates
                loadTemplates()
                _uiState.value = _uiState.value.copy(successMessage = "模板已保存: $name")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "保存失败: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    /**
     * 构建默认标签模板
     */
    private fun buildDefaultLabel(width: Float, height: Float): Label {
        return Label(
            elements = listOf(
                LabelElement.Text(x = 3f, y = 1f, text = "{productName}", fontSize = 10f, isBold = true, textName = "name"),
                LabelElement.Text(x = 3f, y = 8f, text = "{price}", fontSize = 8f, isBold = false, textName = "price"),
                LabelElement.Barcode(x = 3f, y = 15f, content = "{barcode}", format = com.gp.q733.domain.model.BarcodeFormat.CODE128, height = 8f, textName = "barcode")
            ),
            widthMm = width,
            heightMm = height
        )
    }
}
