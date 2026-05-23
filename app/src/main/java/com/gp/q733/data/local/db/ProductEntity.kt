package com.gp.q733.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gp.q733.domain.model.ProductInfo

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
    val barcode: String,     // 商品条码（唯一索引）
    val name: String,        // 商品名称
    val price: Double,       // 商品价格
    val spec: String = "",   // 规格（如：330ml、500g）
    val unit: String = "",   // 单位（如：个、瓶、箱）
    val origin: String = "", // 产地（如：上海、浙江）— 对应 Barsoft textName=area
    val category: String = "",// 分类
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * ProductEntity ↔ ProductInfo 转换
 */
fun ProductEntity.toDomain() = ProductInfo(
    barcode = barcode,
    name = name,
    price = price,
    spec = spec,
    unit = unit,
    origin = origin,
    category = category
)

fun ProductInfo.toEntity() = ProductEntity(
    barcode = barcode,
    name = name,
    price = price,
    spec = spec,
    unit = unit,
    origin = origin,
    category = category
)
