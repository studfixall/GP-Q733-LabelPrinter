package com.gp.q733.presentation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.presentation.viewmodel.LabelTemplate
import com.gp.q733.presentation.viewmodel.ScanProductUiState
import com.gp.q733.presentation.viewmodel.ScanProductViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanProductScreen(
    viewModel: ScanProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (labelId: String, width: Float?, height: Float?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val context = LocalContext.current
    
    // 扫码对话框状态
    var showScanDialog by remember { mutableStateOf(false) }
    var manualBarcode by remember { mutableStateOf("") }
    var showTemplateSelector by remember { mutableStateOf(false) }
    
    // 获取可用的商品模板
    val productTemplates = viewModel.getProductTemplates()
    
    // ZXing 扫码启动器
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            // 扫码成功，查询商品
            viewModel.onManualBarcodeEntered(result.contents)
        }
    }
    
    // 相机权限请求启动器
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限 granted，启动扫码
            val options = ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                .setPrompt("将条码对准扫描框")
                .setCameraId(0)
                .setBeepEnabled(true)
                .setBarcodeImageEnabled(false)
            scanLauncher.launch(options)
        } else {
            // 权限被拒绝
            viewModel.setError("需要相机权限才能扫码")
        }
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
                    
                    // 手动输入条码
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        label = { Text("商品条码") },
                        placeholder = { Text("例如: 6901234567890") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (manualBarcode.isNotBlank()) {
                                    viewModel.onManualBarcodeEntered(manualBarcode)
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // 请求相机权限并启动扫码
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("扫码")
                        }
                        
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
                    }
                }
            }
            
            // 加载状态
            if (uiState.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                            onClick = {
                                viewModel.fillTemplateAndPreview(template)
                            }
                        )
                    }
                }
            }
            
            // 填充后的预览
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
                        
                        // 简单的预览信息
                        val textElements = label.elements.filterIsInstance<LabelElement.Text>()
                        textElements.forEach { element ->
                            val style = if (element.isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
                            Text(
                                text = element.text,
                                style = style
                            )
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
                            Button(
                                onClick = { viewModel.reset() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("重新扫码")
                            }
                            
                            Button(
                                onClick = { viewModel.printFilledLabel() },
                                modifier = Modifier.weight(1f),
                                enabled = connectionState == ConnectionState.Connected
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("打印")
                            }
                        }
                    }
                }
            }
            
            // 连接状态提示
            if (connectionState == ConnectionState.Disconnected) {
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
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null)
                        Text("请先连接蓝牙打印机")
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("价格:", style = MaterialTheme.typography.bodyMedium)
                Text("¥${String.format("%.2f", product.price)}", style = MaterialTheme.typography.titleLarge)
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
