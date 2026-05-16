package com.gp.q733.domain.model

/**
 * 商品信息数据模型
 * 用于扫码后从数据库/API获取的商品数据
 */
data class ProductInfo(
    val barcode: String,      // 商品条码（扫描得到）
    val name: String,         // 商品名称
    val price: Double         // 商品价格
)

/**
 * 标签模板中的占位符字段定义
 * 用于标识模板中需要动态填充的位置
 */
object TemplateFields {
    const val PRODUCT_NAME = "{productName}"  // 商品名称占位符
    const val BARCODE = "{barcode}"           // 条码占位符
    const val PRICE = "{price}"               // 价格占位符
}
