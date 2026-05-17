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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.presentation.viewmodel.ScanProductUiState
import com.gp.q733.presentation.viewmodel.ScanProductViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanProductScreen(
    viewModel: ScanProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (labelId: String, width: Float?, height: Float?) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var manualBarcode by remember { mutableStateOf("") }

    // ZXing 扫码启动器
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            viewModel.onBarcodeScanned(result.contents)
        }
    }

    // 相机权限请求启动器
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val options = ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                .setPrompt("\u5c06\u6761\u7801\u5bf9\u51c6\u626b\u63cf\u6846")
                .setCameraId(0)
                .setBeepEnabled(true)
                .setBarcodeImageEnabled(false)
            scanLauncher.launch(options)
        } else {
            viewModel.setError("\u9700\u8981\u76f8\u673a\u6743\u9650\u624d\u80fd\u626b\u7801")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\u626b\u7801\u5546\u54c1\u6253\u5370") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "\u8fd4\u56de")
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
                        text = "\u626b\u63cf\u6216\u8f93\u5165\u5546\u54c1\u6761\u7801",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // 手动输入条码
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        label = { Text("\u5546\u54c1\u6761\u7801") },
                        placeholder = { Text("\u4f8b\u5982: 6901234567890") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (manualBarcode.isNotBlank()) {
                                    viewModel.onBarcodeScanned(manualBarcode)
                                    manualBarcode = ""
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
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("\u626b\u7801")
                        }
                        Button(
                            onClick = {
                                if (manualBarcode.isNotBlank()) {
                                    viewModel.onBarcodeScanned(manualBarcode)
                                    manualBarcode = ""
                                }
                            },
                            enabled = manualBarcode.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("\u67e5\u8be2")
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
                        Text("\u67e5\u8be2\u5546\u54c1\u4fe1\u606f\u4e2d...")
                    }
                }
            }

            // 错误信息
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null)
                        Text(error, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(Icons.Default.Close, contentDescription = "\u5173\u95ed")
                        }
                    }
                }
            }

            // 成功信息
            uiState.successMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text(msg, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(Icons.Default.Close, contentDescription = "\u5173\u95ed")
                        }
                    }
                }
            }

            // 商品信息显示
            val info = uiState.productInfo
            if (info.barcode.isNotBlank()) {
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
                            text = "\u5546\u54c1\u4fe1\u606f",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (info.name.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("\u540d\u79f0:", style = MaterialTheme.typography.bodyMedium)
                                Text(info.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("\u6761\u7801:", style = MaterialTheme.typography.bodyMedium)
                            Text(info.barcode, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (info.price.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("\u4ef7\u683c:", style = MaterialTheme.typography.bodyMedium)
                                Text("\u00a5${info.price}", style = MaterialTheme.typography.titleLarge)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 打印按钮
                        Button(
                            onClick = { viewModel.printFilledLabel() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isPrinting
                        ) {
                            if (uiState.isPrinting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("\u6253\u5370\u4e2d...")
                            } else {
                                Icon(Icons.Default.Print, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("\u6253\u5370\u6807\u7b7e")
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
                        Text("\u8bf7\u5148\u8fde\u63a5\u84dd\u7259\u6253\u5370\u673a")
                    }
                }
            }
        }
    }
}
