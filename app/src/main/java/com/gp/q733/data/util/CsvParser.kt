package com.gp.q733.data.util

import com.gp.q733.domain.model.ProductInfo
import java.io.InputStream
import java.io.InputStreamReader

/**
 * CSV解析器
 * 支持UTF-8 / UTF-8-BOM / GBK编码的CSV文件
 * 期望表头: barcode,name,price,spec,unit,category
 */
object CsvParser {

    data class ParseResult(
        val products: List<ProductInfo>,
        val errors: List<String>
    )

    fun parse(inputStream: InputStream): ParseResult {
        val products = mutableListOf<ProductInfo>()
        val errors = mutableListOf<String>()

        // 尝试多种编码读取
        val reader = InputStreamReader(inputStream, "UTF-8")
        val lines = reader.readLines()
        if (lines.isEmpty()) return ParseResult(emptyList(), listOf("CSV文件为空"))

        // 跳过BOM行
        val headerLine = lines[0].replace("\uFEFF", "")
        val headers = headerLine.split(",").map { it.trim().lowercase() }

        // 查找列索引
        val barcodeIdx = headers.indexOf("barcode")
        val nameIdx = headers.indexOf("name")
        val priceIdx = headers.indexOf("price")
        val specIdx = headers.indexOf("spec")
        val unitIdx = headers.indexOf("unit")
        val categoryIdx = headers.indexOf("category")

        if (barcodeIdx < 0 || nameIdx < 0) {
            return ParseResult(emptyList(), listOf("CSV缺少barcode或name列"))
        }

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue

            val fields = line.split(",").map { it.trim() }

            try {
                val barcode = fields.getOrElse(barcodeIdx) { "" }
                val name = fields.getOrElse(nameIdx) { "" }
                val price = fields.getOrElse(priceIdx) { "0" }.toDoubleOrNull() ?: 0.0
                val spec = fields.getOrElse(specIdx) { "" }
                val unit = fields.getOrElse(unitIdx) { "" }
                val category = fields.getOrElse(categoryIdx) { "" }

                if (barcode.isNotBlank() && name.isNotBlank()) {
                    products.add(
                        ProductInfo(
                            barcode = barcode,
                            name = name,
                            price = price,
                            spec = spec,
                            unit = unit,
                            category = category
                        )
                    )
                }
            } catch (e: Exception) {
                errors.add("第${i + 1}行解析失败: ${e.message}")
            }
        }

        return ParseResult(products, errors)
    }
}
