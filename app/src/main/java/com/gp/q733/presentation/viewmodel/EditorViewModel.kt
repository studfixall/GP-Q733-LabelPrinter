package com.gp.q733.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.LabelDataStore
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.BarcodeFormat
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.print.PrintProtocol
import com.gp.q733.domain.repository.BluetoothRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ElementType {
    Text,
    Barcode,
    QRCode,
    Line
}

data class EditorUiState(
    val label: Label = Label(
        id = "label_${System.currentTimeMillis()}",
        elements = emptyList(),
        widthMm = 50f,
        heightMm = 30f
    ),
    val selectedElementIndex: Int? = null,
    val isEditing: Boolean = false,
    val showAddElementDialog: Boolean = false,
    val isSaving: Boolean = false,
    val isPrinting: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val gpPrinterService: GpPrinterService,
    private val settingsDataStore: SettingsDataStore,
    private val labelDataStore: LabelDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var connectedDevice: BluetoothDevice? = null

    init {
        // Sync connected device from repository flow
        viewModelScope.launch {
            bluetoothRepository.getConnectedDeviceFlow().collect { device ->
                connectedDevice = device
            }
        }
    }

    fun setConnectedDevice(device: BluetoothDevice?) {
        connectedDevice = device
    }

    fun showAddElementDialog() {
        _uiState.value = _uiState.value.copy(showAddElementDialog = true)
    }

    fun hideAddElementDialog() {
        _uiState.value = _uiState.value.copy(showAddElementDialog = false)
    }

    fun addElement(type: ElementType, textContent: String, format: BarcodeFormat? = null) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()

            val newElement = when (type) {
                ElementType.Text -> LabelElement.Text(
                    x = 5f,
                    y = 5f + (elements.size * 8f),
                    text = textContent,
                    fontSize = 8f,
                    isBold = false
                )
                ElementType.Barcode -> LabelElement.Barcode(
                    x = 5f,
                    y = 5f + (elements.size * 10f),
                    content = textContent,
                    format = format ?: BarcodeFormat.CODE128,
                    height = 8f
                )
                ElementType.QRCode -> LabelElement.QRCode(
                    x = 5f,
                    y = 5f + (elements.size * 10f),
                    content = textContent,
                    size = 12f
                )
                ElementType.Line -> LabelElement.Line(
                    x = 5f,
                    y = 5f + (elements.size * 8f),
                    width = currentLabel.widthMm - 10f,
                    height = 0.5f
                )
            }

            elements.add(newElement)
            val updatedLabel = currentLabel.copy(elements = elements)

            _uiState.value = _uiState.value.copy(
                label = updatedLabel,
                showAddElementDialog = false,
                selectedElementIndex = elements.lastIndex
            )
        }
    }

    fun selectElement(index: Int?) {
        _uiState.value = _uiState.value.copy(
            selectedElementIndex = index,
            isEditing = index != null
        )
    }

    fun updateElementContent(index: Int, newContent: String) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()

            if (index in elements.indices) {
                val element = elements[index]
                elements[index] = when (element) {
                    is LabelElement.Text -> element.copy(text = newContent)
                    is LabelElement.Barcode -> element.copy(content = newContent)
                    is LabelElement.QRCode -> element.copy(content = newContent)
                    is LabelElement.Line -> element
                }

                _uiState.value = _uiState.value.copy(
                    label = currentLabel.copy(elements = elements)
                )
            }
        }
    }

    fun updateElementPosition(index: Int, x: Float, y: Float) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()

            if (index in elements.indices) {
                val element = elements[index]
                elements[index] = when (element) {
                    is LabelElement.Text -> element.copy(x = x, y = y)
                    is LabelElement.Barcode -> element.copy(x = x, y = y)
                    is LabelElement.QRCode -> element.copy(x = x, y = y)
                    is LabelElement.Line -> element.copy(x = x, y = y)
                }

                _uiState.value = _uiState.value.copy(
                    label = currentLabel.copy(elements = elements)
                )
            }
        }
    }

    fun deleteElement(index: Int) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()

            if (index in elements.indices) {
                elements.removeAt(index)
                _uiState.value = _uiState.value.copy(
                    label = currentLabel.copy(elements = elements),
                    selectedElementIndex = null,
                    isEditing = false
                )
            }
        }
    }

    fun updateLabelSize(widthMm: Float, heightMm: Float) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            _uiState.value = _uiState.value.copy(
                label = currentLabel.copy(widthMm = widthMm, heightMm = heightMm)
            )
        }
    }

    fun saveLabel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveSuccess = false)

            val label = _uiState.value.label
            labelDataStore.saveLabel(label)

            _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)

            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(saveSuccess = false)
        }
    }

    fun printLabel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrinting = true, errorMessage = null)

            try {
                val label = _uiState.value.label

                // Check if label has elements
                if (label.elements.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "标签内容为空，请先添加元素"
                    )
                    _uiState.value = _uiState.value.copy(isPrinting = false)
                    return@launch
                }

                // Always reconnect before printing - SDK connection is not persistent
                val btDevice = connectedDevice ?: gpPrinterService.getCurrentDevice() ?: bluetoothRepository.getConnectedDevice()

                android.util.Log.d("PrintDebug", "Editor print - BT device: $btDevice")

                val result = if (btDevice != null) {
                    // Disconnect first to ensure clean state
                    gpPrinterService.disconnect()
                    // Connect and print
                    android.util.Log.d("PrintDebug", "Connecting GpPrinterService for label print")
                    val connectResult = gpPrinterService.connect(btDevice)
                    if (connectResult.isSuccess) {
                        val printResult = gpPrinterService.print(label)
                        // Disconnect after printing
                        gpPrinterService.disconnect()
                        printResult
                    } else {
                        Result.failure(connectResult.exceptionOrNull() ?: Exception("连接失败"))
                    }
                } else {
                    // No device available
                    android.util.Log.d("PrintDebug", "No BT device available for printing")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "请先连接蓝牙打印机"
                    )
                    _uiState.value = _uiState.value.copy(isPrinting = false)
                    return@launch
                }

                result.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "打印失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "打印错误：${e.message}"
                )
            }

            _uiState.value = _uiState.value.copy(isPrinting = false)
        }
    }

    fun loadLabelFromStore(labelId: String) {
        viewModelScope.launch {
            val savedLabel = labelDataStore.getLabel(labelId)
            savedLabel?.let {
                _uiState.value = _uiState.value.copy(
                    label = it,
                    selectedElementIndex = null,
                    isEditing = false
                )
            }
        }
    }

    fun loadTemplate(templateId: String) {
        viewModelScope.launch {
            loadTemplateSync(templateId)
        }
    }

    fun loadTemplateSync(templateId: String) {
        val template = when (templateId) {
            "express" -> Label(
                id = "template_express_${System.currentTimeMillis()}",
                widthMm = 50f,
                heightMm = 30f,
                elements = listOf(
                    LabelElement.Text(x = 2f, y = 2f, text = "收件人:张三", fontSize = 8f, isBold = true),
                    LabelElement.Text(x = 2f, y = 10f, text = "电话:138****8888", fontSize = 7f, isBold = false),
                    LabelElement.Text(x = 2f, y = 17f, text = "北京市朝阳区xxx街道", fontSize = 7f, isBold = false),
                    LabelElement.Barcode(x = 2f, y = 24f, content = "SF1234567890", format = BarcodeFormat.CODE128, height = 5f)
                )
            )
            "product" -> Label(
                id = "template_product_${System.currentTimeMillis()}",
                widthMm = 40f,
                heightMm = 30f,
                elements = listOf(
                    LabelElement.Text(x = 2f, y = 2f, text = "商品名称", fontSize = 9f, isBold = true),
                    LabelElement.Text(x = 2f, y = 10f, text = "型号:ABC-001", fontSize = 7f, isBold = false),
                    LabelElement.QRCode(x = 25f, y = 2f, content = "https://item.example.com/123", size = 12f),
                    LabelElement.Text(x = 2f, y = 20f, text = "¥99.00", fontSize = 10f, isBold = true)
                )
            )
            "price" -> Label(
                id = "template_price_${System.currentTimeMillis()}",
                widthMm = 30f,
                heightMm = 20f,
                elements = listOf(
                    LabelElement.Text(x = 2f, y = 2f, text = "特价", fontSize = 7f, isBold = false),
                    LabelElement.Text(x = 2f, y = 8f, text = "¥9.9", fontSize = 12f, isBold = true),
                    LabelElement.Barcode(x = 2f, y = 16f, content = "690123456789", format = BarcodeFormat.EAN13, height = 3f)
                )
            )
            else -> null
        }

        template?.let {
            _uiState.value = _uiState.value.copy(
                label = it,
                selectedElementIndex = null
            )
        }
    }

    fun resetLabel(widthMm: Float = 50f, heightMm: Float = 30f) {
        _uiState.value = EditorUiState(
            label = Label(
                id = "label_${System.currentTimeMillis()}",
                elements = emptyList(),
                widthMm = widthMm,
                heightMm = heightMm
            )
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun updateBarcodeWidth(index: Int, width: Float) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()

            if (index in elements.indices) {
                val element = elements[index]
                if (element is LabelElement.Barcode) {
                    elements[index] = element.copy(widthMm = width)
                    _uiState.value = _uiState.value.copy(
                        label = currentLabel.copy(elements = elements)
                    )
                }
            }
        }
    }

    fun updateBarcodeHeight(index: Int, height: Float) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()

            if (index in elements.indices) {
                val element = elements[index]
                if (element is LabelElement.Barcode) {
                    elements[index] = element.copy(height = height)
                    _uiState.value = _uiState.value.copy(
                        label = currentLabel.copy(elements = elements)
                    )
                }
            }
        }
    }
}
