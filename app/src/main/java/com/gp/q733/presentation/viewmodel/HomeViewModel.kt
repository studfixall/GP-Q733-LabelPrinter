package com.gp.q733.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.db.CustomTemplateDao
import com.gp.q733.data.local.db.CustomTemplateEntity
import com.gp.q733.data.local.LabelDataStore
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.domain.util.TemplateJsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabelTemplate(
    val templateId: String,
    val name: String,
    val description: String,
    val widthMm: Float,
    val heightMm: Float,
    val label: Label,
    val isBuiltIn: Boolean
)

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
    private val labelDataStore: LabelDataStore,
    private val customTemplateDao: CustomTemplateDao
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
        // 加载所有模板（内置 + 自定义）
        viewModelScope.launch {
            customTemplateDao.getAllSorted().collect { entities ->
                _uiState.value = _uiState.value.copy(
                    templates = entities.map { it.toLabelTemplate() }
                )
            }
        }

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

    fun deleteTemplate(template: LabelTemplate) {
        viewModelScope.launch {
            if (!template.isBuiltIn) {
                customTemplateDao.getByTemplateId(template.templateId)?.let {
                    customTemplateDao.delete(it)
                }
            }
        }
    }
    
    fun setNewLabelSize(width: Float, height: Float) {
        _uiState.value = _uiState.value.copy(
            newLabelWidth = width,
            newLabelHeight = height
        )
    }

    private fun CustomTemplateEntity.toLabelTemplate(): LabelTemplate {
        val elements = TemplateJsonParser.fromJson(elementsJson)
        val description = if (isBuiltIn) {
            "内置模板 · ${widthMm.toInt()}×${heightMm.toInt()}mm"
        } else {
            "自定义模板 · ${widthMm.toInt()}×${heightMm.toInt()}mm"
        }
        return LabelTemplate(
            templateId = templateId,
            name = name,
            description = description,
            widthMm = widthMm,
            heightMm = heightMm,
            label = Label(
                id = templateId,
                elements = elements,
                widthMm = widthMm,
                heightMm = heightMm
            ),
            isBuiltIn = isBuiltIn
        )
    }
}
