package com.gp.q733.ui.template

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.util.BarsoftFieldName
import com.gp.q733.domain.util.BarsoftTemplateParser

/**
 * 模板打印界面
 * 选模板后 → 填数据 → 预览 → 打印
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePrintScreen(
    viewModel: com.gp.q733.ui.template.TemplatePrintViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模板打印") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Label Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "标签预览 (${uiState.label?.widthMm?.toInt() ?: 0}×${uiState.label?.heightMm?.toInt() ?: 0}mm)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LabelPreview(
                        label = uiState.filledLabel ?: uiState.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 250.dp)
                    )
                }
            }

            // Data Input Fields
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("数据填充", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    val fieldHints = uiState.fieldHints
                    val hasBarcode = fieldHints.containsKey("barcode")

                    // Barcode field first (most important for scanning)
                    if (hasBarcode) {
                        OutlinedTextField(
                            value = uiState.productBarcode,
                            onValueChange = viewModel::updateProductBarcode,
                            label = { Text(fieldHints["barcode"] ?: "条码") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Other fields based on template hints
                    if (fieldHints.containsKey("name")) {
                        OutlinedTextField(
                            value = uiState.productName,
                            onValueChange = viewModel::updateProductName,
                            label = { Text(fieldHints["name"] ?: "品名") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (fieldHints.containsKey("price")) {
                        OutlinedTextField(
                            value = uiState.productPrice,
                            onValueChange = viewModel::updateProductPrice,
                            label = { Text(fieldHints["price"] ?: "价格") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (fieldHints.containsKey("mprice")) {
                        OutlinedTextField(
                            value = uiState.productMprice,
                            onValueChange = viewModel::updateProductMprice,
                            label = { Text(fieldHints["mprice"] ?: "会员价") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (fieldHints.containsKey("spec")) {
                        OutlinedTextField(
                            value = uiState.productSpec,
                            onValueChange = viewModel::updateProductSpec,
                            label = { Text(fieldHints["spec"] ?: "规格") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (fieldHints.containsKey("unit")) {
                        OutlinedTextField(
                            value = uiState.productUnit,
                            onValueChange = viewModel::updateProductUnit,
                            label = { Text(fieldHints["unit"] ?: "单位") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (fieldHints.containsKey("area")) {
                        OutlinedTextField(
                            value = uiState.productOrigin,
                            onValueChange = viewModel::updateProductOrigin,
                            label = { Text(fieldHints["area"] ?: "产地") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Product Picker Button
                    OutlinedButton(
                        onClick = { viewModel.toggleProductPicker(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("从商品库选择")
                    }
                }
            }

            // 打印份数
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("打印份数", style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.updatePrintCopies(uiState.printCopies - 1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "减少")
                    }
                    Text(
                        "${uiState.printCopies}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(onClick = { viewModel.updatePrintCopies(uiState.printCopies + 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "增加")
                    }
                }
            }

            // Print Button
            Button(
                onClick = viewModel::print,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                enabled = !uiState.isPrinting && uiState.filledLabel != null
            ) {
                if (uiState.isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("打印中...")
                } else {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CPCL 打印")
                }
            }

            // Print Result
            when (uiState.printResult) {
                "success" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Text(
                            "✅ 打印成功",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                null -> {}
                else -> {
                    val errorMsg = uiState.printResult?.removePrefix("error:") ?: "未知错误"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            "❌ 打印失败: $errorMsg",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Product Picker Dialog
    if (uiState.showProductPicker) {
        ProductPickerDialog(
            products = uiState.productSearchResults,
            searchQuery = uiState.productSearchQuery,
            onSearch = viewModel::searchProducts,
            onSelect = viewModel::fillFromProduct,
            onDismiss = { viewModel.toggleProductPicker(false) }
        )
    }

    // Auto-clear print result after 3 seconds
    if (uiState.printResult != null) {
        LaunchedEffect(uiState.printResult) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearPrintResult()
        }
    }
}

/**
 * Label Preview using Canvas
 * Renders text, barcode, QR code, and line elements scaled to fit
 */
