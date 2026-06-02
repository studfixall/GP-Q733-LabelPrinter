package com.gp.q733.presentation.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.presentation.viewmodel.EditorViewModel
import com.gp.q733.presentation.viewmodel.ElementType
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onPrint: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val label = uiState.label
    var editingElementText by remember { mutableStateOf("") }
    // Snackbar for save/print feedback
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("标签已保存")
        }
    }
    // Export XML result dialog
    var exportedXml by remember { mutableStateOf<String?>(null) }
    if (exportedXml != null) {
        AlertDialog(
            onDismissRequest = { exportedXml = null },
            title = { Text("Barsoft XML 已生成") },
            text = {
                Column {
                    Text("以下 XML 可保存为 .xml 文件，导入佳博软件使用：", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = exportedXml!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { exportedXml = null }) {
                    Text("关闭")
                }
            }
        )
    }
    // 保存为模板对话框
    if (uiState.showSaveTemplateDialog) {
        var localTemplateName by remember { mutableStateOf(uiState.templateName) }
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveTemplateDialog() },
            title = { Text("保存为模板") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("将此标签布局保存为自定义模板，可在模板库中重复使用。", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = localTemplateName,
                        onValueChange = { localTemplateName = it; viewModel.updateTemplateName(it) },
                        label = { Text("模板名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = localTemplateName.ifBlank { "自定义模板" }
                        viewModel.saveAsTemplate(uiState.currentTemplateId, name)
                        viewModel.dismissSaveTemplateDialog()
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSaveTemplateDialog() }) { Text("取消") }
            }
        )
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    BottomSheetScaffold(
    sheetPeekHeight = 80.dp,
    sheetContent = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Selected Element Editor
            if (uiState.selectedElementIndex != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "编辑元素 (${uiState.selectedElementIndex!! + 1}/${label.elements.size})",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    uiState.selectedElementIndex?.let { viewModel.deleteElement(it) }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        OutlinedTextField(
                            value = editingElementText,
                            onValueChange = { text ->
                                editingElementText = text
                                uiState.selectedElementIndex?.let { index ->
                                    viewModel.updateElementContent(index, text)
                                }
                            },
                            label = { Text("内容") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 元素属性编辑（条码 / 文本）
                        uiState.selectedElementIndex?.let { index ->
                            val element = label.elements.getOrNull(index)
                            when (element) {
                                is LabelElement.Barcode -> {
                                    Text(
                                        text = "宽度: ${element.widthMm.toInt()}mm",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Slider(
                                        value = element.widthMm,
                                        onValueChange = { width -> viewModel.updateBarcodeWidth(index, width) },
                                        valueRange = 10f..80f,
                                        steps = 14,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "高度: ${element.height.toInt()}mm",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Slider(
                                        value = element.height,
                                        onValueChange = { height -> viewModel.updateBarcodeHeight(index, height) },
                                        valueRange = 3f..20f,
                                        steps = 17,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    // Issue #3 fix: Barcode也加数据绑定选择器
                                    Spacer(modifier = Modifier.height(8.dp))
                                    var barcodeBindingExpanded by remember { mutableStateOf(false) }
                                    val barcodeBindingOptions = listOf(
                                        "" to "不绑定（固定内容）",
                                        "barcode" to "条码",
                                        "name" to "商品名称",
                                        "price" to "价格",
                                        "mprice" to "会员价"
                                    )
                                    val currentBarcodeBindingLabel = barcodeBindingOptions.find { it.first == element.textName }?.second
                                        ?: "不绑定（固定内容）"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("绑定字段", style = MaterialTheme.typography.labelMedium)
                                        Box {
                                            TextButton(onClick = { barcodeBindingExpanded = true }) {
                                                Text(currentBarcodeBindingLabel)
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                            DropdownMenu(
                                                expanded = barcodeBindingExpanded,
                                                onDismissRequest = { barcodeBindingExpanded = false }
                                            ) {
                                                barcodeBindingOptions.forEach { (value, label) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label) },
                                                        onClick = {
                                                            viewModel.updateBarcodeTextName(index, value)
                                                            barcodeBindingExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
            is LabelElement.Text -> {
                // 离散字号档位 — 对应CPCL打印机实际支持的字体大小
                // 16x16CN(2mm基): 1x=2, 2x=4, 3x=6, 4x=8, 5x=10, 6x=12, 7x=14, 8x=16mm
                // 24x24CN(3mm基): 1x=3, 2x=6, 3x=9, 4x=12, 5x=15, 6x=18, 7x=21, 8x=24mm
                // 去重合并排序: 2,3,4,6,8,9,10,12,14,15,16,18,21,24
                val fontSizeOptions = listOf(2f, 3f, 4f, 6f, 8f, 9f, 10f, 12f, 14f, 15f, 16f, 18f, 21f, 24f)
                val fontSizeLabels = fontSizeOptions.map { "${it.toInt()}mm" }
                var fontSizeExpanded by remember { mutableStateOf(false) }
                                Text(
                    text = "字号",
                    style = MaterialTheme.typography.labelMedium
                )
                ExposedDropdownMenuBox(
                    expanded = fontSizeExpanded,
                    onExpandedChange = { fontSizeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${element.fontSize.toInt()}mm",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontSizeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fontSizeExpanded,
                        onDismissRequest = { fontSizeExpanded = false }
                    ) {
                        fontSizeOptions.forEachIndexed { idx, size ->
                            DropdownMenuItem(
                                text = { Text(fontSizeLabels[idx]) },
                                onClick = {
                                    viewModel.updateTextFontSize(index, size)
                                    fontSizeExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("粗体", style = MaterialTheme.typography.labelMedium)
                                        Switch(
                                            checked = element.isBold,
                                            onCheckedChange = { bold -> viewModel.updateTextBold(index, bold) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("下划线", style = MaterialTheme.typography.labelMedium)
                                        Switch(
                                            checked = element.isUnderline,
                                            onCheckedChange = { underline -> viewModel.updateTextUnderline(index, underline) }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // 数据绑定字段选择
                                    var bindingExpanded by remember { mutableStateOf(false) }
                                    val bindingOptions = listOf(
                                        "" to "不绑定（固定文本）",
                                        "name" to "商品名称",
                                        "price" to "价格",
                                        "mprice" to "会员价",
                                        "barcode" to "条码",
                                        "spec" to "规格",
                                        "unit" to "单位",
                                        "area" to "产地"
                                    )
                                    val currentBindingLabel = bindingOptions.find { it.first == element.textName }?.second ?: "不绑定（固定文本）"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("绑定字段", style = MaterialTheme.typography.labelMedium)
                                        Box {
                                            TextButton(onClick = { bindingExpanded = true }) {
                                                Text(currentBindingLabel)
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                            DropdownMenu(
                                                expanded = bindingExpanded,
                                                onDismissRequest = { bindingExpanded = false }
                                            ) {
                                                bindingOptions.forEach { (value, label) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label) },
                                                        onClick = {
                                                            viewModel.updateTextName(index, value)
                                                            bindingExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
                        // Label Offset Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val labelOffsetXText = remember { mutableStateOf(label.offsetX.toString()) }
                val labelOffsetYText = remember { mutableStateOf(label.offsetY.toString()) }
                LaunchedEffect(label.offsetX) { labelOffsetXText.value = label.offsetX.toString() }
                LaunchedEffect(label.offsetY) { labelOffsetYText.value = label.offsetY.toString() }
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "标签偏移",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = labelOffsetXText.value,
                            onValueChange = { text ->
                                labelOffsetXText.value = text
                                val filtered = text.filter { it.isDigit() || it == '-' || it == '.' }
                                if (filtered == text) {
                                    text.toFloatOrNull()?.let { viewModel.updateLabelOffset(it, label.offsetY) }
                                }
                            },
                            label = { Text("水平偏移(mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = labelOffsetYText.value,
                            onValueChange = { text ->
                                labelOffsetYText.value = text
                                val filtered = text.filter { it.isDigit() || it == '-' || it == '.' }
                                if (filtered == text) {
                                    text.toFloatOrNull()?.let { viewModel.updateLabelOffset(label.offsetX, it) }
                                }
                            },
                            label = { Text("垂直偏移(mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Text(
                        text = "负数向左/上偏移，叠加全局设置偏移",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

// Element Tools
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "添加元素",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElementButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.TextFields,
                            label = "文字",
                            onClick = {
                                viewModel.addElement(ElementType.Text, "文字内容")
                            }
                        )
                        ElementButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.ViewWeek,
                            label = "条码",
                            onClick = {
                                viewModel.addElement(ElementType.Barcode, "123456789")
                            }
                        )
                        ElementButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.QrCode,
                            label = "二维码",
                            onClick = {
                                viewModel.addElement(ElementType.QRCode, "https://example.com")
                            }
                        )
                        ElementButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.HorizontalRule,
                            label = "线条",
                            onClick = {
                                viewModel.addElement(ElementType.Line, "")
                            }
                        )
                    }
                }
            }
        }
    },
        topBar = {
            TopAppBar(
                title = { Text("标签编辑 - ${label.widthMm.toInt()}×${label.heightMm.toInt()}mm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.printLabel() }) {
                            Icon(Icons.Default.Print, contentDescription = "打印")
                        }
                    }
                    IconButton(onClick = { viewModel.saveCurrentTemplate() }) {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                        }
                        IconButton(onClick = { viewModel.showSaveTemplateDialog() }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "存为模板")
                        }
                        IconButton(onClick = {
                            val xml = viewModel.exportBarsoftXml()
                            exportedXml = xml
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "导出XML")
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
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                
                // Calculate initial auto-fit scale
                var autoScale by remember { mutableFloatStateOf(2.7f) } // 默认缩放 270%
                var userScale by remember { mutableFloatStateOf(1f) }
                val scale = autoScale * userScale
                
                // Pan offset for moving the canvas
                var panOffset by remember { mutableStateOf(Offset.Zero) }
                
                // Calculate base label size in pixels (8 dots per mm at 203dpi)
                val labelWidthPx = label.widthMm * 8f
                val labelHeightPx = label.heightMm * 8f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Detect pinch zoom gestures
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                // Update user scale with zoom gesture
                                userScale = (userScale * zoom).coerceIn(0.5f, 5f)
                                // Update pan offset
                                panOffset = panOffset + pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Label canvas with pan and zoom applied
                    Box(
                        modifier = Modifier
                            .offset { 
                                IntOffset(
                                    panOffset.x.toInt(),
                                    panOffset.y.toInt()
                                )
                            }
                            .width(with(density) { (labelWidthPx * scale).toDp() })
                            .height(with(density) { (labelHeightPx * scale).toDp() })
                            .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
                            .background(Color.White)
                    ) {
                        // Show label dimensions
                        Text(
                            text = "${label.widthMm.toInt()}×${label.heightMm.toInt()}mm",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        )
                        // Render label elements
                        label.elements.forEachIndexed { index, element ->
                            val isSelected = uiState.selectedElementIndex == index
                            // Use element position directly - no local cache to avoid stale data
                            var dragOffsetX by remember { mutableFloatStateOf(0f) }
                            var dragOffsetY by remember { mutableFloatStateOf(0f) }
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = with(density) { ((element.x + dragOffsetX) * 8f * scale).toDp() },
                                        y = with(density) { ((element.y + dragOffsetY) * 8f * scale).toDp() }
                                    )
                                    .pointerInput(element) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                viewModel.updateElementPosition(index, element.x + dragOffsetX, element.y + dragOffsetY)
                                                dragOffsetX = 0f
                                                dragOffsetY = 0f
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            // Convert drag amount to mm
                                            val dragMmX = dragAmount.x / (8f * scale)
                                            val dragMmY = dragAmount.y / (8f * scale)
                                            dragOffsetX = (dragOffsetX + dragMmX).coerceIn(-element.x, label.widthMm - element.x)
                                            dragOffsetY = (dragOffsetY + dragMmY).coerceIn(-element.y, label.heightMm - element.y)
                                        }
                                    }
                                    .clickable {
                                        viewModel.selectElement(index)
                                        editingElementText = when (element) {
                                            is LabelElement.Text -> element.text
                                            is LabelElement.Barcode -> element.content
                                            is LabelElement.QRCode -> element.content
                                            is LabelElement.Line -> ""
                                        }
                                    }
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(2.dp)
                                        ) else Modifier
                                    )
                            ) {
                                when (element) {
                                    is LabelElement.Text -> Text(
                                        text = element.text,
                                        fontSize = (element.fontSize * scale).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.Black
                                    )
                                    is LabelElement.Barcode -> {
                                        val barcodeHeightMm = element.height
                                        val barcodeWidthMm = element.widthMm
                                        val barcodeBitmap = remember(element.content, barcodeWidthMm, barcodeHeightMm, scale) {
                                            generateBarcodeBitmap(element.content, barcodeWidthMm, barcodeHeightMm)
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            barcodeBitmap?.let { bitmap ->
                                                // 使用设置的宽度 + 两侧各2mm边距
                                                val displayWidthMm = barcodeWidthMm + 4f
                                                val barcodeWidth = with(density) { (displayWidthMm * 8f * scale).toDp() }
                                                val barcodeHeight = with(density) { (barcodeHeightMm * 8f * scale).toDp() }
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .width(barcodeWidth)
                                                        .height(barcodeHeight)
                                                )
                                            } ?: Icon(
                                                imageVector = Icons.Default.ViewWeek,
                                                contentDescription = null,
                                                modifier = Modifier.size(with(density) { (element.height * 8f * scale).toDp() }),
                                                tint = Color.Black
                                            )
                                            Text(
                                                text = element.content,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    is LabelElement.QRCode -> {
                                        val qrBitmap = remember(element.content, element.size, scale) {
                                            generateQRCodeBitmap(element.content, element.size)
                                        }
                                        qrBitmap?.let { bitmap ->
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(with(density) { (element.size * 8f * scale).toDp() })
                                            )
                                        } ?: Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(with(density) { (element.size * 8f * scale).toDp() }),
                                            tint = Color.Black
                                        )
                                    }
                                    is LabelElement.Line -> {
                                        val lineWidth = with(density) { (element.width * 8f * scale).toDp() }
                                        val lineHeight = with(density) { (element.height.coerceAtLeast(0.5f) * 8f * scale).toDp() }
                                        Box(
                                            modifier = Modifier
                                                .width(lineWidth)
                                                .height(lineHeight)
                                                .background(Color.Black)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Zoom indicator
                Text(
                    text = "${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ElementButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * 生成条码 Bitmap (CODE128) - 指定宽度
 */
private fun generateBarcodeBitmap(content: String, widthMm: Float, heightMm: Float): Bitmap? {
    return try {
        val widthPx = (widthMm * 8f).toInt().coerceAtLeast(50)
        val heightPx = (heightMm * 8f).toInt().coerceAtLeast(30)
        
        val writer = MultiFormatWriter()
        val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.CODE_128, widthPx, heightPx)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 生成条码 Bitmap (CODE128) - 自动计算宽度
 */
private fun generateBarcodeBitmapAutoWidth(content: String, heightMm: Float): Bitmap? {
    return try {
        val heightPx = (heightMm * 8f).toInt().coerceAtLeast(50)
        // 先用一个足够大的宽度生成条码
        val tempWidthPx = (content.length * 12f * 8f).toInt().coerceAtLeast(200)
        
        val writer = MultiFormatWriter()
        val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.CODE_128, tempWidthPx, heightPx)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        
        // 裁剪掉两侧空白区域
        var leftBound = 0
        var rightBound = width - 1
        
        // 找左边界
        for (x in 0 until width) {
            var hasBlack = false
            for (y in 0 until height) {
                if (bitMatrix[x, y]) {
                    hasBlack = true
                    break
                }
            }
            if (hasBlack) {
                leftBound = x
                break
            }
        }
        
        // 找右边界
        for (x in width - 1 downTo 0) {
            var hasBlack = false
            for (y in 0 until height) {
                if (bitMatrix[x, y]) {
                    hasBlack = true
                    break
                }
            }
            if (hasBlack) {
                rightBound = x
                break
            }
        }
        
        // 创建裁剪后的 bitmap（只包含实际条码内容，不含两侧空白）
        val actualWidth = rightBound - leftBound + 1
        val bitmap = Bitmap.createBitmap(actualWidth, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until actualWidth) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[leftBound + x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 生成二维码 Bitmap
 */
private fun generateQRCodeBitmap(content: String, sizeMm: Float): Bitmap? {
    return try {
        val sizePx = (sizeMm * 8f).toInt().coerceAtLeast(100)
        
        val writer = MultiFormatWriter()
        val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
