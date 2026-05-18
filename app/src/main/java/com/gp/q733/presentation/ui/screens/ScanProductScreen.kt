package com.gp.q733.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.domain.model.LabelTemplate
import com.gp.q733.presentation.viewmodel.PrintStatus
import com.gp.q733.presentation.viewmodel.ScanProductUiState
import com.gp.q733.presentation.viewmodel.ScanProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanProductScreen(
    viewModel: ScanProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (labelId: String, width: Float?, height: Float?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var manualBarcode by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val productTemplates = viewModel.getProductTemplates()

    // 打印成功后2秒自动重置，准备下次扫码
    LaunchedEffect(uiState.printStatus) {
        if (uiState.printStatus == PrintStatus.Success) {
            kotlinx.coroutines.delay(2000)
            viewModel.reset()
            manualBarcode = ""
            // 重新聚焦输入框，方便扫码枪连续扫码
            focusRequester.requestFocus()
        }
    }

    // 页面加载时自动聚焦输入框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫码商品打印") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 扫码/输入区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "扫描或输入商品条码",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "将光标置于输入框，扫码枪扫码后自动查询",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        label = { Text("商品条码") },
                        placeholder = { Text("扫码枪扫描或手动输入") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (manualBarcode.isNotBlank()) {
                                    viewModel.onManualBarcodeEntered(manualBarcode)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (manualBarcode.isNotBlank()) {
                                    viewModel.onManualBarcodeEntered(manualBarcode)
                                }
                            },
                            enabled = manualBarcode.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("查询")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.reset()
                                manualBarcode = ""
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("清空")
                        }
                    }
                }
            }

            // 加载状态
            if (uiState.isLoading) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("查询商品信息中...")
                    }
                }
            }

            // 错误信息
            uiState.error?.let { error ->
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null)
                        Text(error)
                    }
                }
            }

            // 商品信息显示
            uiState.product?.let { product ->
                ProductInfoCard(product)

                // 模板选择
                if (uiState.showFillPreview) {
                    Text(
                        text = "选择标签模板",
                        style = MaterialTheme.typography.titleMedium
                    )
                    productTemplates.forEach { template ->
                        TemplateCard(
                            template = template,
                            onClick = { viewModel.fillTemplateAndPreview(template) }
                        )
                    }
                }
            }

            // 填充后的预览 + 打印
            uiState.filledLabel?.let { label ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "预览",
                            style = MaterialTheme.typography.titleMedium
                        )

                        val textElements = label.elements.filterIsInstance<LabelElement.Text>()
                        textElements.forEach { element ->
                            val style = if (element.isBold)
                                MaterialTheme.typography.titleMedium
                            else
                                MaterialTheme.typography.bodyLarge
                            Text(text = element.text, style = style)
                        }

                        val barcodeElements = label.elements.filterIsInstance<LabelElement.Barcode>()
                        barcodeElements.forEach { element ->
                            Text(
                                text = "条码: ${element.content}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.reset()
                                    manualBarcode = ""
                                    focusRequester.requestFocus()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("重新扫码")
                            }
                            Button(
                                onClick = { viewModel.printFilledLabel() },
                                modifier = Modifier.weight(1f),
                                enabled = uiState.printStatus != PrintStatus.Printing
                            ) {
                                if (uiState.printStatus == PrintStatus.Printing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("打印中...")
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("打印")
                                }
                            }
                        }

                        // 打印状态反馈
                        uiState.printMessage?.let { msg ->
                            val isSuccess = uiState.printStatus == PrintStatus.Success
                            val isError = uiState.printStatus == PrintStatus.Failed
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        isSuccess -> Icons.Default.CheckCircle
                                        isError -> Icons.Default.Error
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isSuccess -> MaterialTheme.colorScheme.primary
                                        isError -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = msg,
                                    color = when {
                                        isSuccess -> MaterialTheme.colorScheme.primary
                                        isError -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // 连接状态提示（仅在未连接且无打印操作时显示）
            if (connectionState == ConnectionState.Disconnected && uiState.printStatus == PrintStatus.Idle) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null)
                        Text("未连接打印机（点击打印时会自动重连）")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductInfoCard(product: ProductInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "商品信息",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("名称:", style = MaterialTheme.typography.bodyMedium)
                Text(product.name, style = MaterialTheme.typography.bodyLarge)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("条码:", style = MaterialTheme.typography.bodyMedium)
                Text(product.barcode, style = MaterialTheme.typography.bodyLarge)
            }
            product.spec?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("规格:", style = MaterialTheme.typography.bodyMedium)
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("价格:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "¥${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(template: LabelTemplate, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "选择"
            )
        }
    }
}
