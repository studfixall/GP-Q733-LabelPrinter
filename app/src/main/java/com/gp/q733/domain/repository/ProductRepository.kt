package com.gp.q733.domain.repository

import com.gp.q733.domain.model.ProductInfo

/**
 * 商品数据仓库接口
 * 提供通过条码获取商品信息的抽象
 */
interface ProductRepository {
    /**
     * 根据条码获取商品信息
     * @param barcode 商品条码
     * @return 商品信息，如果未找到则返回 null
     */
    suspend fun getProductByBarcode(barcode: String): ProductInfo?
}
