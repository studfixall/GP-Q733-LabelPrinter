package com.gp.q733.presentation.viewmodel

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.LabelDataStore
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.BarcodeFormat
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.util.BarsoftTemplateParser
import com.gp.q733.domain.util.TemplateJsonParser
import com.gp.q733.domain.model.PrinterDevice
import com.gp.q733.domain.print.GpPrinterService
import com.gp.q733.domain.print.PrintProtocol
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val currentTemplateId: String? = null,   // 正在编辑的模板ID（来自导航参数）
    val errorMessage: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val gpPrinterService: GpPrinterService,
    private val settingsDataStore: SettingsDataStore,
    private val labelDataStore: LabelDataStore,
    private val customTemplateDao: com.gp.q733.data.local.db.CustomTemplateDao,
    @ApplicationContext private val context: android.content.Context
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
    fun updateLabelOffset(offsetX: Float, offsetY: Float) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            _uiState.value = _uiState.value.copy(
                label = currentLabel.copy(offsetX = offsetX, offsetY = offsetY)
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
     * 直接保存当前模板到Room（覆盖，不弹对话框）
     * - 有 currentTemplateId → 直接 upsert 覆盖
     * - 无 currentTemplateId → 自动生成名称并保存
     */
    fun saveCurrentTemplate() {
        viewModelScope.launch {
            val label = _uiState.value.label
            val elementsJson = TemplateJsonParser.toJson(label.elements)
            val tid = _uiState.value.currentTemplateId
            val (finalTemplateId, finalIsBuiltIn, finalName) = when {
                tid == null -> {
                    val autoName = "\u6807\u7b7e ${label.widthMm.toInt()}x${label.heightMm.toInt()}"
                    val existing = customTemplateDao.getByNameAndSize(autoName, label.widthMm, label.heightMm)
                    if (existing != null) {
                        Triple(existing.templateId, existing.isBuiltIn, existing.name)
                    } else {
                        Triple("custom_${System.currentTimeMillis()}", false, autoName)
                    }
                }
                tid.startsWith("templates/") -> {
                    val existing = customTemplateDao.getByTemplateId(tid)
                    Triple(tid, true, existing?.name ?: "\u5185\u7f6e\u6a21\u677f")
                }
                else -> {
                    val existing = customTemplateDao.getByTemplateId(tid)
                    Triple(tid, existing?.isBuiltIn ?: false, existing?.name ?: "\u81ea\u5b9a\u4e49\u6a21\u677f")
                }
            }
            customTemplateDao.upsert(
                templateId = finalTemplateId,
                name = finalName,
                widthMm = label.widthMm,
                heightMm = label.heightMm,
                elementsJson = elementsJson,
                offsetX = label.offsetX,
                offsetY = label.offsetY,
                isBuiltIn = finalIsBuiltIn,
                sortOrder = 0,
                createdAt = System.currentTimeMillis()
            )
            _uiState.value = _uiState.value.copy(
                saveSuccess = true,
                currentTemplateId = finalTemplateId
            )
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(saveSuccess = false)
        }
    }

/**
     * 保存为自定义模板（存入Room数据库）
     * @param sourceTemplateId 来源模板ID（来自导航参数）。
     *   null               → 新建模板
     *   templates/...      → 内置模板另存（upsert 到同一 templateId，isBuiltIn=true）
     *   saved_xxx / custom_xxx → 更新已有自定义模板记录
     */
    fun saveAsTemplate(sourceTemplateId: String?, templateName: String) {
        viewModelScope.launch {
            val label = _uiState.value.label
            val elementsJson = com.gp.q733.domain.util.TemplateJsonParser.toJson(label.elements)
            // 根据来源决定 templateId 与 isBuiltIn
            val templateNameToUse = templateName.ifBlank { "自定义模板" }
            val (finalTemplateId, finalIsBuiltIn) = when {
                sourceTemplateId == null -> {
                    // 纯新建：先按 name+size 查重，命中则更新已有记录，避免重复
                    val existing = customTemplateDao.getByNameAndSize(templateNameToUse, label.widthMm, label.heightMm)
                    if (existing != null) {
                        existing.templateId to existing.isBuiltIn
                    } else {
                        "custom_${System.currentTimeMillis()}" to false
                    }
                }
                sourceTemplateId.startsWith("templates/") -> {
                    // 内置模板另存为：复用同一 asset path 作为 templateId，upsert 覆盖
                    sourceTemplateId to true
                }
                else -> {
                    // saved_xxx / custom_xxx / built_in_xxx → 更新已有记录
                    sourceTemplateId to false
                }
            }
            customTemplateDao.upsert(
                templateId = finalTemplateId,
                name = templateNameToUse,
                widthMm = label.widthMm,
                heightMm = label.heightMm,
                elementsJson = elementsJson,
                offsetX = label.offsetX,
                offsetY = label.offsetY,
                isBuiltIn = finalIsBuiltIn,
                sortOrder = 0,
                createdAt = System.currentTimeMillis()
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
                        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
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
    suspend fun loadTemplateSync(templateId: String) {
        val label: Label? = when {
            templateId.startsWith("built_in_") || templateId.startsWith("custom_") -> {
                val entity = customTemplateDao.getByTemplateId(templateId)
                entity?.let {
                    Label(
                        id = "template_${System.currentTimeMillis()}",
                        widthMm = it.widthMm,
                        heightMm = it.heightMm,
                        offsetX = it.offsetX,
                        offsetY = it.offsetY,
                        elements = TemplateJsonParser.fromJson(it.elementsJson)
                    )
                }
            }
            templateId.startsWith("saved_") -> {
                // Room 数据库中的自定义模板，id = 数字
                val idStr = templateId.removePrefix("saved_")
                val id = idStr.toLongOrNull()
                id?.let {
                    val entity = customTemplateDao.getById(it)
                    entity?.let { e ->
                        Label(
                            id = "template_${e.id}",
                            widthMm = e.widthMm,
                            heightMm = e.heightMm,
                            offsetX = e.offsetX,
                            offsetY = e.offsetY,
                            elements = TemplateJsonParser.fromJson(e.elementsJson)
                        )
                    }
                }
            }
            templateId.startsWith("templates/") -> {
                // 先查 Room：保存过的内置模板（isBuiltIn=true）以 Room 记录为准
                val savedEntity = customTemplateDao.getByTemplateId(templateId)
                if (savedEntity != null) {
                    Label(
                        id = "template_${savedEntity.id}",
                        widthMm = savedEntity.widthMm,
                        heightMm = savedEntity.heightMm,
                        offsetX = savedEntity.offsetX,
                        offsetY = savedEntity.offsetY,
                        elements = TemplateJsonParser.fromJson(savedEntity.elementsJson)
                    )
                } else {
                    // 无 Room 记录，回退到 assets 原始 XML
                    BarsoftTemplateParser.loadFromAssets(context, templateId)
                }
            }
            else -> null
        }
        label?.let {
            _uiState.value = _uiState.value.copy(
                label = it,
                selectedElementIndex = null,
                currentTemplateId = templateId  // 保存当前模板ID，存模板时用于判断upsert目标
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
    // Issue #3 fix: Barcode数据绑定字段更新
    fun updateBarcodeTextName(index: Int, textName: String) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()
            if (index in elements.indices) {
                val element = elements[index]
                if (element is LabelElement.Barcode) {
                    elements[index] = element.copy(
                        textName = textName,
                        variable = if (textName.isNotEmpty()) 1 else 0
                    )
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
     * 设置文本元素的商品数据绑定字段
     * @param textName 绑定字段名（""=不绑定，固定文本）
     */
    fun updateTextName(index: Int, textName: String) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()
            if (index in elements.indices) {
                val element = elements[index]
                if (element is LabelElement.Text) {
                    elements[index] = element.copy(
                        textName = textName,
                        variable = if (textName.isNotEmpty()) 1 else 0
                    )
                    _uiState.value = _uiState.value.copy(
                        label = currentLabel.copy(elements = elements)
                    )
                }
            }
        }
    }
    fun updateQrTextName(index: Int, textName: String) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()
            if (index in elements.indices) {
                val element = elements[index]
                if (element is LabelElement.QRCode) {
                    elements[index] = element.copy(
                        textName = textName,
                        variable = if (textName.isNotEmpty()) 1 else 0
                    )
                    _uiState.value = _uiState.value.copy(
                        label = currentLabel.copy(elements = elements)
                    )
                }
            }
        }
    }

    fun updateQrSize(index: Int, size: Float) {
        viewModelScope.launch {
            val currentLabel = _uiState.value.label
            val elements = currentLabel.elements.toMutableList()
            if (index in elements.indices) {
                val element = elements[index]
                if (element is LabelElement.QRCode) {
                    elements[index] = element.copy(size = size)
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
                    sb.append(""")    <item viewtype="0" text="${element.text.replace("\"", "&quot;")}" left="${String.format("%.1f", element.x)}" top="${String.format("%.1f", element.y)}" width="${String.format("%.1f", if (element.widthMm > 0) element.widthMm else label.widthMm - element.x)}" height="${String.format("%.1f", if (element.heightMm > 0) element.heightMm else 5)}" textsize="${element.fontSize.toInt()}" font="${element.fontId}" align="$align" variable="${element.variable}" textName="${element.textName}" />
""")
                }
                is LabelElement.Barcode -> {
                    val formatName = when (element.format) {
                        BarcodeFormat.CODE128 -> "CODE_128"
                        BarcodeFormat.CODE39 -> "CODE_39"
                        BarcodeFormat.EAN13 -> "EAN13"
                    }
                    sb.append(""")    <item viewtype="1" text="${element.content.replace("\"", "&quot;")}" format="$formatName" left="${String.format("%.1f", element.x)}" top="${String.format("%.1f", element.y)}" width="${String.format("%.1f", element.widthMm)}" height="${String.format("%.1f", element.height)}" MinBarWidth="${element.minBarWidth}" textposition="${element.textPosition}" variable="${element.variable}" textName="${element.textName}" />
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
