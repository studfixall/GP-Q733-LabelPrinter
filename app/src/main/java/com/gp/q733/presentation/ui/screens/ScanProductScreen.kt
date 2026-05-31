package com.gp.q733.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.Label
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.BoxWithConstraints
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.repository.ConnectionState
import com.gp.q733.presentation.viewmodel.ScanTemplateOption
import com.gp.q733.presentation.viewmodel.ScanProductUiState
import com.gp.q733.presentation.viewmodel.ScanProductViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanProductScreen(
    viewModel: ScanProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String, Float?, Float?) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

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
                .setPrompt("将条码对准扫描框")
                .setCameraId(0)
                .setBeepEnabled(true)
                .setBarcodeImageEnabled(false)
            scanLauncher.launch(options)
        } // 权限被拒则静默
    }

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

            // ===== 扫码按钮 =====
            Button(
                onClick = {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("扫描条码")
            }

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
            TemplateCardWithPreview(
                template = template,
                isSelected = isSelected,
                onClick = { viewModel.selectTemplate(template.id) }
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

        // ===== 未找到商品提示弹窗 =====
        if (uiState.showNotFoundDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissNotFound() },
                title = { Text("未查到商品") },
                text = { Text("条码 ${uiState.productInfo.barcode} 在商品库中未找到，是否维护商品信息？") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmNotFound() }) { Text("去维护") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissNotFound() }) { Text("取消") }
                }
            )
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

/**
 * 模板卡片+缩略预览
 */
@Composable
fun TemplateCardWithPreview(
    template: ScanTemplateOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 缩略预览
            LabelThumbnail(
                label = template.label,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = template.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已选中",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 标签缩略图渲染
 */
@Composable
fun LabelThumbnail(
    label: Label,
    modifier: Modifier = Modifier
) {
    val aspectRatio = label.widthMm / label.heightMm

    BoxWithConstraints(modifier = modifier) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = if (constraints.maxHeight > 0) constraints.maxHeight.toFloat() else boxWidth / aspectRatio
        val displayW = boxWidth
        val displayH = minOf(boxHeight, boxWidth / aspectRatio)
        val labelW = label.widthMm
        val labelH = label.heightMm
        val scaleX = displayW / labelW
        val scaleY = displayH / labelH

        Canvas(modifier = Modifier.size(
            with(LocalDensity.current) { displayW.toDp() },
            with(LocalDensity.current) { displayH.toDp() }
        )) {
            drawRect(color = Color.White)
            // 简单边框
            drawLine(color = Color.LightGray, start = Offset.Zero, end = Offset(displayW, 0f), strokeWidth = 1f)
            drawLine(color = Color.LightGray, start = Offset.Zero, end = Offset(0f, displayH), strokeWidth = 1f)
            drawLine(color = Color.LightGray, start = Offset(displayW, 0f), end = Offset(displayW, displayH), strokeWidth = 1f)
            drawLine(color = Color.LightGray, start = Offset(0f, displayH), end = Offset(displayW, displayH), strokeWidth = 1f)
            drawRect(color = Color.White)

            label.elements.forEach { element ->
                when (element) {
                    is LabelElement.Text -> {
                        val x = element.x * scaleX
                        val y = element.y * scaleY
                        val fontSize = (element.fontSize * minOf(scaleX, scaleY) * 0.8f).coerceAtLeast(4f)
                        drawContext.canvas.nativeCanvas.drawText(
                            element.text,
                            x, y + fontSize,
                            android.graphics.Paint().apply {
                                textSize = fontSize
                                color = android.graphics.Color.BLACK
                                isAntiAlias = true
                                if (element.isBold) isFakeBoldText = true
                            }
                        )
                    }
                    is LabelElement.Barcode -> {
                        val x = element.x * scaleX
                        val y = element.y * scaleY
                        val w = if (element.widthMm > 0f) element.widthMm * scaleX else labelW * 0.8f * scaleX
                        val h = element.height * scaleY
                        // 简化条码显示
                        val barCount = element.content.length.coerceAtMost(20)
                        val barW = w / (barCount * 2)
                        for (i in 0 until barCount) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(x + i * barW * 2, y),
                                size = androidx.compose.ui.geometry.Size(barW, h * 0.7f)
                            )
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            element.content.take(12),
                            x, y + h,
                            android.graphics.Paint().apply {
                                textSize = 6f * minOf(scaleX, scaleY)
                                color = android.graphics.Color.BLACK
                                isAntiAlias = true
                            }
                        )
                    }
                    is LabelElement.QRCode -> {
                        val x = element.x * scaleX
                        val y = element.y * scaleY
                        val size = element.size * minOf(scaleX, scaleY)
                        val cellSize = size / 21
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            style = android.graphics.Paint.Style.FILL
                        }
                        // 三个定位符
                        for (corner in listOf(0f to 0f, 14f * cellSize to 0f, 0f to 14f * cellSize)) {
                            for (row in 0..6) {
                                for (col in 0..6) {
                                    val isOuter = row == 0 || row == 6 || col == 0 || col == 6
                                    val isInner = row in 2..4 && col in 2..4
                                    if (isOuter || isInner) {
                                        drawContext.canvas.nativeCanvas.drawRect(
                                            x + corner.first + col * cellSize,
                                            y + corner.second + row * cellSize,
                                            x + corner.first + (col + 1) * cellSize,
                                            y + corner.second + (row + 1) * cellSize,
                                            paint
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is LabelElement.Line -> {
                        val x = element.x * scaleX
                        val y = element.y * scaleY
                        val w = element.width * scaleX
                        val h = element.height * scaleY
                        drawLine(
                            color = Color.Black,
                            start = Offset(x, y),
                            end = Offset(x + w, y + h),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }
}
