package com.gp.q733.domain.model

/**
 * 商品信息数据模型
 * 用于扫码后从本地数据库查询的商品数据
 */
data class ProductInfo(
    val barcode: String = "",    // 商品条码（扫描得到）
    val name: String = "",       // 商品名称
    val price: Double = 0.0,     // 商品价格
    val mprice: Double = 0.0,    // 会员价（0表示无会员价）
    val spec: String = "",       // 规格（如：330ml、500g）
    val unit: String = "",       // 单位（如：个、瓶、箱）
    val origin: String = "",     // 产地（如：上海、浙江）
    val category: String = ""    // 分类
)

/**
 * 标签模板中的占位符字段定义
 * 用于标识模板中需要动态填充的位置
 */
object TemplateFields {
    const val PRODUCT_NAME = "{productName}"  // 商品名称占位符
    const val BARCODE = "{barcode}"           // 条码占位符
    const val PRICE = "{price}"               // 价格占位符
    const val MPRICE = "{mprice}"             // 会员价占位符
    const val SPEC = "{spec}"                 // 规格占位符
    const val UNIT = "{unit}"                 // 单位占位符
    const val ORIGIN = "{origin}"             // 产地占位符
}
