package com.gp.q733.domain.repository

import android.bluetooth.BluetoothDevice
import com.gp.q733.domain.model.PrinterDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothRepository {
    fun startScan()
    fun stopScan()
    fun getDiscoveredDevices(): Flow<List<PrinterDevice>>
    suspend fun connect(device: PrinterDevice): Result<Unit>
    suspend fun disconnect()
    fun getConnectionState(): Flow<ConnectionState>
    suspend fun write(data: ByteArray): Result<Unit>
    fun getConnectedDevice(): BluetoothDevice?
    fun getConnectedDeviceFlow(): StateFlow<BluetoothDevice?>
}

enum class ConnectionState {
    Disconnected, Connecting, Connected, Error
}
