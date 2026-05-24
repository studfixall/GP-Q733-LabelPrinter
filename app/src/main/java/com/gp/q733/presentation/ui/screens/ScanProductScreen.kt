package com.gp.q733.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gp.q733.presentation.viewmodel.ScanProductUiState
import com.gp.q733.presentation.viewmodel.ScanProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanProductScreen(
    viewModel: ScanProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String, Float?, Float?) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // 自动聚焦到扫码输入框
    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫码打印") },
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
            // ===== 扫码输入栏 =====
            var barcodeInput by remember { mutableStateOf("") }
            OutlinedTextField(
                value = barcodeInput,
                onValueChange = { barcodeInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("扫描条码或手动输入") },
                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = {
                        if (barcodeInput.isNotBlank()) {
                            viewModel.onBarcodeScanned(barcodeInput.trim())
                        }
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "查询")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (barcodeInput.isNotBlank()) {
                        viewModel.onBarcodeScanned(barcodeInput.trim())
                    }
                })
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 加载中 =====
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // ===== 商品信息展示 =====
            if (uiState.productInfo.name.isNotBlank() && !uiState.showProductDialog) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.productExistsInDb)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.productExistsInDb) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (uiState.productExistsInDb) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.productExistsInDb) "商品已入库" else "商品信息",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("条码: ${uiState.productInfo.barcode}", style = MaterialTheme.typography.bodyMedium)
                        Text("名称: ${uiState.productInfo.name}", style = MaterialTheme.typography.bodyLarge)
                        if (uiState.productInfo.price > 0) {
                            Text("价格: ¥${String.format("%.2f", uiState.productInfo.price)}", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (uiState.productInfo.spec.isNotBlank()) {
                            Text("规格: ${uiState.productInfo.spec}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===== 模板选择 =====
                Text("选择打印模板", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.templates) { template ->
                        val isSelected = template.id == uiState.selectedTemplateId
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectTemplate(template.id) },
                            label = { Text(template.name, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===== 打印按钮 =====
                Button(
                    onClick = { viewModel.print() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !uiState.isPrinting && uiState.selectedTemplateId.isNotBlank()
                ) {
                    if (uiState.isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打印中...")
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打印")
                    }
                }
            }

            // ===== 未扫码时的提示 =====
            if (uiState.productInfo.name.isBlank() && !uiState.isLoading && !uiState.showProductDialog) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("扫描商品条码开始打印", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ===== 消息提示 =====
            uiState.errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Snackbar(
                    action = { TextButton(onClick = { viewModel.clearMessages() }) { Text("关闭") } }
                ) { Text(msg) }
            }
            uiState.successMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Snackbar(
                    action = { TextButton(onClick = { viewModel.clearMessages() }) { Text("关闭") } }
                ) { Text(msg) }
            }
        }

        // ===== 维护商品资料弹窗 =====
        if (uiState.showProductDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissProductDialog() },
                title = { Text("商品未入库") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("条码: ${uiState.productInfo.barcode}", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = uiState.dialogName,
                            onValueChange = { viewModel.updateDialogName(it) },
                            label = { Text("商品名称") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.dialogPrice,
                            onValueChange = { viewModel.updateDialogPrice(it) },
                            label = { Text("价格") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.saveProductFromDialog() }) {
                        Text("保存并继续")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissProductDialog() }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
