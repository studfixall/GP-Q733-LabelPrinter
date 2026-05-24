package com.gp.q733.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自定义标签模板
 * 存储用户通过编辑器创建的模板
 */
@Entity(tableName = "custom_templates")
data class CustomTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,              // 模板名称
    val widthMm: Float,            // 标签宽度mm
    val heightMm: Float,           // 标签高度mm
    val elementsJson: String,      // 元素列表JSON序列化
    val createdAt: Long = System.currentTimeMillis()
)
