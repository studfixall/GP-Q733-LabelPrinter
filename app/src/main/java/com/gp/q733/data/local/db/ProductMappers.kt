package com.gp.q733.data.local.db

import com.gp.q733.domain.model.ProductInfo

/**
 * ProductEntity ↔ ProductInfo 转换扩展
 */

fun ProductEntity.toDomain(): ProductInfo = ProductInfo(
    barcode = barcode,
    name = name,
    price = price,
    spec = spec,
    unit = unit,
    category = category
)

fun ProductInfo.toEntity(
    id: Long = 0,
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): ProductEntity = ProductEntity(
    id = id,
    barcode = barcode,
    name = name,
    price = price,
    spec = spec,
    unit = unit,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)
