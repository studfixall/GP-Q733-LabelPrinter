package com.gp.q733.domain.repository

import com.gp.q733.domain.model.ProductInfo
import kotlinx.coroutines.flow.Flow

/**
 * 商品数据仓库接口
 * 提供商品CRUD、搜索、导入功能
 */
interface ProductRepository {

    /** 根据条码获取商品信息 */
    suspend fun getProductByBarcode(barcode: String): ProductInfo?
    /** 获取所有商品（Flow，实时更新） */
    fun getAllProducts(): Flow<List<ProductInfo>>
    /** 搜索商品（按名称或条码模糊匹配） */
    fun searchProducts(keyword: String): Flow<List<ProductInfo>>
    /** 添加商品，返回行ID */
    suspend fun addProduct(product: ProductInfo): Long
    /** 更新商品 */
    suspend fun updateProduct(product: ProductInfo)
    /** 删除商品 */
    suspend fun deleteProduct(product: ProductInfo)
    /** 批量导入（CSV），返回导入数量 */
    suspend fun importProducts(products: List<ProductInfo>): Int
    /** 获取商品总数 */
    suspend fun getProductCount(): Int
}
