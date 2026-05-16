package com.gp.q733.domain.model

import android.bluetooth.BluetoothDevice

data class PrinterDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val signalStrength: Int = 0
) {
    val displayName: String
        get() = name.ifEmpty { "Unknown Device" }
}

enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Error
}

data class BluetoothConnection(
    val device: PrinterDevice,
    val state: ConnectionState,
    val lastError: String? = null
)