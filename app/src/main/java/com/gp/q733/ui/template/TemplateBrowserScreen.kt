package com.gp.q733.ui.template

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gp.q733.domain.model.Label

/**
 * Barsoft 模板浏览器界面
 * 显示 97 个 XML 标签模板，按分类筛选，点击选择模板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateBrowserScreen(
    viewModel: TemplateBrowserViewModel,
    onBack: () -> Unit,
    onTemplateSelected: (Label) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("标签模板库") },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索栏
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.search(it) },
                placeholder = "搜索模板名称或尺寸...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 分类筛选
            CategoryFilter(
                categories = listOf("全部", "通用", "价格", "服装", "珠宝", "自定义"),
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 加载状态
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // 模板数量
                Text(
                    text = "共 ${uiState.filteredTemplates.size} 个模板",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // 模板列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredTemplates) { template ->
                        TemplateCard(
                            template = template,
                            onClick = {
                    com.gp.q733.presentation.navigation.SharedTemplateHolder.label = template.label
                    onTemplateSelected(template.label)
                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryMap = mapOf(
        "全部" to "全部", "通用" to "normal", "价格" to "price",
        "服装" to "clothe", "珠宝" to "jewelry", "自定义" to "custome"
    )

    ScrollableTabRow(
        selectedTabIndex = categories.indexOf(categoryMap.entries.find { it.value == selectedCategory }?.key ?: "全部"),
        edgePadding = 0.dp,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        categories.forEach { label ->
            val internalCategory = categoryMap[label] ?: "全部"
            Tab(
                selected = selectedCategory == internalCategory,
                onClick = { onCategorySelected(internalCategory) },
                text = { Text(label, fontSize = 13.sp) }
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: BarsoftTemplateInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 尺寸图标区
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${template.widthMm.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "×${template.heightMm.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "mm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 模板信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${template.elementCount} 个元素 · ${template.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (template.fieldNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    // 数据绑定字段标签
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        template.fieldNames.take(5).forEach { field ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = fieldNameLabel(field),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        if (template.fieldNames.size > 5) {
                            Text(
                                text = "+${template.fieldNames.size - 5}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun fieldNameLabel(textName: String): String {
    return when (textName.lowercase().trim()) {
        "name" -> "品名"
        "price" -> "价格"
        "mprice" -> "会员价"
        "spec", "sepc" -> "规格"
        "unit" -> "单位"
        "area", "aera" -> "产地"
        "barcode" -> "条码"
        else -> textName
    }
}
