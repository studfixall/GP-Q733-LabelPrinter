package com.gp.q733.ui.template

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.util.BarsoftTemplateParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Barsoft XML 模板信息
 */
data class BarsoftTemplateInfo(
    val assetPath: String,      // assets 完整路径（如 "templates/Templet/normal/4030.xml"）
    val fileName: String,       // 文件名（如 "4030.xml"）
    val category: String,       // 分类（normal/price/clothe/jewelry/custome）
    val displayName: String,    // 显示名称（如 "40×30mm 商品标签"）
    val widthMm: Float,         // 标签宽度mm
    val heightMm: Float,        // 标签高度mm
    val elementCount: Int,      // 元素数量
    val fieldNames: List<String>, // 数据绑定字段名列表
    val label: Label            // 解析后的Label对象
)

data class TemplateBrowserUiState(
    val templates: List<BarsoftTemplateInfo> = emptyList(),
    val filteredTemplates: List<BarsoftTemplateInfo> = emptyList(),
    val selectedCategory: String = "全部",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Barsoft 模板浏览器 ViewModel
 * 从 assets/templates/ 加载 97 个 XML 模板
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class TemplateBrowserViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateBrowserUiState())
    val uiState: StateFlow<TemplateBrowserUiState> = _uiState.asStateFlow()

    private val categories = listOf("全部", "normal", "price", "clothe", "jewelry", "custome")

    suspend fun loadTemplates() = withContext(Dispatchers.IO) {
        try {
            val templates = mutableListOf<BarsoftTemplateInfo>()
            val topDirs = context.assets.list("templates") ?: emptyArray()

            for (topDir in topDirs) {
                val categories = context.assets.list("templates/$topDir") ?: emptyArray()
                for (category in categories) {
                    val files = context.assets.list("templates/$topDir/$category") ?: emptyArray()
                    for (file in files) {
                        if (file.endsWith(".xml")) {
                            val filePath = "templates/$topDir/$category/$file"
                            try {
                                val inputStream = context.assets.open(filePath)
                                val templateInfo = parseTemplate(inputStream, filePath, file, category)
                                if (templateInfo != null) {
                                    templates.add(templateInfo)
                                }
                            } catch (e: Exception) {
                                // Skip unreadable templates
                            }
                        }
                    }
                }
            }

            // Sort by size (width*height) then by name
            templates.sortWith(compareBy({ it.widthMm * it.heightMm }, { it.fileName }))

            _uiState.value = _uiState.value.copy(
                templates = templates,
                filteredTemplates = templates,
                isLoading = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "加载模板失败: ${e.message}"
            )
        }
    }

    fun selectCategory(category: String) {
        val current = _uiState.value
        val filtered = if (category == "全部") {
            current.templates
        } else {
            current.templates.filter { it.category == category }
        }.let { list ->
            if (current.searchQuery.isBlank()) list
            else list.filter { it.displayName.contains(current.searchQuery, ignoreCase = true) }
        }

        _uiState.value = current.copy(
            selectedCategory = category,
            filteredTemplates = filtered
        )
    }

    fun search(query: String) {
        val current = _uiState.value
        val filtered = current.templates.let { list ->
            if (current.selectedCategory == "全部") list
            else list.filter { it.category == current.selectedCategory }
        }.filter { it.displayName.contains(query, ignoreCase = true) || it.fileName.contains(query, ignoreCase = true) }

        _uiState.value = current.copy(
            searchQuery = query,
            filteredTemplates = filtered
        )
    }

    private fun parseTemplate(inputStream: InputStream, assetPath: String, fileName: String, category: String): BarsoftTemplateInfo? {
        return try {
            val label = BarsoftTemplateParser.parse(inputStream)
            inputStream.close()

            // Extract field names from textName bindings
            val fieldNames = label.elements
                .filterIsInstance<com.gp.q733.domain.model.LabelElement.Text>()
                .mapNotNull { (it as? com.gp.q733.domain.model.LabelElement.Text)?.textName }
                .filter { it.isNotBlank() && BarsoftTemplateParser.isDataBindingField(it) }
                .distinct()

            // Generate display name
            val displayName = generateDisplayName(fileName, label.widthMm, label.heightMm, category)

            BarsoftTemplateInfo(
                assetPath = assetPath,
                fileName = fileName,
                category = category,
                displayName = displayName,
                widthMm = label.widthMm,
                heightMm = label.heightMm,
                elementCount = label.elements.size,
                fieldNames = fieldNames,
                label = label
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun generateDisplayName(fileName: String, width: Float, height: Float, category: String): String {
        val sizeStr = "${width.toInt()}×${height.toInt()}mm"
        val categoryLabel = when (category) {
            "normal" -> "通用"
            "price" -> "价格"
            "clothe" -> "服装"
            "jewelry" -> "珠宝"
            "custome" -> "自定义"
            else -> ""
        }
        return "$sizeStr $categoryLabel"
    }
}
