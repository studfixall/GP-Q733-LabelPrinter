package com.gp.q733.domain.util

import com.gp.q733.domain.model.BarcodeFormat
import com.gp.q733.domain.model.LabelElement
import org.json.JSONArray
import org.json.JSONObject

/**
 * Label元素 JSON 序列化/反序列化
 * 用于 Room 数据库存储自定义模板
 */
object TemplateJsonParser {

    fun toJson(elements: List<LabelElement>): String {
        val arr = JSONArray()
        for (el in elements) {
            val obj = JSONObject()
            when (el) {
                is LabelElement.Text -> {
                    obj.put("type", "text")
                    obj.put("x", el.x)
                    obj.put("y", el.y)
                    obj.put("text", el.text)
                    obj.put("fontSize", el.fontSize)
                    obj.put("isBold", el.isBold)
                    obj.put("textName", el.textName)
                    obj.put("variable", el.variable)
                    obj.put("widthMm", el.widthMm)
                    obj.put("heightMm", el.heightMm)
                    obj.put("fontId", el.fontId)
                    obj.put("align", el.align)
                    obj.put("isUnderline", el.isUnderline)
                    obj.put("rotation", el.rotation)
                    obj.put("scale", el.scale)
                }
                is LabelElement.Barcode -> {
                    obj.put("type", "barcode")
                    obj.put("x", el.x)
                    obj.put("y", el.y)
                    obj.put("content", el.content)
                    obj.put("format", el.format.name)
                    obj.put("height", el.height)
                    obj.put("widthMm", el.widthMm)
                    obj.put("textName", el.textName)
                    obj.put("variable", el.variable)
                    obj.put("minBarWidth", el.minBarWidth)
                    obj.put("textPosition", el.textPosition)
                }
                is LabelElement.QRCode -> {
                    obj.put("type", "qrcode")
                    obj.put("x", el.x)
                    obj.put("y", el.y)
                    obj.put("content", el.content)
                    obj.put("size", el.size)
                }
                is LabelElement.Line -> {
                    obj.put("type", "line")
                    obj.put("x", el.x)
                    obj.put("y", el.y)
                    obj.put("width", el.width)
                    obj.put("height", el.height)
                }
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun fromJson(json: String): List<LabelElement> {
        val arr = JSONArray(json)
        val elements = mutableListOf<LabelElement>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            when (obj.getString("type")) {
                "text" -> elements.add(LabelElement.Text(
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat(),
                    text = obj.optString("text", ""),
                    fontSize = obj.optDouble("fontSize", 12.0).toFloat(),
                    isBold = obj.optBoolean("isBold", false),
                    textName = obj.optString("textName", ""),
                    variable = obj.optInt("variable", 0),
                    widthMm = obj.optDouble("widthMm", 0.0).toFloat(),
                    heightMm = obj.optDouble("heightMm", 0.0).toFloat(),
                    fontId = obj.optString("fontId", "0"),
                    align = obj.optInt("align", 0),
                    isUnderline = obj.optBoolean("isUnderline", false),
                    rotation = obj.optInt("rotation", 0),
                    scale = obj.optDouble("scale", 1.0).toFloat()
                ))
                "barcode" -> {
                    val formatStr = obj.optString("format", "CODE128")
                    val format = try { BarcodeFormat.valueOf(formatStr) } catch (_: Exception) { BarcodeFormat.CODE128 }
                    elements.add(LabelElement.Barcode(
                        x = obj.getDouble("x").toFloat(),
                        y = obj.getDouble("y").toFloat(),
                        content = obj.optString("content", ""),
                        format = format,
                        height = obj.optDouble("height", 8.0).toFloat(),
                        widthMm = obj.optDouble("widthMm", 0.0).toFloat(),
                        textName = obj.optString("textName", ""),
                        variable = obj.optInt("variable", 0),
                        minBarWidth = obj.optInt("minBarWidth", 2),
                        textPosition = obj.optInt("textPosition", 0)
                    ))
                }
                "qrcode" -> elements.add(LabelElement.QRCode(
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat(),
                    content = obj.optString("content", ""),
                    size = obj.optDouble("size", 20.0).toFloat()
                ))
                "line" -> elements.add(LabelElement.Line(
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat(),
                    width = obj.optDouble("width", 10.0).toFloat(),
                    height = obj.optDouble("height", 0.5).toFloat()
                ))
            }
        }
        return elements
    }
}
