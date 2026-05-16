import re

with open('app/src/main/java/com/gp/q733/domain/print/PrintProtocol.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the entire CPCL generate method
old_cpcl = '''    data object CPCL : PrintProtocol() {
        override fun generate(label: Label, density: Int, speed: Int, gapMm: Float): ByteArray {
            val widthDots = label.widthMm.mmToDots()
            val heightDots = label.heightMm.mmToDots()
            val gapDots = gapMm.mmToDots()

            val baos = java.io.ByteArrayOutputStream()
            
            // CPCL header - using actual DPI (203 for GP-Q733)
            // ! 0 width height labelQty
            // For gap paper, height should include the gap
            val totalHeight = heightDots + gapDots
            baos.write("! 0 $widthDots $totalHeight 1\\r\\n".toByteArray(PRINTER_CHARSET))
            baos.write("PAGE-WIDTH $widthDots\\r\\n".toByteArray(PRINTER_CHARSET))
            // Enable UTF-8 for Chinese support
            baos.write("! U1 SETCODEPAGE UTF-8\\r\\n".toByteArray(Charsets.UTF_8))
            baos.write("SETMAG 1 1\\r\\n".toByteArray(PRINTER_CHARSET))
            baos.write("CONTRAST $density\\r\\n".toByteArray(PRINTER_CHARSET))

            label.elements.forEach { element ->
                when (element) {
                    is LabelElement.Text -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        // CPCL font sizes: 0-6 predefined
                        // 0=16pt, 1=20pt, 2=24pt, 3=28pt, 4=32pt, 5=36pt, 6=40pt
                        // Font 55 is typically the Chinese font on GP printers
                        val fontSize = when {
                            element.fontSize >= 10f -> 4
                            element.fontSize >= 8f -> 3
                            element.fontSize >= 6f -> 2
                            element.fontSize >= 4f -> 1
                            else -> 0
                        }
                        
                        // Check if text contains Chinese characters
                        val hasChinese = element.text.any { it.code > 0x4E00 && it.code < 0x9FFF }
                        // Use font 55 for Chinese, standard font for ASCII
                        val font = if (hasChinese) 55 else fontSize
                        
                        baos.write("SETBOLD ${if (element.isBold) 1 else 0}\\r\\n".toByteArray(Charsets.UTF_8))
                        // TEXT x y font data
                        baos.write("TEXT $x $y $font ".toByteArray(Charsets.UTF_8))
                        baos.write(element.text.toByteArray(Charsets.UTF_8))
                        baos.write("\\r\\n".toByteArray(Charsets.UTF_8))
                    }
                    is LabelElement.Barcode -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val h = element.height.mmToDots()
                        val w = element.widthMm.mmToDots()
                        // ratio 参数控制条码宽度 (1-10)，根据宽度计算
                        val ratio = (w / 100).coerceIn(1, 10)
                        // BARCODE type x y ratio height content
                        baos.write("BARCODE ${element.format.cpclName} $x $y $ratio $h ${element.content}\\r\\n".toByteArray(PRINTER_CHARSET))
                    }
                    is LabelElement.QRCode -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val qrSize = element.size.mmToDots().coerceIn(1, 32)
                        // QR Code: BARCODE QR x y M errorCorrection U size
                        baos.write("BARCODE QR $x $y M 2 U $qrSize\\r\\n".toByteArray(PRINTER_CHARSET))
                        baos.write("MA,".toByteArray(PRINTER_CHARSET))
                        baos.write(element.content.toByteArray(PRINTER_CHARSET))
                        baos.write("\\r\\nENDQR\\r\\n".toByteArray(PRINTER_CHARSET))
                    }
                    is LabelElement.Line -> {
                        val x1 = element.x.mmToDots()
                        val y1 = element.y.mmToDots()
                        val x2 = (element.x + element.width).mmToDots()
                        val y2 = (element.y + element.height).mmToDots()
                        baos.write("BOX $x1 $y1 $x2 $y2 1\\r\\n".toByteArray(PRINTER_CHARSET))
                    }
                }
            }

            // For gap paper, use GAP command before FORM
            if (gapMm > 0) {
                baos.write("GAP $gapDots\\r\\n".toByteArray(PRINTER_CHARSET))
            }
            baos.write("FORM\\r\\n".toByteArray(PRINTER_CHARSET))
            baos.write("PRINT\\r\\n".toByteArray(PRINTER_CHARSET))
            return baos.toByteArray()
        }
    }'''

new_cpcl = '''    data object CPCL : PrintProtocol() {
        override fun generate(label: Label, density: Int, speed: Int, gapMm: Float): ByteArray {
            val widthDots = label.widthMm.mmToDots()
            val heightDots = label.heightMm.mmToDots()
            val gapDots = gapMm.mmToDots()

            val baos = java.io.ByteArrayOutputStream()
            
            // CPCL header - using actual DPI (203 for GP-Q733)
            // ! 0 width height labelQty
            // Note: For gap paper, do NOT add gap to height - CPCL handles gaps separately
            baos.write("! 0 $widthDots $heightDots 1\\r\\n".toByteArray(PRINTER_CHARSET))
            baos.write("PAGE-WIDTH $widthDots\\r\\n".toByteArray(PRINTER_CHARSET))
            baos.write("SETMAG 1 1\\r\\n".toByteArray(PRINTER_CHARSET))
            baos.write("CONTRAST $density\\r\\n".toByteArray(PRINTER_CHARSET))

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
                        
                        baos.write("SETBOLD ${if (element.isBold) 1 else 0}\\r\\n".toByteArray(PRINTER_CHARSET))
                        
                        if (hasChinese) {
                            // Use TTF command for Chinese text with GBK encoding
                            // TTF x y fontSize "text" - uses printer's built-in Chinese font
                            val ttfSize = when {
                                element.fontSize >= 10f -> 24
                                element.fontSize >= 8f -> 20
                                element.fontSize >= 6f -> 16
                                else -> 12
                            }
                            baos.write("TTF $x $y $ttfSize \\\"${element.text}\\\"\\r\\n".toByteArray(PRINTER_CHARSET))
                        } else {
                            // Use standard TEXT command for ASCII
                            baos.write("TEXT $x $y $fontSize ".toByteArray(PRINTER_CHARSET))
                            baos.write(element.text.toByteArray(Charsets.US_ASCII))
                            baos.write("\\r\\n".toByteArray(PRINTER_CHARSET))
                        }
                    }
                    is LabelElement.Barcode -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val h = element.height.mmToDots()
                        val w = element.widthMm.mmToDots()
                        // ratio 参数控制条码宽度 (1-10)，根据宽度计算
                        val ratio = (w / 100).coerceIn(1, 10)
                        // BARCODE type x y ratio height content
                        baos.write("BARCODE ${element.format.cpclName} $x $y $ratio $h ${element.content}\\r\\n".toByteArray(PRINTER_CHARSET))
                    }
                    is LabelElement.QRCode -> {
                        val x = element.x.mmToDots()
                        val y = element.y.mmToDots()
                        val qrSize = element.size.mmToDots().coerceIn(1, 32)
                        // QR Code: BARCODE QR x y M errorCorrection U size
                        baos.write("BARCODE QR $x $y M 2 U $qrSize\\r\\n".toByteArray(PRINTER_CHARSET))
                        baos.write("MA,".toByteArray(PRINTER_CHARSET))
                        baos.write(element.content.toByteArray(PRINTER_CHARSET))
                        baos.write("\\r\\nENDQR\\r\\n".toByteArray(PRINTER_CHARSET))
                    }
                    is LabelElement.Line -> {
                        val x1 = element.x.mmToDots()
                        val y1 = element.y.mmToDots()
                        val x2 = (element.x + element.width).mmToDots()
                        val y2 = (element.y + element.height).mmToDots()
                        baos.write("BOX $x1 $y1 $x2 $y2 1\\r\\n".toByteArray(PRINTER_CHARSET))
                    }
                }
            }

            // CPCL ending - no GAP command here, it causes double feeding
            baos.write("FORM\\r\\n".toByteArray(PRINTER_CHARSET))
            baos.write("PRINT\\r\\n".toByteArray(PRINTER_CHARSET))
            return baos.toByteArray()
        }
    }'''

if old_cpcl in content:
    content = content.replace(old_cpcl, new_cpcl)
    print('Replaced CPCL implementation')
else:
    print('CPCL pattern not found - trying partial replacement')
    # Try simpler replacements
    if 'val totalHeight = heightDots + gapDots' in content:
        content = content.replace('val totalHeight = heightDots + gapDots', '// Page height is just the label height, not including gap')
        content = content.replace('baos.write("! 0 $widthDots $totalHeight', 'baos.write("! 0 $widthDots $heightDots')
        print('Fixed height calculation')
    
    if '! U1 SETCODEPAGE UTF-8' in content:
        content = content.replace('// Enable UTF-8 for Chinese support\n            baos.write("! U1 SETCODEPAGE UTF-8\\r\\n".toByteArray(Charsets.UTF_8))', '// Using GBK encoding for Chinese support')
        print('Removed UTF-8 codepage')
    
    if 'baos.write("GAP $gapDots' in content:
        content = content.replace('// For gap paper, use GAP command before FORM\n            if (gapMm > 0) {\n                baos.write("GAP $gapDots\\r\\n".toByteArray(PRINTER_CHARSET))\n            }', '// GAP command removed - causes double feeding')
        print('Removed GAP command')

with open('app/src/main/java/com/gp/q733/domain/print/PrintProtocol.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print('Done')
