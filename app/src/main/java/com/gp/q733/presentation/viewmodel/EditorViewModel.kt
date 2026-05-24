package com.gp.q733.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.LabelDataStore
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.BarcodeFormat
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.PrinterDevice
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.print.PrintProtocol
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ElementType { Text, Barcode, QRCode, Line }

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
    val showSaveTemplateDialog: Boolean = false,
    val templateName: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val gpPrinterService: GpPrinterService,
    private val settingsDataStore: SettingsDataStore,
    private val labelDataStore: LabelDataStore,
    private val customTemplateDao: com.gp.q733.data.local.db.CustomTemplateDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var connectedDevice: BluetoothDevice? = null

    init {
        viewModelScope.launch {
            bluetoothRepository.getConnectedDeviceFlow().collect { device ->
                connectedDevice = device
            }
        }
    }

    fun setConnectedDevice(device: BluetoothDevice?) {
        connectedDevice = device
    }

    fun showSaveTemplateDialog() {
        _uiState.value = _uiState.value.copy(showSaveTemplateDialog = true, templateName = "自定义模板")
    }

    fun dismissSaveTemplateDialog() {
        _uiState.value = _uiState.value.copy(showSaveTemplateDialog = false)
    }

    fun updateTemplateName(name: String) {
        _uiState.value = _uiState.value.copy(templateName = name)
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
                    x = 5f, y = 5f + (elements.size * 8f),
                    text = textContent, fontSize = 8f, isBold = false
                )
                ElementType.Barcode -> LabelElement.Barcode(
                    x = 5f, y = 5f + (elements.size * 10f),
                    content = textContent, format = format ?: BarcodeFormat.CODE128, height = 8f
                )
                ElementType.QRCode -> LabelElement.QRCode(
                    x = 5f, y = 5f + (elements.size * 10f),
                    content = textContent, size = 12f
                )
                ElementType.Line -> LabelElement.Line(
                    x = 5f, y = 5f + (elements.size * 8f),
                    width = currentLabel.widthMm - 10f, height = 0.5f
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

    /**
     * 保存为自定义模板（存入Room数据库）
     */
    fun saveAsTemplate(templateName: String) {
        viewModelScope.launch {
            val label = _uiState.value.label
            val elementsJson = com.gp.q733.domain.util.TemplateJsonParser.toJson(label.elements)
            customTemplateDao.insert(
                com.gp.q733.data.local.db.CustomTemplateEntity(
                    name = templateName,
                    widthMm = label.widthMm,
                    heightMm = label.heightMm,
                    elementsJson = elementsJson
                )
            )
            _uiState.value = _uiState.value.copy(saveSuccess = true)
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(saveSuccess = false)
        }
    }

    /**
     * Print label - generate commands via GpPrinterService, send via BluetoothRepository socket
     * FIX: No longer use SDK RTPrinter connection (unstable, disconnects after 2-3s)
     */
    fun printLabel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrinting = true, errorMessage = null)
            try {
                val label = _uiState.value.label
                if (label.elements.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "\u6807\u7b7e\u5185\u5bb9\u4e3a\u7a7a\uff0c\u8bf7\u5148\u6dfb\u52a0\u5143\u7d20",
                        isPrinting = false
                    )
                    return@launch
                }

                // Check connection state
                val connectionState = bluetoothRepository.getConnectionState().first()
                if (connectionState != ConnectionState.Connected) {
                    // Try to reconnect using saved MAC
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

                // Generate commands via SDK, send via socket
                android.util.Log.d("PrintDebug", "Editor print - generate commands, send via socket")
                val cmdBytes = gpPrinterService.generatePrintCommands(label)
                val writeResult = bluetoothRepository.write(cmdBytes)
                writeResult.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "\u6253\u5370\u5931\u8d25"
                    )
                    bluetoothRepository.disconnect()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "\u6253\u5370\u9519\u8bef\uff1a${e.message}"
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
        viewModelScope.launch { loadTemplateSync(templateId) }
    }

    fun loadTemplateSync(templateId: String) {
        val template = when (templateId) {
            "express" -> Label(
                id = "template_express_${System.currentTimeMillis()}",
                widthMm = 50f, heightMm = 30f,
                elements = listOf(
                    LabelElement.Text(x = 2f, y = 2f, text = "\u6536\u4ef6\u4eba:\u5f20\u4e09", fontSize = 8f, isBold = true),
                    LabelElement.Text(x = 2f, y = 10f, text = "\u7535\u8bdd:138****8888", fontSize = 7f, isBold = false),
                    LabelElement.Text(x = 2f, y = 17f, text = "\u5317\u4eac\u5e02\u671d\u9633\u533axxx\u8857\u9053", fontSize = 7f, isBold = false),
                    LabelElement.Barcode(x = 2f, y = 24f, content = "SF1234567890", format = BarcodeFormat.CODE128, height = 5f)
                )
            )
            "product" -> Label(
                id = "template_product_${System.currentTimeMillis()}",
                widthMm = 40f, heightMm = 30f,
                elements = listOf(
                    LabelElement.Text(x = 2f, y = 2f, text = "\u5546\u54c1\u540d\u79f0", fontSize = 9f, isBold = true),
                    LabelElement.Text(x = 2f, y = 10f, text = "\u578b\u53f7:ABC-001", fontSize = 7f, isBold = false),
                    LabelElement.QRCode(x = 25f, y = 2f, content = "https://item.example.com/123", size = 12f),
                    LabelElement.Text(x = 2f, y = 20f, text = "\u00a599.00", fontSize = 10f, isBold = true)
                )
            )
            "price" -> Label(
                id = "template_price_${System.currentTimeMillis()}",
                widthMm = 30f, heightMm = 20f,
                elements = listOf(
                    LabelElement.Text(x = 2f, y = 2f, text = "\u7279\u4ef7", fontSize = 7f, isBold = false),
                    LabelElement.Text(x = 2f, y = 8f, text = "\u00a59.9", fontSize = 12f, isBold = true),
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

    fun updateTextFontSize(index: Int, fontSize: Float) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()
            if (index in elements.indices) {
                val element = elements[index]
                if (element is LabelElement.Text) {
                    elements[index] = element.copy(fontSize = fontSize)
                    _uiState.value = _uiState.value.copy(
                        label = currentLabel.copy(elements = elements)
                    )
                }
            }
        }
    }

    fun updateTextBold(index: Int, isBold: Boolean) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()
            if (index in elements.indices) {
                val element = elements[index]
                if (element is LabelElement.Text) {
                    elements[index] = element.copy(isBold = isBold)
                    _uiState.value = _uiState.value.copy(
                        label = currentLabel.copy(elements = elements)
                    )
                }
            }
        }
    }

    fun updateTextUnderline(index: Int, isUnderline: Boolean) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()
            if (index in elements.indices) {
                val element = elements[index]
                if (element is LabelElement.Text) {
                    elements[index] = element.copy(isUnderline = isUnderline)
                    _uiState.value = _uiState.value.copy(
                        label = currentLabel.copy(elements = elements)
                    )
                }
            }
        }
    }

    /**
     * 导出为 Barsoft XML 格式字符串
     */
    fun exportBarsoftXml(): String {
        val label = _uiState.value.label
        val sb = StringBuilder()
        sb.append(""")<?xml version="1.0" encoding="utf-8"?>
<Barsoft Version="1.0" width="${label.widthMm.toInt()}" height="${label.heightMm.toInt()}" gap="0" speed="3" density="15">
  <items>
""")
        for (element in label.elements) {
            when (element) {
                is LabelElement.Text -> {
                    val align = when (element.align) {
                        1 -> "ALIGN_CENTER"
                        2 -> "ALIGN_RIGHT"
                        else -> "ALIGN_NORMAL"
                    }
                    sb.append(""")    <item viewtype="0" text="${element.text.replace("\"", "&quot;")}" left="${String.format("%.1f", element.x)}" top="${String.format("%.1f", element.y)}" width="${String.format("%.1f", if (element.widthMm > 0) element.widthMm else label.widthMm - element.x)}" height="${String.format("%.1f", if (element.heightMm > 0) element.heightMm else 5)}" textsize="${element.fontSize.toInt()}" font="${element.fontId}" align="$align" variable="0" />
""")
                }
                is LabelElement.Barcode -> {
                    val formatName = when (element.format) {
                        BarcodeFormat.CODE128 -> "CODE_128"
                        BarcodeFormat.CODE39 -> "CODE_39"
                        BarcodeFormat.EAN13 -> "EAN13"
                    }
                    sb.append(""")    <item viewtype="1" text="${element.content.replace("\"", "&quot;")}" format="$formatName" left="${String.format("%.1f", element.x)}" top="${String.format("%.1f", element.y)}" width="${String.format("%.1f", element.widthMm)}" height="${String.format("%.1f", element.height)}" MinBarWidth="${element.minBarWidth}" textposition="${element.textPosition}" />
""")
                }
                is LabelElement.QRCode -> {
                    sb.append(""")    <item viewtype="3" text="${element.content.replace("\"", "&quot;")}" left="${String.format("%.1f", element.x)}" top="${String.format("%.1f", element.y)}" width="${String.format("%.1f", element.size)}" height="${String.format("%.1f", element.size)}" textsize="8" />
""")
                }
                is LabelElement.Line -> {
                    sb.append(""")    <item viewtype="2" left="${String.format("%.1f", element.x)}" top="${String.format("%.1f", element.y)}" width="${String.format("%.1f", element.width)}" height="${String.format("%.1f", element.height)}" />
""")
                }
            }
        }
        sb.append(""">  </items>
</Barsoft>
""")
        return sb.toString()
    }
}
