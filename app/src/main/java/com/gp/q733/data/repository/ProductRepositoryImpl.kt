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
        "6901234567890" to ProductInfo(barcode = "6901234567890", name = "可口可乐 330ml", price = "3.5", unit = "瓶"),
        "6901234567891" to ProductInfo(barcode = "6901234567891", name = "雪碧 330ml", price = "3.5", unit = "瓶"),
        "6901234567892" to ProductInfo(barcode = "6901234567892", name = "芬达 330ml", price = "3.5", unit = "瓶"),
        "6901234567893" to ProductInfo(barcode = "6901234567893", name = "冰红茶 500ml", price = "4.0", unit = "瓶"),
        "6901234567894" to ProductInfo(barcode = "6901234567894", name = "矿泉水 550ml", price = "2.0", unit = "瓶"),
        "6901234567895" to ProductInfo(barcode = "6901234567895", name = "牛奶 250ml", price = "5.5", unit = "盒"),
        "6901234567896" to ProductInfo(barcode = "6901234567896", name = "面包 100g", price = "6.0", unit = "个"),
        "6901234567897" to ProductInfo(barcode = "6901234567897", name = "饼干 200g", price = "8.5", unit = "包"),
        "6901234567898" to ProductInfo(barcode = "6901234567898", name = "薯片 150g", price = "7.0", unit = "包"),
        "6901234567899" to ProductInfo(barcode = "6901234567899", name = "巧克力 100g", price = "12.0", unit = "盒")
    )

    override suspend fun getProductByBarcode(barcode: String): ProductInfo? {
        // TODO: 替换为真实的 API 调用或数据库查询
        // 示例：return apiService.getProduct(barcode)
        return mockProducts[barcode]
    }
}
