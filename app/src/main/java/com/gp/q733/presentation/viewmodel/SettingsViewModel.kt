package com.gp.q733.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.AppSettings
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.print.PaperType
import com.gp.q733.domain.print.PrintProtocol
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val labelWidth: Float = 50.0f,
    val labelHeight: Float = 30.0f,
    val printCopies: Int = 1,
    val printProtocol: PrintProtocol = PrintProtocol.CPCL,
    val printDensity: Int = 8,
    val printSpeed: Int = 4,
    val paperType: PaperType = PaperType.LABEL,
    val gapMm: Float = 2f,
    val blackMarkOffset: Float = 0f,
    val autoReconnect: Boolean = true,
    val reconnectInterval: Int = 5,
    val darkMode: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                _uiState.update { it.copy(
                    labelWidth = settings.labelWidth,
                    labelHeight = settings.labelHeight,
                    printCopies = settings.printCopies,
                    printProtocol = settings.printProtocol,
                    printDensity = settings.printDensity,
                    printSpeed = settings.printSpeed,
                    paperType = settings.paperType,
                    gapMm = settings.gapMm,
                    blackMarkOffset = settings.blackMarkOffset,
                    autoReconnect = settings.autoReconnect,
                    reconnectInterval = settings.reconnectInterval
                ) }
            }
        }
    }

    fun updateLabelWidth(width: Float) { _uiState.update { it.copy(labelWidth = width) } }
    fun updateLabelHeight(height: Float) { _uiState.update { it.copy(labelHeight = height) } }

    fun updatePrintCopies(copies: Int) {
        if (copies in 1..99) { _uiState.update { it.copy(printCopies = copies) } }
    }

    fun updatePrintProtocol(protocol: PrintProtocol) { _uiState.update { it.copy(printProtocol = protocol) } }
    fun updatePrintDensity(density: Int) {
        if (density in 0..15) { _uiState.update { it.copy(printDensity = density) } }
    }

    fun updatePrintSpeed(speed: Int) {
        if (speed in 1..10) { _uiState.update { it.copy(printSpeed = speed) } }
    }

    fun updatePaperType(paperType: PaperType) { _uiState.update { it.copy(paperType = paperType) } }
    fun updateGapMm(gapMm: Float) { _uiState.update { it.copy(gapMm = gapMm) } }
    fun updateBlackMarkOffset(offset: Float) { _uiState.update { it.copy(blackMarkOffset = offset) } }
    fun updateAutoReconnect(enabled: Boolean) { _uiState.update { it.copy(autoReconnect = enabled) } }

    fun updateReconnectInterval(seconds: Int) {
        if (seconds in 1..60) { _uiState.update { it.copy(reconnectInterval = seconds) } }
    }

    fun updateDarkMode(enabled: Boolean) { _uiState.update { it.copy(darkMode = enabled) } }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false) }
            val current = _uiState.value
            settingsDataStore.saveAllSettings(
                AppSettings(
                    labelWidth = current.labelWidth,
                    labelHeight = current.labelHeight,
                    printCopies = current.printCopies,
                    printProtocol = current.printProtocol,
                    printDensity = current.printDensity,
                    printSpeed = current.printSpeed,
                    paperType = current.paperType,
                    gapMm = current.gapMm,
                    blackMarkOffset = current.blackMarkOffset,
                    autoReconnect = current.autoReconnect,
                    reconnectInterval = current.reconnectInterval
                )
            )
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            settingsDataStore.saveAllSettings(AppSettings())
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }
}
