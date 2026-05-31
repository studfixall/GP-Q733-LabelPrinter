package com.gp.q733.domain.model

import com.google.zxing.BarcodeFormat as ZXingFormat

data class Label(
    val id: String = System.currentTimeMillis().toString(),
    val elements: List<LabelElement> = emptyList(),
    val widthMm: Float = 50f,
    val heightMm: Float = 25f
)

sealed class LabelElement {
    abstract val x: Float
    abstract val y: Float
    /**
     * 文本元素
     * @param textName Barsoft 数据绑定字段名（如 name/price/spec/unit/area/barcode），空字符串表示固定文本
     * @param variable 0=固定文本, 1=数据源绑定
     * @param widthMm 元素宽度(mm)
     * @param heightMm 元素高度(mm)
     * @param fontId 字体ID（Barsoft: 0=默认, 2=宋体, 4=SDK内置, 或字体名称如"微软雅黑"）
     * @param align 对齐方式: 0=LEFT, 1=CENTER, 2=RIGHT
     * @param isUnderline 是否下划线
     * @param rotation 旋转角度: 0/90/180/270
     * @param scale 缩放比例
     */
    data class Text(
        override val x: Float,
        override val y: Float,
        val text: String,
        val fontSize: Float = 12f,
        val isBold: Boolean = false,
        val textName: String = "",
        val variable: Int = 0,
        val widthMm: Float = 0f,
        val heightMm: Float = 0f,
        val fontId: String = "0",
        val align: Int = 0,
        val isUnderline: Boolean = false,
        val rotation: Int = 0,
        val scale: Float = 1f
    ) : LabelElement()
    /**
     * 条码元素
     * @param textName Barsoft 数据绑定字段名
     * @param variable 0=固定内容, 1=数据源绑定
     * @param minBarWidth 最小窄条宽度(dots)
     * @param textPosition 条码文字位置: 0=下方, 1=上方
     */
    data class Barcode(
        override val x: Float,
        override val y: Float,
        val content: String,
        val format: BarcodeFormat = BarcodeFormat.CODE128,
        val height: Float = 20f,
        val widthMm: Float = 40f,
        val textName: String = "",
        val variable: Int = 0,
        val minBarWidth: Int = 2,
        val textPosition: Int = 0
    ) : LabelElement()
    data class QRCode(
        override val x: Float,
        override val y: Float,
        val content: String,
        val size: Float = 20f
    ) : LabelElement()
    data class Line(
        override val x: Float,
        override val y: Float,
        val width: Float,
        val height: Float
    ) : LabelElement()
}

enum class BarcodeFormat(val tsplName: String, val cpclName: String) {
    CODE128("128", "128"),
    CODE39("39", "39"),
    EAN13("EAN13", "EAN13");
    companion object {
        fun fromZXing(format: ZXingFormat): BarcodeFormat = when (format) {
            ZXingFormat.CODE_128 -> CODE128
            ZXingFormat.CODE_39 -> CODE39
            ZXingFormat.EAN_13 -> EAN13
            else -> CODE128
        }
    }
}

data class PrintJob(
    val label: Label,
    val copies: Int = 1,
    val status: PrintStatus = PrintStatus.Pending
)

enum class PrintStatus {
    Pending, Printing, Completed, Failed
}
