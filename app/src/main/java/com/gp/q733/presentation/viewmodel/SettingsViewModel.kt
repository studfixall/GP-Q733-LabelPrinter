package com.gp.q733.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.data.local.AppSettings
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.data.remote.RmisApiClient
import com.gp.q733.data.remote.RmisProductRepository
import com.gp.q733.domain.print.PaperType
import com.gp.q733.domain.print.PrintProtocol
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoreInfo(
    val id: String,
    val name: String
)

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
    val printOffsetX: Float = 0f,
    val printOffsetY: Float = 0f,
    val autoReconnect: Boolean = true,
    val reconnectInterval: Int = 5,
    val darkMode: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    // Issue #13: Multi-store + RMIS settings
    val storeId: String = "",
    val storeName: String = "",
    val rmisBaseUrl: String = "",
    val rmisUserNo: String = "",
    val rmisMasterKey: String = "",
    val storeList: List<StoreInfo> = emptyList(),
    val isLoadingStores: Boolean = false,
    val storeLoadError: String? = null,
    val showStorePicker: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val rmisApiClient: RmisApiClient,
    private val rmisProductRepository: RmisProductRepository
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
                    printOffsetX = settings.printOffsetX,
                    printOffsetY = settings.printOffsetY,
                    autoReconnect = settings.autoReconnect,
                    reconnectInterval = settings.reconnectInterval,
                    storeId = settings.storeId,
                    storeName = settings.storeName,
                    rmisBaseUrl = settings.rmisBaseUrl,
                    rmisUserNo = settings.rmisUserNo,
                    rmisMasterKey = settings.rmisMasterKey
                ) }
                // 同步配置到RMIS客户端
                syncRmisConfig(settings)
            }
        }
    }
    private fun syncRmisConfig(settings: AppSettings) {
        rmisApiClient.updateConfig(
            baseUrl = settings.rmisBaseUrl,
            userNo = settings.rmisUserNo,
            masterKey = settings.rmisMasterKey
        )
        rmisProductRepository.storeId = settings.storeId
    }
    fun updateLabelWidth(width: Float) { _uiState.update { it.copy(labelWidth = width) } }
    fun updateLabelHeight(height: Float) { _uiState.update { it.copy(labelHeight = height) } }
    fun updatePrintCopies(copies: Int) { if (copies in 1..99) { _uiState.update { it.copy(printCopies = copies) } } }
    fun updatePrintProtocol(protocol: PrintProtocol) { _uiState.update { it.copy(printProtocol = protocol) } }
    fun updatePrintDensity(density: Int) { if (density in 0..15) { _uiState.update { it.copy(printDensity = density) } } }
    fun updatePrintSpeed(speed: Int) { if (speed in 1..10) { _uiState.update { it.copy(printSpeed = speed) } } }
    fun updatePaperType(paperType: PaperType) { _uiState.update { it.copy(paperType = paperType) } }
    fun updateGapMm(gapMm: Float) { _uiState.update { it.copy(gapMm = gapMm) } }
    fun updateBlackMarkOffset(offset: Float) { _uiState.update { it.copy(blackMarkOffset = offset) } }
    fun updateAutoReconnect(enabled: Boolean) { _uiState.update { it.copy(autoReconnect = enabled) } }
    fun updateReconnectInterval(seconds: Int) { if (seconds in 1..60) { _uiState.update { it.copy(reconnectInterval = seconds) } } }
    fun updatePrintOffsetX(offset: Float) { viewModelScope.launch { settingsDataStore.savePrintOffsetX(offset) } }
    fun updatePrintOffsetY(offset: Float) { viewModelScope.launch { settingsDataStore.savePrintOffsetY(offset) } }
    fun updateDarkMode(enabled: Boolean) { _uiState.update { it.copy(darkMode = enabled) } }
    // Issue #13: Multi-store + RMIS settings
    fun updateRmisBaseUrl(url: String) { _uiState.update { it.copy(rmisBaseUrl = url) } }
    fun updateRmisUserNo(userNo: String) { _uiState.update { it.copy(rmisUserNo = userNo) } }
    fun updateRmisMasterKey(key: String) { _uiState.update { it.copy(rmisMasterKey = key) } }
    fun selectStore(store: StoreInfo) {
        _uiState.update { it.copy(
            storeId = store.id,
            storeName = store.name,
            showStorePicker = false
        ) }
        viewModelScope.launch {
            settingsDataStore.saveStoreId(store.id)
            settingsDataStore.saveStoreName(store.name)
            rmisProductRepository.storeId = store.id
        }
    }
    fun showStorePicker() {
        _uiState.update { it.copy(showStorePicker = true) }
        loadStoreList()
    }
    fun dismissStorePicker() {
        _uiState.update { it.copy(showStorePicker = false) }
    }
    private fun loadStoreList() {
        if (_uiState.value.rmisBaseUrl.isBlank() || _uiState.value.rmisUserNo.isBlank()) {
            _uiState.update { it.copy(storeLoadError = "请先配置RMIS地址和应用程序编码") }
            return
        }
        _uiState.update { it.copy(isLoadingStores = true, storeLoadError = null) }
        viewModelScope.launch {
            try {
                val stores = rmisProductRepository.getStoreList()
                _uiState.update { it.copy(
                    storeList = stores.map { (id, name) -> StoreInfo(id, name) },
                    isLoadingStores = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoadingStores = false,
                    storeLoadError = "获取门店列表失败: ${e.message}"
                ) }
            }
        }
    }
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
            printOffsetX = current.printOffsetX,
            printOffsetY = current.printOffsetY,
                    autoReconnect = current.autoReconnect,
                    reconnectInterval = current.reconnectInterval,
                    storeId = current.storeId,
                    storeName = current.storeName,
                    rmisBaseUrl = current.rmisBaseUrl,
                    rmisUserNo = current.rmisUserNo,
                    rmisMasterKey = current.rmisMasterKey
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
