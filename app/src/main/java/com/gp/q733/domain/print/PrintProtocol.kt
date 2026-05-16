package com.gp.q733.domain.print

import com.gp.q733.domain.model.*
import java.nio.charset.Charset

/**
 * GP-Q733 打印机默认分辨率: 203 DPI
 * 1mm = 203/25.4 ≈ 8 dots
 */
private const val DPI = 203f
private const val MM_TO_DOTS = DPI / 25.4f

private fun Float.mmToDots(): Int = (this * MM_TO_DOTS).toInt()

/** GP-Q733 打印机使用 GBK 编码处理中文 */
private val PRINTER_CHARSET: Charset = try {
    Charset.forName("GBK")
} catch (_: Exception) {
    Charsets.UTF_8
}

sealed class PrintProtocol {
    abstract fun generate(label: Label, density: Int = 8, speed: Int = 4, gapMm: Float = 2f): ByteArray

    data object TSPL : PrintProtocol() {
        override fun generate(label: Label, density: Int, speed: Int, gapMm: Float): ByteArray {
            val sb = StringBuilder()

            // Printer setup
            sb.appendLine("SIZE ${label.widthMm.toInt()}mm,${label.heightMm.toInt()}mm")
            sb.appendLine("GAP ${gapMm.toInt()}mm,0")
            sb.appendLine("DIRECTION 1")
            sb.appendLine("REFERENCE 0,0")
            sb.appendLine("CODEPAGE 936")
            sb.appendLine("DENSITY $density")
            sb.appendLine("SPEED $speed")
            sb.appendLine("SET TEAR OFF")
            sb.appendLine("CLS")

            label.elements.forEach { element ->
                when (element) {
                    is LabelElement.Text -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val fontHeight = element.fontSize.mmToDots().coerceIn(12, 96)
                        // TSPL font: TSS24.BF2 (24x24 Chinese), TSS16.BF2 (16x16)
                        // GP-Q733 简体中文需要 TST24.BF2（非 TSS24.BF2 繁体）
                        val fontName = if (fontHeight >= 24) "TST24.BF2" else "TST16.BF2"
                        val mul = (fontHeight / 24).coerceIn(1, 8)
                        // TSPL TEXT format: TEXT x,y,"font",rotation,xmul,ymul,"content"
                        sb.appendLine("TEXT $x,$y,\"$fontName\",0,$mul,$mul,\"${element.text}\"")
                    }
                    is LabelElement.Barcode -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val h = element.height.mmToDots().coerceIn(10, 320)
                        sb.appendLine("BARCODE $x,$y,\"${element.format.tsplName}\",$h,1,0,2,2,\"${element.content}\"")
                    }
                    is LabelElement.QRCode -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val qrSize = element.size.mmToDots().coerceIn(1, 32)
                        sb.appendLine("QRCODE $x,$y,L,$qrSize,A,0,\"${element.content}\"")
                    }
                    is LabelElement.Line -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val w = element.width.mmToDots()
                        val h = element.height.mmToDots().coerceIn(1, 10)
                        sb.appendLine("BAR $x,$y,$w,$h")
                    }
                }
            }

            sb.appendLine("PRINT 1,1")
            sb.appendLine("END")
            return sb.toString().toByteArray(PRINTER_CHARSET)
        }
    }

    data object CPCL : PrintProtocol() {
        override fun generate(label: Label, density: Int, speed: Int, gapMm: Float): ByteArray {
            val widthDots = label.widthMm.mmToDots()
            val heightDots = label.heightMm.mmToDots()
            val gapDots = gapMm.mmToDots()

            val baos = java.io.ByteArrayOutputStream()
            
            // CPCL header - using actual DPI (203 for GP-Q733)
            // ! 0 width height labelQty
            // Note: For gap paper, do NOT add gap to height - CPCL handles gaps separately
            baos.write("! 0 $widthDots $heightDots 1\r\n".toByteArray(PRINTER_CHARSET))
            baos.write("PAGE-WIDTH $widthDots\r\n".toByteArray(PRINTER_CHARSET))
            baos.write("SETMAG 1 1\r\n".toByteArray(PRINTER_CHARSET))
            baos.write("CONTRAST $density\r\n".toByteArray(PRINTER_CHARSET))

            label.elements.forEach { element ->
                when (element) {
                    is LabelElement.Text -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        // CPCL font sizes: 0-6 predefined
                        // 0=16pt, 1=20pt, 2=24pt, 3=28pt, 4=32pt, 5=36pt, 6=40pt
                        val fontSize = when {
                            element.fontSize >= 10f -> 4
                            element.fontSize >= 8f -> 3
                            element.fontSize >= 6f -> 2
                            element.fontSize >= 4f -> 1
                            else -> 0
                        }
                        
                        // Check if text contains Chinese characters
                        val hasChinese = element.text.any { it.code > 0x4E00 && it.code < 0x9FFF }
                        
                        baos.write("SETBOLD ${if (element.isBold) 1 else 0}\r\n".toByteArray(PRINTER_CHARSET))
                        
                        // Always use font 55 for Chinese support on GP printers
                        // Font 55 is the internal Chinese font
                        val font = if (hasChinese) 55 else fontSize
                        baos.write("TEXT $x $y $font ".toByteArray(PRINTER_CHARSET))
                        baos.write(element.text.toByteArray(PRINTER_CHARSET))
                        baos.write("\r\n".toByteArray(PRINTER_CHARSET))
                    }
                    is LabelElement.Barcode -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val h = element.height.mmToDots()
                        val w = element.widthMm.mmToDots()
                        // ratio 参数控制条码宽度 (1-10)，根据宽度计算
                        val ratio = (w / 100).coerceIn(1, 10)
                        // BARCODE type x y ratio height content
                        baos.write("BARCODE ${element.format.cpclName} $x $y $ratio $h ${element.content}\r\n".toByteArray(PRINTER_CHARSET))
                    }
                    is LabelElement.QRCode -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val qrSize = element.size.mmToDots().coerceIn(1, 32)
                        // QR Code: BARCODE QR x y M errorCorrection U size
                        baos.write("BARCODE QR $x $y M 2 U $qrSize\r\n".toByteArray(PRINTER_CHARSET))
                        baos.write("MA,".toByteArray(PRINTER_CHARSET))
                        baos.write(element.content.toByteArray(PRINTER_CHARSET))
                        baos.write("\r\nENDQR\r\n".toByteArray(PRINTER_CHARSET))
                    }
                    is LabelElement.Line -> {
                        val x1 = element.x.mmToDots()
                        val y1 = element.y.mmToDots()
                        val x2 = (element.x + element.width).mmToDots()
                        val y2 = (element.y + element.height).mmToDots()
                        baos.write("BOX $x1 $y1 $x2 $y2 1\r\n".toByteArray(PRINTER_CHARSET))
                    }
                }
            }

            // CPCL ending - try PRINT without FORM to avoid double feeding
            baos.write("PRINT\r\n".toByteArray(PRINTER_CHARSET))
            return baos.toByteArray()
        }
    }

    data object ESCPOS : PrintProtocol() {
        override fun generate(label: Label, density: Int, speed: Int, gapMm: Float): ByteArray {
            val baos = java.io.ByteArrayOutputStream()

            // Initialize
            baos.write(byteArrayOf(0x1B, 0x40))
            // Set print density
            baos.write(byteArrayOf(0x1D, 0x28, 0x4B, 0x02, 0x00, 0x30, density.toByte()))

            label.elements.forEach { element ->
                when (element) {
                    is LabelElement.Text -> {
                        baos.write(byteArrayOf(0x1B, 0x21, if (element.isBold) 0x08 else 0x00))
                        val size = (element.fontSize / 12f).toInt().coerceIn(1, 4)
                        baos.write(byteArrayOf(0x1D, 0x21, (((size - 1) shl 4) or (size - 1)).toByte()))
                        baos.write(element.text.toByteArray(Charsets.UTF_8))
                        baos.write('\n'.code)
                    }
                    is LabelElement.Barcode -> {
                        baos.write(byteArrayOf(0x1D, 0x6B))
                        when (element.format) {
                            BarcodeFormat.CODE128 -> baos.write(73)
                            BarcodeFormat.CODE39 -> baos.write(69)
                            BarcodeFormat.EAN13 -> baos.write(67)
                            else -> baos.write(73)
                        }
                        baos.write(element.content.length)
                        baos.write(element.content.toByteArray(Charsets.UTF_8))
                        baos.write(0)
                        baos.write('\n'.code)
                    }
                    is LabelElement.QRCode -> {
                        val content = element.content.toByteArray(Charsets.UTF_8)
                        val size = element.size.toInt().coerceIn(1, 16)
                        baos.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
                        baos.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.toByte()))
                        baos.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30))
                        val pL = (content.size + 3) and 0xFF
                        val pH = ((content.size + 3) shr 8) and 0xFF
                        baos.write(byteArrayOf(0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30))
                        baos.write(content)
                        baos.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
                        baos.write('\n'.code)
                    }
                    is LabelElement.Line -> {
                        repeat(element.width.toInt() / 8) {
                            baos.write(0xFF)
                        }
                        baos.write(0x0A)
                    }
                }
            }

            // Feed and cut
            baos.write(byteArrayOf(0x1B, 0x64, 0x03)) // Feed 3 lines
            baos.write(byteArrayOf(0x1D, 0x56, 0x01)) // Cut
            return baos.toByteArray()
        }
    }
}
