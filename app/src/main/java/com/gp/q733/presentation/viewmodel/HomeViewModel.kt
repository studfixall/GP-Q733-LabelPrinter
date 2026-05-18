package com.gp.q733.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.LabelDataStore
import com.gp.q733.domain.model.BarcodeFormat
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.LabelTemplate
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentLabels: List<Label> = emptyList(),
    val templates: List<LabelTemplate> = emptyList(),
    val printerStatus: PrinterStatus = PrinterStatus.Disconnected,
    val showConnectionDialog: Boolean = false,
    val isLoading: Boolean = false,
    val newLabelWidth: Float = 50f,
    val newLabelHeight: Float = 30f
)

enum class PrinterStatus {
    Connected,
    Connecting,
    Disconnected,
    Error
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val labelDataStore: LabelDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = bluetoothRepository.getConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.Disconnected
        )

    init {
        // Load templates
        _uiState.value = _uiState.value.copy(templates = createDefaultTemplates())

        // Observe connection state and update UI
        viewModelScope.launch {
            connectionState.collect { state ->
                val status = when (state) {
                    ConnectionState.Connected -> PrinterStatus.Connected
                    ConnectionState.Connecting -> PrinterStatus.Connecting
                    ConnectionState.Disconnected -> PrinterStatus.Disconnected
                    ConnectionState.Error -> PrinterStatus.Error
                }
                _uiState.value = _uiState.value.copy(printerStatus = status)
            }
        }

        // Load saved labels
        loadSavedLabels()
    }

    private fun loadSavedLabels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            labelDataStore.savedLabelsFlow.collect { labels ->
                _uiState.value = _uiState.value.copy(
                    recentLabels = labels,
                    isLoading = false
                )
            }
        }
    }

    private fun createDefaultTemplates(): List<LabelTemplate> {
        return listOf(
            LabelTemplate(
                id = "express",
                name = "快递面单",
                description = "50×30mm 标准快递标签",
                label = Label(
                    id = "template_express",
                    widthMm = 50f,
                    heightMm = 30f,
                    elements = listOf(
                        LabelElement.Text(x = 2f, y = 2f, text = "收件人：张三", fontSize = 8f, isBold = true),
                        LabelElement.Text(x = 2f, y = 10f, text = "电话：138****8888", fontSize = 7f, isBold = false),
                        LabelElement.Text(x = 2f, y = 17f, text = "北京市朝阳区xxx街道", fontSize = 7f, isBold = false),
                        LabelElement.Barcode(x = 2f, y = 24f, content = "SF1234567890", format = BarcodeFormat.CODE128, height = 5f)
                    )
                )
            ),
            LabelTemplate(
                id = "product",
                name = "商品标签",
                description = "40×30mm 商品信息标签",
                label = Label(
                    id = "template_product",
                    widthMm = 40f,
                    heightMm = 30f,
                    elements = listOf(
                        LabelElement.Text(x = 2f, y = 2f, text = "商品名称", fontSize = 9f, isBold = true),
                        LabelElement.Text(x = 2f, y = 10f, text = "型号：ABC-001", fontSize = 7f, isBold = false),
                        LabelElement.QRCode(x = 25f, y = 2f, content = "https://item.example.com/123", size = 12f),
                        LabelElement.Text(x = 2f, y = 20f, text = "￥99.00", fontSize = 10f, isBold = true)
                    )
                )
            ),
            LabelTemplate(
                id = "price",
                name = "价格标签",
                description = "30×20mm 简洁价格标签",
                label = Label(
                    id = "template_price",
                    widthMm = 30f,
                    heightMm = 20f,
                    elements = listOf(
                        LabelElement.Text(x = 2f, y = 2f, text = "特价", fontSize = 7f, isBold = false),
                        LabelElement.Text(x = 2f, y = 8f, text = "￥49.9", fontSize = 12f, isBold = true),
                        LabelElement.Barcode(x = 2f, y = 16f, content = "690123456789", format = BarcodeFormat.EAN13, height = 3f)
                    )
                )
            )
        )
    }

    fun showConnectionDialog() {
        _uiState.value = _uiState.value.copy(showConnectionDialog = true)
    }

    fun hideConnectionDialog() {
        _uiState.value = _uiState.value.copy(showConnectionDialog = false)
    }

    fun deleteLabel(label: Label) {
        viewModelScope.launch {
            labelDataStore.deleteLabel(label.id)
        }
    }
    
    fun setNewLabelSize(width: Float, height: Float) {
        _uiState.value = _uiState.value.copy(
            newLabelWidth = width,
            newLabelHeight = height
        )
    }
}
