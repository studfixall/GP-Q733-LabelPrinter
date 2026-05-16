package com.gp.q733.presentation.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gp.q733.domain.print.PrintProtocol
import com.gp.q733.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var labelWidthText by remember { mutableStateOf("50") }
    var labelHeightText by remember { mutableStateOf("30") }
    var showProtocolMenu by remember { mutableStateOf(false) }
    var showDensityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Sync initial values
    LaunchedEffect(uiState.labelWidth) {
        labelWidthText = uiState.labelWidth.toInt().toString()
    }
    LaunchedEffect(uiState.labelHeight) {
        labelHeightText = uiState.labelHeight.toInt().toString()
    }

    // Show save success snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("设置已保存")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveSettings() }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Label Settings
            Text(
                text = "标签设置",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = labelWidthText,
                        onValueChange = { text ->
                            labelWidthText = text
                            text.toFloatOrNull()?.let { viewModel.updateLabelWidth(it) }
                        },
                        label = { Text("标签宽度 (mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = labelHeightText,
                        onValueChange = { text ->
                            labelHeightText = text
                            text.toFloatOrNull()?.let { viewModel.updateLabelHeight(it) }
                        },
                        label = { Text("标签高度 (mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Print Protocol Settings
            Text(
                text = "打印协议",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Protocol Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "指令协议",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            Button(
                                onClick = { showProtocolMenu = true }
                            ) {
                                Text(
                                    when (uiState.printProtocol) {
                                        PrintProtocol.TSPL -> "TSPL"
                                        PrintProtocol.CPCL -> "CPCL"
                                        PrintProtocol.ESCPOS -> "ESC/POS"
                                    }
                                )
                            }
                            DropdownMenu(
                                expanded = showProtocolMenu,
                                onDismissRequest = { showProtocolMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("TSPL (佳博/ TSC)") },
                                    onClick = {
                                        viewModel.updatePrintProtocol(PrintProtocol.TSPL)
                                        showProtocolMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("CPCL (斑马)") },
                                    onClick = {
                                        viewModel.updatePrintProtocol(PrintProtocol.CPCL)
                                        showProtocolMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ESC/POS (票据机)") },
                                    onClick = {
                                        viewModel.updatePrintProtocol(PrintProtocol.ESCPOS)
                                        showProtocolMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Print Density
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "打印浓度",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (uiState.printDensity > 0) {
                                        viewModel.updatePrintDensity(uiState.printDensity - 1)
                                    }
                                },
                                enabled = uiState.printDensity > 0
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "减少")
                            }
                            Text(
                                text = uiState.printDensity.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.width(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (uiState.printDensity < 15) {
                                        viewModel.updatePrintDensity(uiState.printDensity + 1)
                                    }
                                },
                                enabled = uiState.printDensity < 15
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "增加")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Print Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "打印速度",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (uiState.printSpeed > 1) {
                                        viewModel.updatePrintSpeed(uiState.printSpeed - 1)
                                    }
                                },
                                enabled = uiState.printSpeed > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "减少")
                            }
                            Text(
                                text = uiState.printSpeed.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.width(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (uiState.printSpeed < 10) {
                                        viewModel.updatePrintSpeed(uiState.printSpeed + 1)
                                    }
                                },
                                enabled = uiState.printSpeed < 10
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "增加")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Print Settings
            Text(
                text = "打印设置",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "打印份数",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (uiState.printCopies > 1) {
                                        viewModel.updatePrintCopies(uiState.printCopies - 1)
                                    }
                                },
                                enabled = uiState.printCopies > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "减少")
                            }
                            Text(
                                text = uiState.printCopies.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.width(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (uiState.printCopies < 99) {
                                        viewModel.updatePrintCopies(uiState.printCopies + 1)
                                    }
                                },
                                enabled = uiState.printCopies < 99
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "增加")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection Settings
            Text(
                text = "连接设置",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "自动重连",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "打印失败时自动重连",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.autoReconnect,
                        onCheckedChange = { viewModel.updateAutoReconnect(it) }
                    )
                }
            }

            if (uiState.autoReconnect) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "重连间隔",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (uiState.reconnectInterval > 1) {
                                        viewModel.updateReconnectInterval(uiState.reconnectInterval - 1)
                                    }
                                },
                                enabled = uiState.reconnectInterval > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "减少")
                            }
                            Text(
                                text = "${uiState.reconnectInterval}s",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.width(48.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (uiState.reconnectInterval < 60) {
                                        viewModel.updateReconnectInterval(uiState.reconnectInterval + 1)
                                    }
                                },
                                enabled = uiState.reconnectInterval < 60
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "增加")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 命令预览 - 当前TSPL指令
            Text(
                text = "命令预览",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cmdPreview = buildString {
                    appendLine("SIZE ${uiState.labelWidth.toInt()} mm,${uiState.labelHeight.toInt()} mm")
                    appendLine("GAP 2 mm,0")
                    appendLine("DIRECTION 1")
                    appendLine("REFERENCE 0,0")
                    appendLine("CODEPAGE 936")
                    appendLine("DENSITY ${uiState.printDensity}")
                    appendLine("SPEED ${uiState.printSpeed}")
                    appendLine("CLS")
                    appendLine("...标签元素...")
                    appendLine("PRINT 1,1")
                    appendLine("END")
                }
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "当前设置的TSPL指令预览（GP-Q733）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = cmdPreview,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Section
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("版本")
                        Text(
                            text = "1.0.1",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("目标设备")
                        Text(
                            text = "GP-Q733",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("当前协议")
                        Text(
                            text = when (uiState.printProtocol) {
                                PrintProtocol.TSPL -> "TSPL"
                                PrintProtocol.CPCL -> "CPCL"
                                PrintProtocol.ESCPOS -> "ESC/POS"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Reset Button
            OutlinedButton(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("恢复默认设置")
            }
        }
    }
}
