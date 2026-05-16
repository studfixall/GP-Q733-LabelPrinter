package com.gp.q733.data.repository

import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.repository.ProductRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 商品数据仓库实现（模拟数据）
 * 
 * TODO: 实际项目中需要替换为真实的 API 或数据库查询
 * 当前使用模拟数据用于功能演示，后续需要：
 * 1. 配置 SQL Server 连接（通过 REST API 中转）
 * 2. 或实现真实的 API 客户端调用远程服务
 */
@Singleton
class ProductRepositoryImpl @Inject constructor() : ProductRepository {

    // 模拟商品数据库（测试用）
    private val mockProducts = mapOf(
        "6901234567890" to ProductInfo("6901234567890", "可口可乐 330ml", 3.5),
        "6901234567891" to ProductInfo("6901234567891", "雪碧 330ml", 3.5),
        "6901234567892" to ProductInfo("6901234567892", "芬达 330ml", 3.5),
        "6901234567893" to ProductInfo("6901234567893", "冰红茶 500ml", 4.0),
        "6901234567894" to ProductInfo("6901234567894", "矿泉水 550ml", 2.0),
        "6901234567895" to ProductInfo("6901234567895", "牛奶 250ml", 5.5),
        "6901234567896" to ProductInfo("6901234567896", "面包 100g", 6.0),
        "6901234567897" to ProductInfo("6901234567897", "饼干 200g", 8.5),
        "6901234567898" to ProductInfo("6901234567898", "薯片 150g", 7.0),
        "6901234567899" to ProductInfo("6901234567899", "巧克力 100g", 12.0)
    )

    override suspend fun getProductByBarcode(barcode: String): ProductInfo? {
        // TODO: 替换为真实的 API 调用或数据库查询
        // 示例：return apiService.getProduct(barcode)
        
        return mockProducts[barcode]
    }
}
