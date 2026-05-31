package com.gp.q733.ui.template

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.util.BarsoftTemplateParser
import com.gp.q733.domain.util.TemplateJsonParser
import com.gp.q733.data.local.db.CustomTemplateDao
import com.gp.q733.data.local.db.CustomTemplateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * 自定义模板信息（来自 Room 数据库）
 */
data class CustomTemplateInfo(
    val id: Long,
    val templateId: String,  // "custom_xxx" or "built_in_xxx"
    val isQuickPrint: Boolean,  // 是否显示在扫码打印快捷选择中
    val name: String,
    val displayName: String,  // 格式："40×30mm 自定义"
    val widthMm: Float,
    val heightMm: Float,
    val elementCount: Int,
    val fieldNames: List<String>,
    val label: Label,
    val isBuiltIn: Boolean,
    val createdAt: Long
)

/**
 * Barsoft XML 模板信息（来自 assets）
 */
data class BarsoftTemplateInfo(
    val assetPath: String,
    val fileName: String,
    val category: String,
    val displayName: String,
    val widthMm: Float,
    val heightMm: Float,
    val elementCount: Int,
    val fieldNames: List<String>,
    val label: Label
)

data class TemplateBrowserUiState(
    val builtInTemplates: List<BarsoftTemplateInfo> = emptyList(),
    val customTemplates: List<CustomTemplateInfo> = emptyList(),
    val filteredBuiltIn: List<BarsoftTemplateInfo> = emptyList(),
    val filteredCustom: List<CustomTemplateInfo> = emptyList(),
    val selectedCategory: String = "全部",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * 模板浏览器：合并展示内置模板(assets) + 自定义模板(Room)
 * 自定义模板排前面（按创建时间倒序），内置按尺寸排后面
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class TemplateBrowserViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customTemplateDao: CustomTemplateDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateBrowserUiState())
    val uiState: StateFlow<TemplateBrowserUiState> = _uiState.asStateFlow()
    suspend fun loadTemplates() = withContext(Dispatchers.IO) {
        try {
            // 1. Load built-in templates from assets
            val builtInTemplates = mutableListOf<BarsoftTemplateInfo>()
            val topDirs = context.assets.list("templates") ?: emptyArray()
            for (topDir in topDirs) {
                val cats = context.assets.list("templates/$topDir") ?: emptyArray()
                for (cat in cats) {
                    val files = context.assets.list("templates/$topDir/$cat") ?: emptyArray()
                    for (file in files) {
                        if (file.endsWith(".xml")) {
                            val filePath = "templates/$topDir/$cat/$file"
                            try {
                                val inputStream = context.assets.open(filePath)
                                val templateInfo = parseTemplate(inputStream, filePath, file, cat)
                                inputStream.close()
                                if (templateInfo != null) {
                                    builtInTemplates.add(templateInfo)
                                }
                            } catch (e: Exception) {
                                // Skip unreadable
                            }
                        }
                    }
                }
            }
            builtInTemplates.sortWith(compareBy({ it.widthMm * it.heightMm }, { it.fileName }))
            // 2. Load custom/built-in templates from Room
            val customTemplates = mutableListOf<CustomTemplateInfo>()
            val entities = customTemplateDao.getAllSorted().first()
            for (entity in entities) {
                try {
                    val elements = TemplateJsonParser.fromJson(entity.elementsJson)
                    val fieldNames = elements
                        .filterIsInstance<com.gp.q733.domain.model.LabelElement.Text>()
                        .mapNotNull { it.textName }
                        .filter { BarsoftTemplateParser.isDataBindingField(it) }
                        .distinct()
                    val sizeStr = "${entity.widthMm.toInt()}×${entity.heightMm.toInt()}mm"
                    val categoryLabel = if (entity.isBuiltIn) "内置" else "自定义"
                    customTemplates.add(
                        CustomTemplateInfo(
                            id = entity.id,
                            templateId = entity.templateId,
                            name = entity.name,
                            displayName = "$sizeStr $categoryLabel",
                            widthMm = entity.widthMm,
                            heightMm = entity.heightMm,
                            elementCount = elements.size,
                            fieldNames = fieldNames,
                            label = Label(
                                id = "template_${entity.id}",
                                widthMm = entity.widthMm,
                                heightMm = entity.heightMm,
                                elements = elements
                            ),
                            isQuickPrint = entity.isQuickPrint,
                            isBuiltIn = entity.isBuiltIn,
                            createdAt = entity.createdAt
                        )
                    )
                } catch (e: Exception) {
                    // Skip corrupt
                }
            }
            // 自定义按 createdAt DESC（新的在前）
            customTemplates.sortByDescending { it.createdAt }
            _uiState.value = _uiState.value.copy(
                builtInTemplates = builtInTemplates,
                customTemplates = customTemplates,
                filteredBuiltIn = builtInTemplates,
                filteredCustom = customTemplates,
                isLoading = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "加载模板失败: ${e.message}"
            )
        }
    }
    fun toggleQuickPrint(templateId: String, currentQuickPrint: Boolean) {
        viewModelScope.launch {
            customTemplateDao.setQuickPrint(templateId, !currentQuickPrint)
            loadTemplates()
        }
    }
    fun selectCategory(category: String) {
        val current = _uiState.value
        val builtInFiltered = if (category == "全部") {
            current.builtInTemplates
        } else {
            current.builtInTemplates.filter { it.category == category }
        }.let { list ->
            if (current.searchQuery.isBlank()) list
            else list.filter {
                it.displayName.contains(current.searchQuery, ignoreCase = true) ||
                it.fileName.contains(current.searchQuery, ignoreCase = true)
            }
        }
        val customFiltered = current.customTemplates.let { list ->
            if (current.searchQuery.isBlank()) list
            else list.filter {
                it.displayName.contains(current.searchQuery, ignoreCase = true) ||
                it.name.contains(current.searchQuery, ignoreCase = true)
            }
        }
        _uiState.value = current.copy(
            selectedCategory = category,
            filteredBuiltIn = builtInFiltered,
            filteredCustom = customFiltered
        )
    }
    fun search(query: String) {
        val current = _uiState.value
        val builtInFiltered = current.builtInTemplates.let { list ->
            if (current.selectedCategory == "全部") list
            else list.filter { it.category == current.selectedCategory }
        }.filter {
            it.displayName.contains(query, ignoreCase = true) ||
            it.fileName.contains(query, ignoreCase = true)
        }
        val customFiltered = current.customTemplates.filter {
            it.displayName.contains(query, ignoreCase = true) ||
            it.name.contains(query, ignoreCase = true)
        }
        _uiState.value = current.copy(
            searchQuery = query,
            filteredBuiltIn = builtInFiltered,
            filteredCustom = customFiltered
        )
    }
    private fun parseTemplate(
        inputStream: InputStream,
        assetPath: String,
        fileName: String,
        category: String
    ): BarsoftTemplateInfo? {
        return try {
            val label = BarsoftTemplateParser.parse(inputStream)
            val fieldNames = label.elements
                .filterIsInstance<com.gp.q733.domain.model.LabelElement.Text>()
                .mapNotNull { it.textName }
                .filter { BarsoftTemplateParser.isDataBindingField(it) }
                .distinct()
            BarsoftTemplateInfo(
                assetPath = assetPath,
                fileName = fileName,
                category = category,
                displayName = generateDisplayName(fileName, label.widthMm, label.heightMm, category),
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
