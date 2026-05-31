package com.gp.q733.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 标签模板
 * 内置模板（isBuiltIn=true）由系统初始化，不可删除
 * 自定义模板（isBuiltIn=false）由用户创建，可增删
 * templateId 唯一索引，支持 upsert 语义
 */
@Entity(
    tableName = "custom_templates",
    indices = [Index(value = ["templateId"], unique = true)]
)
data class CustomTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: String,         // 唯一标识，内置模板用字符串ID（built_in_xxx），自定义用数字字符串
    val name: String,               // 模板名称
    val widthMm: Float,             // 标签宽度mm
    val heightMm: Float,           // 标签高度mm
    val elementsJson: String,      // 元素列表JSON序列化
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val isBuiltIn: Boolean = false, // 是否内置模板
    val sortOrder: Int = 0,        // 排序顺序（内置优先，再按sortOrder）
    val createdAt: Long = System.currentTimeMillis(),
    val isQuickPrint: Boolean = false  // 是否显示在扫码打印快捷选择中
)
