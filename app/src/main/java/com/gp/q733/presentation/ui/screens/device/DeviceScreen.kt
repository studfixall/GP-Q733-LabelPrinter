package com.gp.q733.presentation.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gp.q733.domain.model.PrinterDevice
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.presentation.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("连接设备") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Scan Button
            Button(
                onClick = {
                    if (uiState.isScanning) {
                        viewModel.stopScan()
                    } else {
                        viewModel.startScan()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionState != ConnectionState.Connecting
            ) {
                if (uiState.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("扫描中...")
                } else {
                    Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("扫描设备")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection Status + Test Print
            if (connectionState == ConnectionState.Connected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "已连接到 ${uiState.selectedDevice?.name ?: "设备"}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.disconnect() }) {
                                Text("断开")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.printTestPage() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isPrinting
                        ) {
                            if (uiState.isPrinting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("打印中...")
                            } else {
                                Icon(Icons.Default.Print, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("测试打印")
                            }
                        }

                        uiState.printResult?.let { result ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (result.startsWith("打印失败")) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Device List
            Text(
                text = "可用设备",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (discoveredDevices.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.isScanning) "扫描中..." else "点击扫描查找设备",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discoveredDevices) { device ->
                        val isSelected = uiState.selectedDevice?.address == device.address
                        Card(
                            onClick = { viewModel.selectDevice(device) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (isSelected) {
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            } else {
                                CardDefaults.cardColors()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (connectionState != ConnectionState.Connecting) {
                                    Button(
                                        onClick = {
                                            // FIX: Pass device directly to connect() instead of
                                            // selectDevice + connect (which causes StateFlow race condition)
                                            viewModel.connect(device)
                                        }
                                    ) {
                                        Text("连接")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
