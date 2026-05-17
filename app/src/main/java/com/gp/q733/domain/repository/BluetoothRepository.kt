package com.gp.q733.domain.repository

import android.bluetooth.BluetoothDevice
import com.gp.q733.domain.model.PrinterDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothRepository {
    /**
     * Start scanning for Bluetooth devices
     */
    fun startScan()

    /**
     * Stop scanning for Bluetooth devices
     */
    fun stopScan()

    /**
     * Get flow of discovered devices
     */
    fun getDiscoveredDevices(): Flow<List<PrinterDevice>>

    /**
     * Connect to a specific device
     */
    suspend fun connect(device: PrinterDevice): Result<Unit>

    /**
     * Disconnect from current device
     */
    suspend fun disconnect()

    /**
     * Get flow of connection state
     */
    fun getConnectionState(): Flow<ConnectionState>

    /**
     * Write data to connected device
     */
    suspend fun write(data: ByteArray): Result<Unit>

    /**
     * Get currently connected BluetoothDevice (for SDK integration)
     */
    fun getConnectedDevice(): BluetoothDevice?

    /**
     * Get flow of currently connected BluetoothDevice
     */
    fun getConnectedDeviceFlow(): StateFlow<BluetoothDevice?>

    /**
     * Notify repository that connection state was changed externally (by SDK).
     * Used when GpPrinterService manages the connection instead of this repository.
     */
    fun notifyConnectionStateChanged(state: ConnectionState, device: BluetoothDevice? = null)

    /**
     * Notify repository that device was disconnected externally (by SDK).
     */
    fun notifyDisconnected()
}

enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Error
}
