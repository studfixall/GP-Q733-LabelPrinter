package com.gp.q733.presentation.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gp.q733.domain.model.Label
import com.gp.q733.presentation.viewmodel.HomeViewModel
import com.gp.q733.presentation.viewmodel.PrinterStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDevice: () -> Unit,
    onNavigateToEditor: (templateId: String, width: Float?, height: Float?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToScanProduct: () -> Unit = {},
    onNavigateToProductManagement: () -> Unit = {},
    onNavigateToTemplateBrowser: () -> Unit = {},
    onEditLabel: (Label) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showNewLabelDialog by remember { mutableStateOf(false) }
    var labelWidth by remember { mutableStateOf("50") }
    var labelHeight by remember { mutableStateOf("30") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GP-Q733 标签打印") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "打印机状态",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (uiState.printerStatus) {
                                PrinterStatus.Connected -> Icons.Default.CheckCircle
                                PrinterStatus.Connecting -> Icons.Default.Sync
                                PrinterStatus.Disconnected -> Icons.Default.LinkOff
                                PrinterStatus.Error -> Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = when (uiState.printerStatus) {
                                PrinterStatus.Connected -> MaterialTheme.colorScheme.primary
                                PrinterStatus.Connecting -> MaterialTheme.colorScheme.tertiary
                                PrinterStatus.Disconnected -> MaterialTheme.colorScheme.outline
                                PrinterStatus.Error -> MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (uiState.printerStatus) {
                                PrinterStatus.Connected -> "已连接"
                                PrinterStatus.Connecting -> "连接中..."
                                PrinterStatus.Disconnected -> "未连接"
                                PrinterStatus.Error -> "连接错误"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Quick Actions - Row 1
            Text(
                text = "快捷操作",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bluetooth,
                    label = "连接设备",
                    onClick = onNavigateToDevice
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Add,
                    label = "新建标签",
                    onClick = { showNewLabelDialog = true }
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.QrCodeScanner,
                    label = "扫码打印",
                    onClick = onNavigateToScanProduct
                )
            }
            // Quick Actions - Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.GridView,
                    label = "模板库",
                    onClick = onNavigateToTemplateBrowser
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Inventory,
                    label = "商品管理",
                    onClick = onNavigateToProductManagement
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            // Saved Labels Section
            if (uiState.recentLabels.isNotEmpty()) {
                Text(
                    text = "已保存标签",
                    style = MaterialTheme.typography.titleMedium
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.recentLabels.take(5).forEach { label ->
                        SavedLabelCard(
                            label = label,
                            onClick = { onEditLabel(label) },
                            onDelete = { viewModel.deleteLabel(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Templates
            Text(
                text = "标签模板",
                style = MaterialTheme.typography.titleMedium
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.templates.forEach { template ->
                    TemplateCard(
                        name = template.name,
                        description = template.description,
                        icon = when (template.id) {
                            "express" -> Icons.Default.LocalShipping
                            "product" -> Icons.Default.Inventory
                            "price" -> Icons.Default.AttachMoney
                            else -> Icons.AutoMirrored.Filled.Label
                        },
                        onClick = { onNavigateToEditor(template.id, null, null) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Settings Button
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("设置")
            }
        }
    }

    // 新建标签对话框
    if (showNewLabelDialog) {
        AlertDialog(
            onDismissRequest = { showNewLabelDialog = false },
            title = { Text("新建标签") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("请设置标签尺寸（毫米）")
                    OutlinedTextField(
                        value = labelWidth,
                        onValueChange = { labelWidth = it.filter { c -> c.isDigit() } },
                        label = { Text("宽度 (mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        suffix = { Text("mm") }
                    )
                    OutlinedTextField(
                        value = labelHeight,
                        onValueChange = { labelHeight = it.filter { c -> c.isDigit() } },
                        label = { Text("高度 (mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        suffix = { Text("mm") }
                    )
                    Text(text = "常用尺寸：", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(onClick = { labelWidth = "30"; labelHeight = "20" }, label = { Text("30x20") })
                        SuggestionChip(onClick = { labelWidth = "40"; labelHeight = "30" }, label = { Text("40x30") })
                        SuggestionChip(onClick = { labelWidth = "50"; labelHeight = "30" }, label = { Text("50x30") })
                        SuggestionChip(onClick = { labelWidth = "60"; labelHeight = "40" }, label = { Text("60x40") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val width = labelWidth.toFloatOrNull() ?: 50f
                    val height = labelHeight.toFloatOrNull() ?: 30f
                    showNewLabelDialog = false
                    onNavigateToEditor("new", width.coerceIn(10f, 100f), height.coerceIn(10f, 100f))
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showNewLabelDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SavedLabelCard(
    modifier: Modifier = Modifier,
    label: Label,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "标签 ${label.widthMm.toInt()}x${label.heightMm.toInt()}mm", style = MaterialTheme.typography.titleSmall)
                Text(text = "${label.elements.size} 个元素", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TemplateCard(
    modifier: Modifier = Modifier,
    name: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleSmall)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    ElevatedButton(onClick = onClick, modifier = modifier.height(80.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
