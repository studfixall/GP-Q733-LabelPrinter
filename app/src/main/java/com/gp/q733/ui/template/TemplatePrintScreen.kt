package com.gp.q733.ui.template

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePrintScreen(
    viewModel: TemplatePrintViewModel,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.print() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = "打印")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 模板信息
            uiState.label?.let { label ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${label.widthMm.toInt()}×${label.heightMm.toInt()}mm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${label.elements.size}个元素",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { viewModel.toggleProductPicker(true) }) {
                            Text("从商品库选")
                        }
                    }
                }
            }

            // 数据输入区
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("商品数据", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    val fields = uiState.fieldHints
                    if (fields.containsKey("name") || true) {
                        FieldInput("品名", uiState.productName) { viewModel.updateProductName(it) }
                    }
                    if (fields.containsKey("barcode") || true) {
                        FieldInput("条码", uiState.productBarcode) { viewModel.updateProductBarcode(it) }
                    }
                    if (fields.containsKey("price") || true) {
                        FieldInput("价格", uiState.productPrice) { viewModel.updateProductPrice(it) }
                    }
                    if (fields.containsKey("spec")) {
                        FieldInput("规格", uiState.productSpec) { viewModel.updateProductSpec(it) }
                    }
                    if (fields.containsKey("unit")) {
                        FieldInput("单位", uiState.productUnit) { viewModel.updateProductUnit(it) }
                    }
                    if (fields.containsKey("area") || fields.containsKey("aera")) {
                        FieldInput("产地", uiState.productOrigin) { viewModel.updateProductOrigin(it) }
                    }
                }
            }

            // 打印预览
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("打印预览", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.filledLabel?.let { label ->
                        LabelPreview(
                            label = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 300.dp)
                        )
                    } ?: run {
                        Text("填写数据后预览", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // 打印状态
            uiState.printResult?.let { result ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    when (result) {
                        "success" -> {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("✅ 打印成功", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.clearPrintResult() }) { Text("关闭") }
                            }
                        }
                        else -> {
                            val msg = result.removePrefix("error:")
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("❌ 打印失败: $msg", color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.clearPrintResult() }) { Text("关闭") }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // FAB space
        }
    }

    // 商品选择弹窗
    if (uiState.showProductPicker) {
        ProductPickerDialog(
            products = uiState.productSearchResults,
            searchQuery = uiState.productSearchQuery,
            onSearch = { viewModel.searchProducts(it) },
            onSelect = { viewModel.fillFromProduct(it) },
            onDismiss = { viewModel.toggleProductPicker(false) }
        )
    }
}

@Composable
private fun FieldInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true
    )
}

@Composable
private fun LabelPreview(label: Label, modifier: Modifier = Modifier) {
    val labelW = label.widthMm
    val labelH = label.heightMm
    val aspectRatio = labelW / labelH

    BoxWithConstraints(modifier = modifier) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = if (constraints.maxHeight > 0) constraints.maxHeight.toFloat() else boxWidth / aspectRatio
        val displayW = boxWidth
        val displayH = minOf(boxHeight, boxWidth / aspectRatio)

        val scaleX = displayW / labelW
        val scaleY = displayH / labelH

        Canvas(modifier = Modifier.size(with(LocalDensity.current) { displayW.toDp() }, with(LocalDensity.current) { displayH.toDp() })) {
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
                            x,
                            y + fontSize,
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
                        val w = if (element.widthMm > 0) element.widthMm * scaleX else labelW * 0.8f * scaleX
                        val h = element.height * scaleY
                        // Draw barcode placeholder
                        drawRect(color = Color.DarkGray, topLeft = Offset(x, y), size = Size(w, h * 0.7f))
                        // Draw text below barcode
                        drawContext.canvas.nativeCanvas.drawText(
                            element.content,
                            x,
                            y + h * 0.7f + 10f,
                            android.graphics.Paint().apply {
                                textSize = 8f * minOf(scaleX, scaleY)
                                color = android.graphics.Color.BLACK
                            }
                        )
                    }
                    is LabelElement.QRCode -> {
                        val x = element.x * scaleX
                        val y = element.y * scaleY
                        val size = element.size * minOf(scaleX, scaleY)
                        drawRect(color = Color.DarkGray, topLeft = Offset(x, y), size = Size(size, size))
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
                            Text("¥${String.format("%.2f", product.price)} | ${product.spec} | ${product.barcode}", fontSize = 12.sp, color = Color.Gray)
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
