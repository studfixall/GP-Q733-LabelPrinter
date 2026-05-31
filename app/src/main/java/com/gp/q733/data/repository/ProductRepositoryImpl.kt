package com.gp.q733.data.repository

import android.content.Context
import com.gp.q733.data.local.db.ProductDao
import com.gp.q733.data.local.db.ProductDatabase
import com.gp.q733.data.local.db.toDomain
import com.gp.q733.data.local.db.toEntity
import com.gp.q733.data.util.CsvParser
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.repository.ProductRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 商品数据仓库实现
 * 基于Room本地数据库，支持CRUD + 模糊搜索 + 批量导入
 * 首次启动自动从assets预加载示例数据
 */
@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDatabase: ProductDatabase.Provider,
    @ApplicationContext private val context: Context
) : ProductRepository {

    private val dao: ProductDao by lazy { productDatabase.get().productDao() }
    @Volatile
    private var preloaded = false
    private suspend fun ensurePreloaded() {
        if (preloaded) return
        if (dao.getCount() > 0) {
            preloaded = true
            return
        }
        try {
            val inputStream = context.assets.open("sample_products.csv")
            val result = CsvParser.parse(inputStream)
            if (result.products.isNotEmpty()) {
                val entities = result.products.map { it.toEntity() }
                dao.insertAll(entities)
            }
        } catch (e: Exception) {
            // 预加载失败不影响功能
        }
        preloaded = true
    }
    override suspend fun getProductByBarcode(barcode: String): ProductInfo? {
        ensurePreloaded()
        return dao.getProductByBarcode(barcode)?.toDomain()
    }
    override fun getAllProducts(): Flow<List<ProductInfo>> {
        return dao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    override fun searchProducts(keyword: String): Flow<List<ProductInfo>> {
        return dao.searchProducts(keyword).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    override suspend fun addProduct(product: ProductInfo): Long {
        ensurePreloaded()
        return dao.insert(product.toEntity())
    }
    override suspend fun updateProduct(product: ProductInfo) {
        ensurePreloaded()
        val existing = dao.getProductByBarcode(product.barcode)
        if (existing != null) {
            dao.update(product.toEntity().copy(id = existing.id, createdAt = existing.createdAt))
        } else {
            dao.insert(product.toEntity())
        }
    }
    override suspend fun deleteProduct(product: ProductInfo) {
        val existing = dao.getProductByBarcode(product.barcode)
        if (existing != null) {
            dao.delete(existing)
        }
    }
    override suspend fun importProducts(products: List<ProductInfo>): Int {
        ensurePreloaded()
        val entities = products.map { it.toEntity() }
        dao.insertAll(entities)
        return products.size
    }
    override suspend fun getProductCount(): Int {
        return dao.getCount()
    }
}
