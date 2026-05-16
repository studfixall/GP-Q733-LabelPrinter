package com.gp.q733.domain.util

import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.model.TemplateFields

/**
 * 标签模板填充工具
 * 将商品信息填充到标签模板的占位符字段中
 */
object LabelTemplateFiller {

    /**
     * 填充标签模板
     * 将模板中所有占位符替换为实际商品数据
     * 
     * @param template 包含占位符的标签模板
     * @param product 商品信息
     * @return 填充后的标签
     */
    fun fillTemplate(template: Label, product: ProductInfo): Label {
        val filledElements = template.elements.map { element ->
            when (element) {
                is LabelElement.Text -> {
                    val filledText = fillText(element.text, product)
                    element.copy(text = filledText)
                }
                is LabelElement.Barcode -> {
                    // 条码内容替换为实际条码
                    element.copy(content = product.barcode)
                }
                is LabelElement.QRCode -> {
                    // 二维码内容替换为实际条码
                    element.copy(content = product.barcode)
                }
                is LabelElement.Line -> element
            }
        }
        return template.copy(elements = filledElements)
    }

    /**
     * 填充文本中的占位符
     */
    private fun fillText(text: String, product: ProductInfo): String {
        return text
            .replace(TemplateFields.PRODUCT_NAME, product.name)
            .replace(TemplateFields.BARCODE, product.barcode)
            .replace(TemplateFields.PRICE, "¥${String.format("%.2f", product.price)}")
    }

    /**
     * 检查文本是否包含占位符
     */
    fun hasPlaceholders(text: String): Boolean {
        return text.contains(TemplateFields.PRODUCT_NAME) ||
               text.contains(TemplateFields.BARCODE) ||
               text.contains(TemplateFields.PRICE)
    }

    /**
     * 检查标签是否包含占位符
     */
    fun hasPlaceholders(label: Label): Boolean {
        return label.elements.any { element ->
            when (element) {
                is LabelElement.Text -> hasPlaceholders(element.text)
                else -> false
            }
        }
    }
}
