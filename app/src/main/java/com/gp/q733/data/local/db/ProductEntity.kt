package com.gp.q733.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 商品数据表
 * 本地Room数据库存储，支持手动录入和CSV导入
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"], unique = true)
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,       // 商品条码（唯一索引）
    val name: String,          // 商品名称
    val price: Double,         // 商品价格
    val spec: String = "",     // 规格（如：330ml、500g）
    val unit: String = "",     // 单位（如：个、瓶、箱）
    val category: String = "", // 分类
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
