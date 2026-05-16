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
    
    data class Text(
        override val x: Float,
        override val y: Float,
        val text: String,
        val fontSize: Float = 12f,
        val isBold: Boolean = false
    ) : LabelElement()
    
    data class Barcode(
        override val x: Float,
        override val y: Float,
        val content: String,
        val format: BarcodeFormat = BarcodeFormat.CODE128,
        val height: Float = 20f,
        val widthMm: Float = 40f  // 条码宽度（mm）
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
    Pending,
    Printing,
    Completed,
    Failed
}
