package com.gp.q733.domain.util

import com.gp.q733.domain.model.BarcodeFormat
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import android.content.Context
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Barsoft XML 标签模板解析器
 *
 * 解析 Barsoft 原版 XML 模板格式，转换为项目内部的 Label 数据模型。
 * XML 格式示例：
 * <Barsoft Version="1.0" width="40" height="30" gap="0" speed="3" density="15">
 *   <items>
 *     <item viewtype="0" textName="name" text="商品名" left="2.8" top="1.2" width="34" height="5.2" textsize="23" font="0" align="ALIGN_NORMAL" variable="0" />
 *     <item viewtype="1" textName="barcode" text="6901234567890" format="CODE_128" left="2.4" top="16.9" width="34.3" height="5.1" MinBarWidth="5" textposition="0" />
 *   </items>
 * </Barsoft>
 *
 * textName 字段映射（Barsoft 数据绑定体系）：
 * - name → 商品名称
 * - price → 价格
 * - mprice → 会员价
 * - spec → 规格
 * - unit → 单位
 * - area / aera → 产地（aera 是 Barsoft 的拼写错误，兼容处理）
 * - barcode → 条码
 * - 其他值 → 固定文本/占位提示
 */
object BarsoftTemplateParser {

    /**
     * 解析 Barsoft XML 模板
     * @param inputStream XML 文件输入流
     * @return 解析后的 Label 对象
     */
    /**
     * 从 assets 加载并解析 Barsoft XML 模板
     * @param context Android Context
     * @param assetPath assets 下的完整路径，如 "templates/Templet/normal/4030.xml"
     * @return 解析后的 Label 对象，失败返回 null
     */
    fun loadFromAssets(context: Context, assetPath: String): Label? {
        return try {
            context.assets.open(assetPath).use { parse(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun parse(inputStream: InputStream): Label {
        val items = mutableListOf<LabelElement>()
        var widthMm = 40f
        var heightMm = 30f
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(InputStreamReader(inputStream, "UTF-8"))
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Barsoft" -> {
                            // 解析根节点属性
                            widthMm = parser.getAttributeValue(null, "width")?.toFloatOrNull() ?: 40f
                            heightMm = parser.getAttributeValue(null, "height")?.toFloatOrNull() ?: 30f
                        }
                        "item" -> {
                            val element = parseItem(parser)
                            if (element != null) {
                                items.add(element)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return Label(
            elements = items,
            widthMm = widthMm,
            heightMm = heightMm
        )
    }

    /**
     * 解析单个 item 节点
     */
    private fun parseItem(parser: XmlPullParser): LabelElement? {
        val viewtype = parser.getAttributeValue(null, "viewtype")?.toIntOrNull() ?: return null
        val left = parser.getAttributeValue(null, "left")?.toFloatOrNull() ?: 0f
        val top = parser.getAttributeValue(null, "top")?.toFloatOrNull() ?: 0f
        val width = parser.getAttributeValue(null, "width")?.toFloatOrNull() ?: 0f
        val height = parser.getAttributeValue(null, "height")?.toFloatOrNull() ?: 0f
        val textName = parser.getAttributeValue(null, "textName") ?: ""
        val text = parser.getAttributeValue(null, "text") ?: ""
        val textsize = parser.getAttributeValue(null, "textsize")?.toFloatOrNull() ?: 12f
        val font = parser.getAttributeValue(null, "font") ?: "0"
        val isBold = parser.getAttributeValue(null, "fontstyle")?.let {
            it == "1" || it.equals("true", ignoreCase = true)
        } ?: false
        val isUnderline = parser.getAttributeValue(null, "underline")?.toBooleanStrictOrNull() ?: false
        val align = parseAlign(parser.getAttributeValue(null, "align"))
        val rotation = parser.getAttributeValue(null, "rotation")?.toIntOrNull() ?: 0
        val scaleX = parser.getAttributeValue(null, "scale")?.toFloatOrNull() ?: 1f
        val variable = parser.getAttributeValue(null, "variable")?.toIntOrNull() ?: 0

        return when (viewtype) {
            0 -> LabelElement.Text(
                x = left,
                y = top,
                text = text,
                fontSize = textsize,
                isBold = isBold,
                textName = textName,
                variable = variable,
                widthMm = width,
                heightMm = height,
                fontId = font,
                align = align,
                isUnderline = isUnderline,
                rotation = rotation,
                scale = scaleX
            )
            1 -> {
                val format = parseBarcodeFormat(parser.getAttributeValue(null, "format"))
                val minBarWidth = parser.getAttributeValue(null, "MinBarWidth")?.toIntOrNull() ?: 2
                val textPosition = parser.getAttributeValue(null, "textposition")?.toIntOrNull() ?: 0
                LabelElement.Barcode(
                    x = left,
                    y = top,
                    content = text,
                    format = format,
                    height = height,
                    widthMm = width,
                    textName = textName,
                    variable = variable,
                    minBarWidth = minBarWidth,
                    textPosition = textPosition
                )
            }
            2 -> LabelElement.Line(
                x = left,
                y = top,
                width = width,
                height = height
            )
            else -> null
        }
    }

    /**
     * 解析对齐方式
     */
    private fun parseAlign(value: String?): Int {
        return when (value) {
            "ALIGN_CENTER", "ALIGN_MIDDLE" -> 1  // CENTER
            "ALIGN_RIGHT" -> 2                    // RIGHT
            else -> 0                             // LEFT / ALIGN_NORMAL
        }
    }

    /**
     * 解析条码格式
     */
    private fun parseBarcodeFormat(value: String?): BarcodeFormat {
        return when (value) {
            "CODE_128", "CODE128" -> BarcodeFormat.CODE128
            "CODE_39", "CODE39" -> BarcodeFormat.CODE39
            "EAN_13", "EAN13" -> BarcodeFormat.EAN13
            else -> BarcodeFormat.CODE128
        }
    }

    /**
     * Barsoft textName → ProductInfo 字段映射
     * 用于将模板中的数据绑定字段映射到实际商品数据
     */
    fun mapTextNameToField(textName: String): BarsoftFieldName? {
        return when (textName.lowercase().trim()) {
            "name" -> BarsoftFieldName.NAME
            "price" -> BarsoftFieldName.PRICE
            "mprice" -> BarsoftFieldName.MPRICE
            "spec", "sepc" -> BarsoftFieldName.SPEC  // sepc 是 Barsoft 拼写错误
            "unit" -> BarsoftFieldName.UNIT
            "area", "aera" -> BarsoftFieldName.AREA  // aera 是 Barsoft 拼写错误
            "barcode" -> BarsoftFieldName.BARCODE
            else -> null
        }
    }

    /**
     * 判断 textName 是否为数据绑定字段（非固定文本/占位提示）
     */
    fun isDataBindingField(textName: String): Boolean {
        return mapTextNameToField(textName) != null
    }
}

/**
 * Barsoft textName 字段名枚举
 */
enum class BarsoftFieldName {
    NAME,       // 商品名称
    PRICE,      // 价格
    MPRICE,     // 会员价
    SPEC,       // 规格
    UNIT,       // 单位
    AREA,       // 产地
    BARCODE     // 条码
}