@Composable
private fun LabelPreview(
    label: Label?,
    modifier: Modifier = Modifier
) {
    if (label == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("无预览", color = Color.Gray)
        }
        return
    }

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
            drawRect(color = Color.LightGray, style = Stroke(width = 1f))

            label.elements.forEach { element ->
                when (element) {
                    is LabelElement.Text -> {
                        val x = element.x * scaleX
                        val y = element.y * scaleY
                        val fontSize = (element.fontSize * minOf(scaleX, scaleY) * 0.8f).coerceAtLeast(8f)
                        drawContext.canvas.nativeCanvas.drawText(
                            element.text,
                            x, y + fontSize,
                            android.graphics.Paint().apply {
                                this.textSize = fontSize
                                this.color = android.graphics.Color.BLACK
                                this.isAntiAlias = true
                                if (element.isBold) this.isFakeBoldText = true
                            }
                        )
                    }
                    is LabelElement.Barcode -> {
                        val x = element.x * scaleX
                        val y = element.y * scaleY
                        val w = if (element.widthMm > 0f) element.widthMm * scaleX else labelW * 0.8f * scaleX
                        val h = element.height * scaleY
                        val barcodeH = h * 0.7f  // 70% for bars, 30% for text below
                        // Draw barcode bars (simplified CODE_128-like visualization)
                        drawBarcodeBars(
                            content = element.content,
                            x = x, y = y,
                            width = w, height = barcodeH
                        )
                        // Draw barcode text below
                        drawContext.canvas.nativeCanvas.drawText(
                            element.content,
                            x, y + h,
                            android.graphics.Paint().apply {
                                textSize = 8f * minOf(scaleX, scaleY)
                                color = android.graphics.Color.BLACK
                                isAntiAlias = true
                            }
                        )
                    }
                    is LabelElement.QRCode -> {
                        val x = element.x * scaleX
                        val y = element.y * scaleY
                        val size = element.size * minOf(scaleX, scaleY)
                        // Draw QR code pattern
                        drawQrPattern(
                            content = element.content,
                            x = x, y = y,
                            size = size
                        )
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

/**
 * Draw simplified barcode bars visualization
 * Uses real CODE_128 encoding for scannable barcodes
 */
private fun DrawScope.drawBarcodeBars(
    content: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float
) {
    if (content.isBlank()) return

    val modules = com.gp.q733.domain.util.Code128Encoder.encodeToModules(content)
    if (modules.isEmpty()) return

    val moduleWidth = width / modules.size
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.FILL
    }

    for (i in modules.indices) {
        if (modules[i]) {
            val barLeft = x + i * moduleWidth
            drawContext.canvas.nativeCanvas.drawRect(
                barLeft, y, barLeft + moduleWidth, y + height,
                paint
            )
        }
    }
}

/**
 * Draw simplified QR code pattern
 */
private fun DrawScope.drawQrPattern(
    content: String,
    x: Float,
    y: Float,
    size: Float
) {
    val modules = 21  // QR Version 1 = 21x21
    val cellSize = size / modules
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.FILL
    }

    // Draw finder patterns (3 corners)
    drawQrFinderPattern(x, y, cellSize, paint)                      // Top-left
    drawQrFinderPattern(x + (modules - 7) * cellSize, y, cellSize, paint)  // Top-right
    drawQrFinderPattern(x, y + (modules - 7) * cellSize, cellSize, paint)  // Bottom-left

    // Draw data pattern (deterministic from content)
    val contentHash = content.hashCode()
    for (row in 0 until modules) {
        for (col in 0 until modules) {
            // Skip finder pattern areas
            if ((row < 8 && col < 8) || (row < 8 && col >= modules - 8) || (row >= modules - 8 && col < 8)) continue

            val isDark = ((contentHash * 31 + row * 7 + col * 13) and 1) == 0
            if (isDark) {
                drawContext.canvas.nativeCanvas.drawRect(
                    x + col * cellSize,
                    y + row * cellSize,
                    x + (col + 1) * cellSize,
                    y + (row + 1) * cellSize,
                    paint
                )
            }
        }
    }
}

private fun DrawScope.drawQrFinderPattern(
    x: Float,
    y: Float,
    cellSize: Float,
    paint: android.graphics.Paint
) {
    // 7x7 finder pattern: outer border, inner 3x3
    for (row in 0..6) {
        for (col in 0..6) {
            val isOuter = row == 0 || row == 6 || col == 0 || col == 6
            val isInner = row in 2..4 && col in 2..4
            if (isOuter || isInner) {
                drawContext.canvas.nativeCanvas.drawRect(
                    x + col * cellSize,
                    y + row * cellSize,
                    x + (col + 1) * cellSize,
                    y + (row + 1) * cellSize,
                    paint
                )
            }
        }
    }
}

@Composable
private fun ProductPickerDialog(
    products: List<ProductInfo>,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onSelect: (ProductInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择商品") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearch,
                    label = { Text("搜索商品") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(products.size) { index ->
                        val product = products[index]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(product) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "¥${String.format("%.2f", product.price)} | ${product.spec} | ${product.barcode}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
