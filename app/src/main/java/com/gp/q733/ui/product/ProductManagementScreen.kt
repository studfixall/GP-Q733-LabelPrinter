package com.gp.q733.ui.product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gp.q733.data.util.CsvParser
import com.gp.q733.domain.model.ProductInfo

/**
 * 商品管理界面
 * 支持列表展示、搜索、新增/编辑/删除、CSV导入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    viewModel: ProductViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val searchKeyword by viewModel.searchKeyword.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val editProduct by viewModel.editProduct.collectAsState()
    val importMessage by viewModel.importMessage.collectAsState()
    val context = LocalContext.current
    // CSV文件选择器
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    val result = CsvParser.parse(inputStream)
                    if (result.products.isNotEmpty()) {
                        viewModel.importFromCsv(result.products)
                    }
                    if (result.errors.isNotEmpty()) {
                        val errorMsg = result.errors.take(3).joinToString("\n")
                        viewModel.setImportMessage(
                            if (result.products.isEmpty()) "导入失败:\n$errorMsg"
                            else "导入${result.products.size}条，${result.errors.size}行有错误:\n$errorMsg"
                        )
                    }
                }
            } catch (e: Exception) {
                viewModel.setImportMessage("读取文件失败: ${e.message}")
            }
        }
    }
    // 删除确认对话框
    var productToDelete by remember { mutableStateOf<ProductInfo?>(null) }
    // 清空所有数据确认
    var showClearDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("商品管理") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
                actions = {
                    // CSV导入按钮
                    IconButton(onClick = {
                        csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "导入CSV")
                    }
                // 清空数据按钮
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "清空数据")
                }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "添加商品")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { viewModel.updateSearchKeyword(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索商品名称或条码") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            // 导入提示
            importMessage?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearImportMessage() }) {
                            Text("关闭")
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(msg)
                }
            }
            // 商品列表
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暂无商品数据", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("点击 + 手动添加，或点击顶部 ↑ 导入CSV", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.barcode }) { product ->
                        ProductItem(
                            product = product,
                            onEdit = { viewModel.showEditDialog(product) },
                            onDelete = { productToDelete = product }
                        )
                    }
                }
            }
        }
        // 编辑/新增对话框
        if (showEditDialog) {
            ProductEditDialog(
                product = editProduct,
                onDismiss = { viewModel.dismissEditDialog() },
                onSave = { viewModel.saveProduct(it) }
            )
        }
        // 删除确认对话框
        productToDelete?.let { product ->
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("确认删除") },
                text = { Text("确定要删除商品「${product.name}」吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteProduct(product)
                            productToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }

    // 清空数据确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空所有商品") },
            text = { Text("确定要清空所有商品数据吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllProducts()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ProductItem(
    product: ProductInfo,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "条码: ${product.barcode}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (product.spec.isNotBlank()) {
                    Text(
                        text = "规格: ${product.spec}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (product.unit.isNotBlank() || product.category.isNotBlank()) {
                    Text(
                        text = listOfNotNull(
                            product.unit.takeIf { it.isNotBlank() },
                            product.category.takeIf { it.isNotBlank() }
                        ).joinToString(" | "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "¥${"%.2f".format(product.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (product.mprice > 0 && product.mprice != product.price) {
                        Text(
                            text = "会员 ¥${"%.2f".format(product.mprice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 商品编辑/新增对话框
 */
@Composable
private fun ProductEditDialog(
    product: ProductInfo?,
    onDismiss: () -> Unit,
    onSave: (ProductInfo) -> Unit
) {
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var mprice by remember { mutableStateOf(product?.mprice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var spec by remember { mutableStateOf(product?.spec ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "添加商品" else "编辑商品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("条码 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = product == null
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("零售价 *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mprice,
                        onValueChange = { mprice = it },
                        label = { Text("会员价") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("同零售价") }
                    )
                }
                OutlinedTextField(
                    value = spec,
                    onValueChange = { spec = it },
                    label = { Text("规格") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("单位") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val priceValue = price.toDoubleOrNull() ?: 0.0
                    val mpriceValue = mprice.toDoubleOrNull() ?: 0.0
                    if (barcode.isNotBlank() && name.isNotBlank()) {
                        onSave(
                            ProductInfo(
                                barcode = barcode.trim(),
                                name = name.trim(),
                                price = priceValue,
                                mprice = if (mpriceValue > 0) mpriceValue else priceValue,
                                spec = spec.trim(),
                                unit = unit.trim(),
                                category = category.trim()
                            )
                        )
                    }
                },
                enabled = barcode.isNotBlank() && name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
