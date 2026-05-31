package com.gp.q733.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.gp.q733.domain.model.PrinterDevice
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BluetoothRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override fun getConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<PrinterDevice>>(emptyList())
    override fun getDiscoveredDevices(): Flow<List<PrinterDevice>> = _discoveredDevices.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    override fun getConnectedDeviceFlow(): StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()
    override fun getConnectedDevice(): BluetoothDevice? = _connectedDevice.value

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        if (bluetoothAdapter == null) {
            try {
                val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                bluetoothAdapter = manager?.adapter
            } catch (e: Exception) { /* Ignore */ }
        }
        return bluetoothAdapter
    }

    private var connectedSocket: BluetoothSocket? = null

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    @Suppress("DEPRECATION")
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        if (it.name != null) {
                            val printer = PrinterDevice(
                                device = it,
                                name = it.name ?: "Unknown",
                                address = it.address
                            )
                            val current = _discoveredDevices.value.toMutableList()
                            if (current.none { d -> d.address == printer.address }) {
                                current.add(printer)
                                _discoveredDevices.value = current
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> { /* Scan completed */ }
            }
        }
    }
    private var isReceiverRegistered = false

    @SuppressLint("MissingPermission")
    override fun startScan() {
        val adapter = getBluetoothAdapter() ?: return
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(discoveryReceiver, filter)
            isReceiverRegistered = true
        }
        _discoveredDevices.value = emptyList()
        if (adapter.isDiscovering) { adapter.cancelDiscovery() }
        adapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        getBluetoothAdapter()?.cancelDiscovery()
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(device: PrinterDevice): Result<Unit> {
        return try {
            val adapter = getBluetoothAdapter()
                ?: return Result.failure(Exception("Bluetooth not available"))

            _connectionState.value = ConnectionState.Connecting
            adapter.cancelDiscovery()
            val btDevice: BluetoothDevice = adapter.getRemoteDevice(device.address)
            val socket = btDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            connectedSocket = socket
            _connectedDevice.value = btDevice
            _connectionState.value = ConnectionState.Connected
            Result.success(Unit)
        } catch (e: IOException) {
            _connectionState.value = ConnectionState.Error
            _connectedDevice.value = null
            Result.failure(Exception(e.message ?: "Connection failed"))
        }
    }

    override suspend fun disconnect() {
        try { connectedSocket?.close() } catch (_: IOException) { }
        connectedSocket = null
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun write(data: ByteArray): Result<Unit> {
        val socket = connectedSocket
        if (socket == null || !socket.isConnected) {
            return Result.failure(Exception("Not connected"))
        }
        android.util.Log.d("PrintDebug", "BluetoothRepository.write() - ${data.size} bytes to ${_connectedDevice.value?.name}")
        return try {
            val chunkSize = 1024
            var offset = 0
            while (offset < data.size) {
                val end = minOf(offset + chunkSize, data.size)
                socket.outputStream.write(data, offset, end - offset)
                socket.outputStream.flush()
                offset = end
                kotlinx.coroutines.delay(10)
            }
            Result.success(Unit)
        } catch (e: IOException) {
            _connectionState.value = ConnectionState.Error
            Result.failure(Exception("Write failed: ${e.message}"))
        }
    }

    fun cleanup() {
        if (isReceiverRegistered) {
            try { context.unregisterReceiver(discoveryReceiver) } catch (_: Exception) { }
            isReceiverRegistered = false
        }
        try { connectedSocket?.close() } catch (_: IOException) { }
        connectedSocket = null
    }
}
