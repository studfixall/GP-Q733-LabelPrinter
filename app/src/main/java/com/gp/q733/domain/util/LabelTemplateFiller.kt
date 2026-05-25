package com.gp.q733.domain.util

import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.model.TemplateFields

/**
 * 标签模板填充工具
 * 将商品信息填充到标签模板的占位符字段中
 *
 * 支持两种填充模式：
 * 1. 占位符模式：文本中包含 {productName}、{barcode}、{price} 等占位符
 * 2. Barsoft textName 模式：通过 LabelElement.Text.textName 字段映射数据
 */
object LabelTemplateFiller {

    /**
     * 填充标签模板
     * 将模板中所有占位符/数据绑定字段替换为实际商品数据
     *
     * @param template 包含占位符的标签模板
     * @param product 商品信息
     * @return 填充后的标签
     */
    fun fillTemplate(template: Label, product: ProductInfo): Label {
        val filledElements = template.elements.map { element ->
            when (element) {
                is LabelElement.Text -> {
                    // 优先使用 Barsoft textName 数据绑定
                    val filledText = if (element.textName.isNotEmpty() &&
                        BarsoftTemplateParser.isDataBindingField(element.textName)
                    ) {
                        // 先解析 textName 得到商品字段值，再做占位符替换（支持嵌套场景）
                        val resolved = resolveTextName(element.textName, product)
                        fillText(resolved, product)
                    } else {
                        fillText(element.text, product)
                    }
                    element.copy(text = filledText)
                }
                is LabelElement.Barcode -> {
                    // variable=1: 通过 textName 泛化解析商品字段值
                    // variable=0: 保持固定 content 不变
                    if (element.variable == 1 && element.textName.isNotEmpty()) {
                        element.copy(content = resolveTextName(element.textName, product))
                    } else {
                        element // 保持原 content
                    }
                }
                is LabelElement.QRCode -> {
                    element.copy(content = product.barcode)
                }
                is LabelElement.Line -> element
            }
        }
        return template.copy(elements = filledElements)
    }

    /**
     * 根据 Barsoft textName 字段名解析商品数据
     */
    private fun resolveTextName(textName: String, product: ProductInfo): String {
        return when (BarsoftTemplateParser.mapTextNameToField(textName)) {
            BarsoftFieldName.NAME -> product.name
            BarsoftFieldName.PRICE -> String.format("%.2f", product.price)
            BarsoftFieldName.MPRICE -> if (product.mprice > 0) String.format("%.2f", product.mprice) else String.format("%.2f", product.price) // 会员价优先，无则用零售价
            BarsoftFieldName.SPEC -> product.spec
            BarsoftFieldName.UNIT -> product.unit
            BarsoftFieldName.AREA -> product.origin
            BarsoftFieldName.BARCODE -> product.barcode
            null -> "" // 非数据绑定字段，保持原文本
        }
    }

    /**
     * 填充文本中的占位符
     */
    private fun fillText(text: String, product: ProductInfo): String {
        return text
            .replace(TemplateFields.PRODUCT_NAME, product.name)
            .replace(TemplateFields.BARCODE, product.barcode)
            .replace(TemplateFields.PRICE, String.format("%.2f", product.price))
            .replace(TemplateFields.SPEC, product.spec)
            .replace(TemplateFields.UNIT, product.unit)
            .replace(TemplateFields.ORIGIN, product.origin)
    }

    /**
     * 检查文本是否包含占位符
     */
    fun hasPlaceholders(text: String): Boolean {
        return text.contains(TemplateFields.PRODUCT_NAME) ||
            text.contains(TemplateFields.BARCODE) ||
            text.contains(TemplateFields.PRICE) ||
            text.contains(TemplateFields.SPEC) ||
            text.contains(TemplateFields.UNIT) ||
            text.contains(TemplateFields.ORIGIN)
    }

    /**
     * 检查标签是否包含占位符或数据绑定字段
     */
    fun hasPlaceholders(label: Label): Boolean {
        return label.elements.any { element ->
            when (element) {
                is LabelElement.Text -> {
                    hasPlaceholders(element.text) ||
                        (element.textName.isNotEmpty() &&
                            BarsoftTemplateParser.isDataBindingField(element.textName))
                }
                is LabelElement.Barcode -> element.textName == "barcode"
                else -> false
            }
        }
    }
}
