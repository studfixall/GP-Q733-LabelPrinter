package com.gp.q733.presentation.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gp.q733.domain.print.PaperType
import com.gp.q733.domain.print.PrintProtocol
import com.gp.q733.presentation.viewmodel.SettingsViewModel
import com.gp.q733.presentation.viewmodel.StoreInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var labelWidthText by remember { mutableStateOf("50") }
    var labelHeightText by remember { mutableStateOf("30") }
    var gapMmText by remember { mutableStateOf("2") }
    var blackMarkOffsetText by remember { mutableStateOf("0") }
    var printOffsetXText by remember { mutableStateOf("0") }
    var printOffsetYText by remember { mutableStateOf("0") }
    var showProtocolMenu by remember { mutableStateOf(false) }
    var showPaperTypeMenu by remember { mutableStateOf(false) }
    var showDensityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
var showStorePicker by remember { mutableStateOf(false) }

    // Sync initial values
    LaunchedEffect(uiState.labelWidth) { labelWidthText = uiState.labelWidth.toInt().toString() }
    LaunchedEffect(uiState.labelHeight) { labelHeightText = uiState.labelHeight.toInt().toString() }
    LaunchedEffect(uiState.gapMm) { gapMmText = uiState.gapMm.toInt().toString() }
    LaunchedEffect(uiState.blackMarkOffset) { blackMarkOffsetText = uiState.blackMarkOffset.toInt().toString() }
        LaunchedEffect(uiState.printOffsetX) { printOffsetXText = if (uiState.printOffsetX < 0) uiState.printOffsetX.toString() else uiState.printOffsetX.toInt().toString() }
        LaunchedEffect(uiState.printOffsetY) { printOffsetYText = if (uiState.printOffsetY < 0) uiState.printOffsetY.toString() else uiState.printOffsetY.toInt().toString() }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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

            // Paper Type Settings
            Text(
                text = "纸张设置",
                style = MaterialTheme.typography.titleMedium
            )
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Paper Type Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "纸张类型",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            Button(
                                onClick = { showPaperTypeMenu = true }
                            ) {
                                Text(
                                    when (uiState.paperType) {
                                        PaperType.LABEL -> "标签纸"
                                        PaperType.BLACK_MARK -> "黑标纸"
                                        PaperType.RECEIPT -> "票据纸"
                                    }
                                )
                            }
                            DropdownMenu(
                                expanded = showPaperTypeMenu,
                                onDismissRequest = { showPaperTypeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("标签纸（带间隙）") },
                                    onClick = {
                                        viewModel.updatePaperType(PaperType.LABEL)
                                        showPaperTypeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("黑标纸（带黑色标记）") },
                                    onClick = {
                                        viewModel.updatePaperType(PaperType.BLACK_MARK)
                                        showPaperTypeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("票据纸（连续纸）") },
                                    onClick = {
                                        viewModel.updatePaperType(PaperType.RECEIPT)
                                        showPaperTypeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Paper type description
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (uiState.paperType) {
                            PaperType.LABEL -> "间隙传感器检纸，需要设置标签间距"
                            PaperType.BLACK_MARK -> "黑标传感器检纸，需要设置黑标偏移"
                            PaperType.RECEIPT -> "连续走纸，无间隙无标记"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Gap setting (only for LABEL)
                    if (uiState.paperType == PaperType.LABEL) {
                        OutlinedTextField(
                            value = gapMmText,
                            onValueChange = { text ->
                                gapMmText = text
                                text.toFloatOrNull()?.let { viewModel.updateGapMm(it) }
                            },
                            label = { Text("标签间距 (mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Black mark offset (only for BLACK_MARK)
                    if (uiState.paperType == PaperType.BLACK_MARK) {
                        OutlinedTextField(
                            value = blackMarkOffsetText,
                            onValueChange = { text ->
                                blackMarkOffsetText = text
                                text.toFloatOrNull()?.let { viewModel.updateBlackMarkOffset(it) }
                            },
                            label = { Text("黑标偏移 (mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = gapMmText,
                            onValueChange = { text ->
                                gapMmText = text
                                text.toFloatOrNull()?.let { viewModel.updateGapMm(it) }
                            },
                            label = { Text("黑标间距 (mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Print offset
                OutlinedTextField(
                    value = printOffsetXText,
                    onValueChange = { text ->
                        // 允许负数输入（如-2表示向左偏移）
                        val filtered = text.replace(Regex("[^0-9\\-.]"), "")
                        printOffsetXText = filtered
                        filtered.toFloatOrNull()?.let { viewModel.updatePrintOffsetX(it) }
                        text.toFloatOrNull()?.let { viewModel.updatePrintOffsetX(it) }
                    },
                    label = { Text("水平偏移 (mm, 负数=向左)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = printOffsetYText,
                    onValueChange = { text ->
                        val filtered = text.replace(Regex("[^0-9\\-.]"), "")
                        printOffsetYText = filtered
                        filtered.toFloatOrNull()?.let { viewModel.updatePrintOffsetY(it) }
                        text.toFloatOrNull()?.let { viewModel.updatePrintOffsetY(it) }
                    },
                    label = { Text("垂直偏移 (mm, 负数=向上)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
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
                    HorizontalDivider()
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
                            ) { Icon(Icons.Default.Remove, contentDescription = "减少") }
                            Text(
                                text = uiState.printDensity.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (uiState.printDensity < 15) {
                                        viewModel.updatePrintDensity(uiState.printDensity + 1)
                                    }
                                },
                                enabled = uiState.printDensity < 15
                            ) { Icon(Icons.Default.Add, contentDescription = "增加") }
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
                            ) { Icon(Icons.Default.Remove, contentDescription = "减少") }
                            Text(
                                text = uiState.printSpeed.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (uiState.printSpeed < 10) {
                                        viewModel.updatePrintSpeed(uiState.printSpeed + 1)
                                    }
                                },
                                enabled = uiState.printSpeed < 10
                            ) { Icon(Icons.Default.Add, contentDescription = "增加") }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (uiState.printCopies > 1) {
                                        viewModel.updatePrintCopies(uiState.printCopies - 1)
                                    }
                                },
                                enabled = uiState.printCopies > 1
                            ) { Icon(Icons.Default.Remove, contentDescription = "减少") }
                            Text(
                                text = uiState.printCopies.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (uiState.printCopies < 99) {
                                        viewModel.updatePrintCopies(uiState.printCopies + 1)
                                    }
                                },
                                enabled = uiState.printCopies < 99
                            ) { Icon(Icons.Default.Add, contentDescription = "增加") }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (uiState.reconnectInterval > 1) {
                                        viewModel.updateReconnectInterval(uiState.reconnectInterval - 1)
                                    }
                                },
                                enabled = uiState.reconnectInterval > 1
                            ) { Icon(Icons.Default.Remove, contentDescription = "减少") }
                            Text(
                                text = "${uiState.reconnectInterval}s",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (uiState.reconnectInterval < 60) {
                                        viewModel.updateReconnectInterval(uiState.reconnectInterval + 1)
                                    }
                                },
                                enabled = uiState.reconnectInterval < 60
                            ) { Icon(Icons.Default.Add, contentDescription = "增加") }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Store & RMIS Settings — Issue #13
            Text(
                text = "门店设置",
                style = MaterialTheme.typography.titleMedium
            )
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // RMIS Base URL
                    OutlinedTextField(
                        value = uiState.rmisBaseUrl,
                        onValueChange = { viewModel.updateRmisBaseUrl(it) },
                        label = { Text("RMIS 服务地址") },
                        placeholder = { Text("例: http://192.168.1.100:8777/Enjoy") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // RMIS UserNo
                    OutlinedTextField(
                        value = uiState.rmisUserNo,
                        onValueChange = { viewModel.updateRmisUserNo(it) },
                        label = { Text("应用程序编码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // RMIS Master Key
                    OutlinedTextField(
                        value = uiState.rmisMasterKey,
                        onValueChange = { viewModel.updateRmisMasterKey(it) },
                        label = { Text("主密钥") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Current Store Display + Change Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "当前门店",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (uiState.storeId.isNotBlank())
                                    "${uiState.storeName} (${uiState.storeId})"
                                else "未选择",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.storeId.isNotBlank())
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.showStorePicker() },
                            enabled = uiState.rmisBaseUrl.isNotBlank() && uiState.rmisUserNo.isNotBlank()
                        ) {
                            Text("选择门店")
                        }
                    }
                    // Store loading indicator
                    if (uiState.isLoadingStores) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    // Store load error
                    if (uiState.storeLoadError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.storeLoadError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Store Picker Dialog
            if (uiState.showStorePicker) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissStorePicker() },
                    title = { Text("选择门店") },
                    text = {
                        if (uiState.storeList.isEmpty() && !uiState.isLoadingStores) {
                            Text("未找到门店，请检查RMIS配置")
                        } else {
                            LazyColumn {
                                items(uiState.storeList) { store ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectStore(store) }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (store.id == uiState.storeId) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "当前",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Column {
                                            Text(store.name, style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                "编码: ${store.id}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissStorePicker() }) {
                            Text("取消")
                        }
                    }
                )
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("纸张类型")
                        Text(
                            text = when (uiState.paperType) {
                                PaperType.LABEL -> "标签纸"
                                PaperType.BLACK_MARK -> "黑标纸"
                                PaperType.RECEIPT -> "票据纸"
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
