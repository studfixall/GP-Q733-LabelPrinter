package com.gp.q733.domain.print

import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrintService @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val settingsDataStore: SettingsDataStore
) {

    suspend fun print(label: Label): Result<Unit> {
        val settings = settingsDataStore.settingsFlow.first()
        val protocol = settings.printProtocol
        val data = protocol.generate(label, settings.printDensity, settings.printSpeed, settings.gapMm)
        return bluetoothRepository.write(data)
    }

    suspend fun printTestPage(deviceName: String): Result<Unit> {
        val settings = settingsDataStore.settingsFlow.first()
        val protocol = settings.printProtocol
        
        android.util.Log.d("PrintDebug", "PrintService.printTestPage() - Protocol: $protocol")
        android.util.Log.d("PrintDebug", "Label size: ${settings.labelWidth}×${settings.labelHeight}mm")
        
        val data = TestPageGenerator.generate(
            protocol = protocol, 
            deviceName = deviceName, 
            density = settings.printDensity, 
            speed = settings.printSpeed, 
            gapMm = settings.gapMm,
            labelWidthMm = settings.labelWidth,
            labelHeightMm = settings.labelHeight
        )
        
        android.util.Log.d("PrintDebug", "Generated data size: ${data.size} bytes")
        android.util.Log.d("PrintDebug", "Commands:\n${data.toString(Charsets.UTF_8)}")
        
        return bluetoothRepository.write(data)
    }

    suspend fun getCurrentProtocol(): PrintProtocol {
        return settingsDataStore.settingsFlow.first().printProtocol
    }
}
