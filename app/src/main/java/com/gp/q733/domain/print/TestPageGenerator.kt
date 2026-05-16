package com.gp.q733.domain.print

import com.gp.q733.domain.model.*

object TestPageGenerator {
    
    fun generate(
        protocol: PrintProtocol, 
        deviceName: String, 
        density: Int = 8, 
        speed: Int = 4, 
        gapMm: Float = 2f,
        labelWidthMm: Float = 50f,
        labelHeightMm: Float = 40f
    ): ByteArray {
        // Fixed layout parameters
        val marginX = 3f
        val marginY = 3f
        val lineHeight = 6f  // Fixed line spacing - increased for better readability
        
        // Font sizes (in mm, converted to CPCL font sizes)
        val fontSizeTitle = 12f   // Large title
        val fontSizeNormal = 8f   // Normal text
        val fontSizeSmall = 6f    // Small text
        
        // Element heights
        val barcodeHeight = 8f
        val qrCodeSize = 12f
        
        // Build layout from top to bottom
        var currentY = marginY
        val elements = mutableListOf<LabelElement>()
        
        // 1. Title
        elements.add(LabelElement.Text(
            x = marginX,
            y = currentY,
            text = "GP-Q733 测试页",
            fontSize = fontSizeTitle,
            isBold = true
        ))
        currentY += fontSizeTitle + 4f
        
        // 2. Device info
        elements.add(LabelElement.Text(
            x = marginX,
            y = currentY,
            text = "设备: $deviceName",
            fontSize = fontSizeNormal,
            isBold = false
        ))
        currentY += lineHeight + 2f
        
        // 3. Protocol info
        elements.add(LabelElement.Text(
            x = marginX,
            y = currentY,
            text = "协议: ${protocol.name}",
            fontSize = fontSizeNormal,
            isBold = false
        ))
        currentY += lineHeight + 2f
        
        // 4. Label size info
        elements.add(LabelElement.Text(
            x = marginX,
            y = currentY,
            text = "标签: ${labelWidthMm.toInt()}x${labelHeightMm.toInt()}mm",
            fontSize = fontSizeNormal,
            isBold = false
        ))
        currentY += lineHeight + 4f
        
        // 5. Barcode section (left side)
        val sectionWidth = (labelWidthMm - marginX * 2f) / 2f
        val barcodeSectionY = currentY
        
        elements.add(LabelElement.Text(
            x = marginX,
            y = barcodeSectionY,
            text = "条码:",
            fontSize = fontSizeSmall,
            isBold = false
        ))
        
        elements.add(LabelElement.Text(
            x = marginX + sectionWidth + 2f,
            y = barcodeSectionY,
            text = "二维码:",
            fontSize = fontSizeSmall,
            isBold = false
        ))
        
        currentY = barcodeSectionY + fontSizeSmall + 1f
        
        elements.add(LabelElement.Barcode(
            x = marginX,
            y = currentY,
            content = "TEST123456",
            format = BarcodeFormat.CODE128,
            height = barcodeHeight
        ))
        
        elements.add(LabelElement.QRCode(
            x = marginX + sectionWidth + 2f,
            y = currentY,
            content = "https://www.example.com",
            size = qrCodeSize
        ))
        
        // Move Y past the larger of barcode and QR
        currentY += maxOf(barcodeHeight, qrCodeSize) + 3f
        
        // 7. Line separator
        elements.add(LabelElement.Line(
            x = marginX,
            y = currentY,
            width = labelWidthMm - marginX * 2f,
            height = 0.5f
        ))
        currentY += 2f
        
        // 8. Timestamp at bottom
        elements.add(LabelElement.Text(
            x = marginX,
            y = currentY,
            text = "时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}",
            fontSize = fontSizeSmall,
            isBold = false
        ))
        
        val testLabel = Label(
            id = "test_page",
            elements = elements,
            widthMm = labelWidthMm,
            heightMm = labelHeightMm
        )
        
        return protocol.generate(testLabel, density, speed, gapMm)
    }
    
    private val PrintProtocol.name: String
        get() = when (this) {
            PrintProtocol.TSPL -> "TSPL"
            PrintProtocol.CPCL -> "CPCL"
            PrintProtocol.ESCPOS -> "ESC/POS"
        }
}
