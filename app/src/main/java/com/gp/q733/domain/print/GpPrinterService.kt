package com.gp.q733.domain.print

import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.rt.printerlibrary.bean.LableSizeBean
import com.rt.printerlibrary.bean.Position
import com.rt.printerlibrary.cmd.Cmd
import com.rt.printerlibrary.cmd.CpclCmd
import com.rt.printerlibrary.cmd.CpclFactory
import com.rt.printerlibrary.cmd.EscFactory
import com.rt.printerlibrary.cmd.TsplFactory
import com.rt.printerlibrary.enumerate.BaseEnum
import com.rt.printerlibrary.enumerate.BarcodeStringPosition
import com.rt.printerlibrary.enumerate.BarcodeType
import com.rt.printerlibrary.enumerate.CommonEnum
import com.rt.printerlibrary.enumerate.CpclFontTypeEnum
import com.rt.printerlibrary.enumerate.PrintDirection
import com.rt.printerlibrary.enumerate.PrintRotation
import com.rt.printerlibrary.enumerate.SettingEnum
import com.rt.printerlibrary.enumerate.SpeedEnum
import com.rt.printerlibrary.enumerate.TsplFontTypeEnum
import com.rt.printerlibrary.exception.SdkException
import com.rt.printerlibrary.setting.BarcodeSetting
import com.rt.printerlibrary.setting.CommonSetting
import com.rt.printerlibrary.setting.TextSetting
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 打印命令生成服务
 * 
 * 架构变更：此服务只负责生成打印命令字节数组，不再管理蓝牙连接。
 * 蓝牙连接统一由 BluetoothRepository 管理（socket 直连，稳定可靠）。
 * 
 * 日志分析结论：SDK 的 RTPrinter 连接 2-3 秒后自动断开，
 * 导致每次打印都要重连（1.6-4s），且第一次经常超时。
 * 改为 socket 直连 + SDK 命令生成，彻底解决连接不稳定问题。
 */
@Singleton
class GpPrinterService @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        private const val DPI = 203
        private fun mmToDots(mm: Number): Int = (mm.toFloat() * DPI / 25.4f).toInt()
    }

    /** Last connected device MAC address - for reconnection tracking */
    private var lastConnectedMac: String? = null

    fun setLastConnectedMac(mac: String?) {
        lastConnectedMac = mac
    }

    fun getLastConnectedMac(): String? = lastConnectedMac

    /**
     * 设置 CPCL 分辨率 — SDK 默认 200，GP-Q733 是 203 DPI
     */
    private fun setCpclResolution() {
        CpclCmd.Lateral_Resolution = DPI.toString()
        CpclCmd.Vertical_Resolution = DPI.toString()
    }

    /**
     * 生成标签打印命令字节数组
     * 调用方负责通过 BluetoothRepository.write() 发送
     */
    suspend fun generatePrintCommands(label: Label): ByteArray {
        val settings = settingsDataStore.settingsFlow.first()
        val cmdType = when (settings.printProtocol) {
            PrintProtocol.CPCL -> BaseEnum.CMD_CPCL
            PrintProtocol.TSPL -> BaseEnum.CMD_TSPL
            PrintProtocol.ESCPOS -> BaseEnum.CMD_ESC
        }
        val cmd = createPrintCommand(cmdType, label, settings)
        val bytes = cmd.appendCmds
        // Log full command content for debugging
        android.util.Log.d("PrintDebug", "generatePrintCommands: ${bytes.size} bytes")
        // Split into chunks for logcat (max ~4000 chars per log entry)
        val fullText = try { String(bytes, java.nio.charset.Charset.forName("GBK")) } catch (_: Exception) { String(bytes, Charsets.US_ASCII) }
        val readable = fullText.replace("\r", "↵").replace("\n", "↓")
        readable.chunked(3000).forEachIndexed { idx, chunk ->
            android.util.Log.d("PrintDebug", "generatePrintCommands[${idx}]: $chunk")
        }
        return bytes
    }

    /**
     * 生成测试页打印命令字节数组
     * 调用方负责通过 BluetoothRepository.write() 发送
     */
    suspend fun generateTestPageCommands(deviceName: String): ByteArray {
        val settings = settingsDataStore.settingsFlow.first()
        val cmdType = when (settings.printProtocol) {
            PrintProtocol.CPCL -> BaseEnum.CMD_CPCL
            PrintProtocol.TSPL -> BaseEnum.CMD_TSPL
            PrintProtocol.ESCPOS -> BaseEnum.CMD_ESC
        }
        val labelWidth = settings.labelWidth.toInt()
        val labelHeight = settings.labelHeight.toInt()
        val cmd = when (cmdType) {
            BaseEnum.CMD_CPCL -> createCpclTestCommand(deviceName, labelWidth, labelHeight, settings)
            BaseEnum.CMD_TSPL -> createTsplTestCommand(deviceName, labelWidth, labelHeight, settings)
            else -> createEscTestCommand(deviceName, settings)
        }
        android.util.Log.d("PrintDebug", "GpPrinterService.generateTestPageCommands() - protocol: ${settings.printProtocol}, size: ${labelWidth}x${labelHeight}mm")
        val bytes = cmd.appendCmds
        // Log full command content for debugging
        android.util.Log.d("PrintDebug", "generateTestPageCommands: ${bytes.size} bytes")
        val fullText = try { String(bytes, java.nio.charset.Charset.forName("GBK")) } catch (_: Exception) { String(bytes, Charsets.US_ASCII) }
        val readable = fullText.replace("\r", "↵").replace("\n", "↓")
        readable.chunked(3000).forEachIndexed { idx, chunk ->
            android.util.Log.d("PrintDebug", "generateTestPageCommands[${idx}]: $chunk")
        }
        return bytes
    }

    private fun createPrintCommand(cmdType: Int, label: Label, settings: com.gp.q733.data.local.AppSettings): Cmd {
        return when (cmdType) {
            BaseEnum.CMD_CPCL -> createCpclCommand(label, settings)
            BaseEnum.CMD_TSPL -> createTsplCommand(label, settings)
            else -> createEscCommand(label, settings)
        }
    }

    /**
     * CPCL label print
     * getCpclHeaderCmd takes mm values, Position takes dots values
     */
    private fun createCpclCommand(label: Label, settings: com.gp.q733.data.local.AppSettings): Cmd {
        setCpclResolution()
        val factory = CpclFactory()
        val cmd = factory.create()
        val width = settings.labelWidth.toInt()
        val height = settings.labelHeight.toInt()
        val offset = 0
        cmd.append(cmd.getCpclHeaderCmd(width, height, 1, offset))
        val commonSetting = CommonSetting()
        commonSetting.speedEnum = SpeedEnum.getEnumByString(settings.printSpeed.toString())
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        label.elements.forEach { element ->
            when (element) {
                is LabelElement.Text -> {
                    val textSetting = TextSetting()
                    textSetting.cpclFontTypeEnum = CpclFontTypeEnum.Font_Chinese_24x24
                    textSetting.txtPrintPosition = Position(mmToDots(element.x), mmToDots(element.y))
                    textSetting.printRotation = PrintRotation.Rotate0
                    textSetting.setxMultiplication(1)
                    textSetting.setyMultiplication(1)
                    textSetting.bold = if (element.isBold) SettingEnum.Enable else SettingEnum.Disable
                    cmd.append(cmd.getTextCmd(textSetting, element.text, "GBK"))
                }
                is LabelElement.Barcode -> {
                    val barcodeSetting = BarcodeSetting()
                    barcodeSetting.printRotation = PrintRotation.Rotate0
                    barcodeSetting.narrowInDot = 2
                    barcodeSetting.wideInDot = 4
                    barcodeSetting.barcodeStringPosition = BarcodeStringPosition.BELOW_BARCODE
                    barcodeSetting.heightInDot = mmToDots(element.height)
                    barcodeSetting.position = Position(mmToDots(element.x), mmToDots(element.y))
                    val barcodeType = when (element.format) {
                        com.gp.q733.domain.model.BarcodeFormat.CODE128 -> BarcodeType.CODE128
                        com.gp.q733.domain.model.BarcodeFormat.CODE39 -> BarcodeType.CODE39
                        com.gp.q733.domain.model.BarcodeFormat.EAN13 -> BarcodeType.EAN13
                    }
                    try {
                        cmd.append(cmd.getBarcodeCmd(barcodeType, barcodeSetting, element.content))
                    } catch (e: SdkException) {
                        android.util.Log.e("PrintDebug", "CPCL barcode cmd error: ${e.message}")
                    }
                }
                is LabelElement.QRCode -> {
                    val barcodeSetting = BarcodeSetting()
                    barcodeSetting.printRotation = PrintRotation.Rotate0
                    val qrSize = (element.size / 4).toInt().coerceIn(1, 15)
                    barcodeSetting.qrcodeDotSize = qrSize
                    barcodeSetting.position = Position(mmToDots(element.x), mmToDots(element.y))
                    try {
                        cmd.append(cmd.getBarcodeCmd(BarcodeType.QR_CODE, barcodeSetting, element.content))
                    } catch (e: SdkException) {
                        android.util.Log.e("PrintDebug", "CPCL QR cmd error: ${e.message}")
                    }
                }
                is LabelElement.Line -> {
                    val x1 = mmToDots(element.x)
                    val y1 = mmToDots(element.y)
                    val x2 = mmToDots(element.x + element.width)
                    val y2 = mmToDots(element.y + element.height)
                    cmd.append("BOX $x1 $y1 $x2 $y2 1\r\n".toByteArray(Charsets.UTF_8))
                }
            }
        }
        cmd.append(cmd.getEndCmd())
        return cmd
    }

    /**
     * TSPL label print
     */
    private fun createTsplCommand(label: Label, settings: com.gp.q733.data.local.AppSettings): Cmd {
        val factory = TsplFactory()
        val cmd = factory.create()
        val width = settings.labelWidth.toInt()
        val height = settings.labelHeight.toInt()
        val commonSetting = CommonSetting()
        commonSetting.lableSizeBean = LableSizeBean(width, height)
        commonSetting.labelGap = settings.gapMm.toInt()
        commonSetting.printDirection = PrintDirection.NORMAL
        commonSetting.speedEnum = SpeedEnum.getEnumByString(settings.printSpeed.toString())
        cmd.append(cmd.headerCmd)
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        label.elements.forEach { element ->
            when (element) {
                is LabelElement.Text -> {
                    val textSetting = TextSetting()
                    textSetting.tsplFontTypeEnum = TsplFontTypeEnum.Font_TSS24_BF2_For_Simple_Chinese
                    textSetting.txtPrintPosition = Position(mmToDots(element.x), mmToDots(element.y))
                    textSetting.printRotation = PrintRotation.Rotate0
                    textSetting.setxMultiplication(1)
                    textSetting.setyMultiplication(1)
                    cmd.append(cmd.getTextCmd(textSetting, element.text, "GBK"))
                }
                is LabelElement.Barcode -> {
                    val barcodeSetting = BarcodeSetting()
                    barcodeSetting.narrowInDot = 2
                    barcodeSetting.wideInDot = 4
                    barcodeSetting.heightInDot = mmToDots(element.height)
                    barcodeSetting.barcodeStringPosition = BarcodeStringPosition.BELOW_BARCODE
                    barcodeSetting.printRotation = PrintRotation.Rotate0
                    barcodeSetting.position = Position(mmToDots(element.x), mmToDots(element.y))
                    val barcodeType = when (element.format) {
                        com.gp.q733.domain.model.BarcodeFormat.CODE128 -> BarcodeType.CODE128
                        com.gp.q733.domain.model.BarcodeFormat.CODE39 -> BarcodeType.CODE39
                        com.gp.q733.domain.model.BarcodeFormat.EAN13 -> BarcodeType.EAN13
                    }
                    try {
                        cmd.append(cmd.getBarcodeCmd(barcodeType, barcodeSetting, element.content))
                    } catch (e: SdkException) {
                        android.util.Log.e("PrintDebug", "TSPL barcode cmd error: ${e.message}")
                    }
                }
                is LabelElement.QRCode -> {
                    val barcodeSetting = BarcodeSetting()
                    val qrSize = (element.size / 4).toInt().coerceIn(1, 10)
                    barcodeSetting.qrcodeDotSize = qrSize
                    barcodeSetting.printRotation = PrintRotation.Rotate0
                    barcodeSetting.position = Position(mmToDots(element.x), mmToDots(element.y))
                    try {
                        cmd.append(cmd.getBarcodeCmd(BarcodeType.QR_CODE, barcodeSetting, element.content))
                    } catch (e: SdkException) {
                        android.util.Log.e("PrintDebug", "TSPL QR cmd error: ${e.message}")
                    }
                }
                is LabelElement.Line -> {
                    val x1 = mmToDots(element.x)
                    val y1 = mmToDots(element.y)
                    val x2 = mmToDots(element.x + element.width)
                    val y2 = mmToDots(element.y + element.height)
                    cmd.append("BAR $x1,$y1,$x2,$y2,1\r\n".toByteArray(Charsets.UTF_8))
                }
            }
        }
        try {
            cmd.append(cmd.getPrintCopies(1))
        } catch (e: SdkException) {
            // Ignore
        }
        cmd.append(cmd.endCmd)
        return cmd
    }

    private fun createEscCommand(label: Label, settings: com.gp.q733.data.local.AppSettings): Cmd {
        val factory = EscFactory()
        val cmd = factory.create()
        cmd.append(cmd.headerCmd)
        cmd.setChartsetName("GBK")
        label.elements.forEach { element ->
            when (element) {
                is LabelElement.Text -> {
                    val textSetting = TextSetting()
                    textSetting.escFontType = com.rt.printerlibrary.enumerate.ESCFontTypeEnum.FONT_A_12x24
                    textSetting.align = CommonEnum.ALIGN_LEFT
                    cmd.append(cmd.getTextCmd(textSetting, element.text, "GBK"))
                    cmd.append(cmd.getLFCRCmd())
                }
                else -> { /* ESC/POS limited support */ }
            }
        }
        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.headerCmd)
        return cmd
    }

    /**
     * CPCL test page
     */
        private fun createCpclTestCommand(deviceName: String, width: Int, height: Int, settings: com.gp.q733.data.local.AppSettings): Cmd {
        setCpclResolution()
        val factory = CpclFactory()
        val cmd = factory.create()
        val offset = 0
        cmd.append(cmd.getCpclHeaderCmd(width, height, 1, offset))

        val commonSetting = CommonSetting()
        commonSetting.speedEnum = SpeedEnum.getEnumByString(settings.printSpeed.toString())
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        var yPosMm = 2f
        val leftMarginMm = 2f

        // === ASCII font test (Font_4) - should always work ===
        val asciiSetting = TextSetting()
        asciiSetting.cpclFontTypeEnum = CpclFontTypeEnum.Font_4
        asciiSetting.printRotation = PrintRotation.Rotate0
        asciiSetting.setxMultiplication(2)
        asciiSetting.setyMultiplication(2)

        asciiSetting.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(asciiSetting, "GP-Q733 TEST PAGE", Charsets.US_ASCII.name()))
        yPosMm += 6f

        val smallAscii = TextSetting()
        smallAscii.cpclFontTypeEnum = CpclFontTypeEnum.Font_4
        smallAscii.printRotation = PrintRotation.Rotate0
        smallAscii.setxMultiplication(1)
        smallAscii.setyMultiplication(1)

        smallAscii.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(smallAscii, "Device: $deviceName", Charsets.US_ASCII.name()))
        yPosMm += 4f

        smallAscii.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(smallAscii, "Protocol: CPCL", Charsets.US_ASCII.name()))
        yPosMm += 4f

        smallAscii.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(smallAscii, "Label: ${width}x${height}mm", Charsets.US_ASCII.name()))
        yPosMm += 4f

        smallAscii.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(smallAscii, "DPI:203 D:${settings.printDensity} S:${settings.printSpeed}", Charsets.US_ASCII.name()))
        yPosMm += 6f

        // === Chinese font test (Font 24) ===
        val cnSetting = TextSetting()
        cnSetting.cpclFontTypeEnum = CpclFontTypeEnum.Font_Chinese_24x24
        cnSetting.printRotation = PrintRotation.Rotate0
        cnSetting.setxMultiplication(1)
        cnSetting.setyMultiplication(1)
        cnSetting.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(cnSetting, "\u6d4b\u8bd5\u4e2d\u6587", "GBK"))
        yPosMm += 5f

        // Test barcode
        val barcodeSetting = BarcodeSetting()
        barcodeSetting.printRotation = PrintRotation.Rotate0
        barcodeSetting.narrowInDot = 2
        barcodeSetting.wideInDot = 4
        barcodeSetting.heightInDot = mmToDots(8f)
        barcodeSetting.barcodeStringPosition = BarcodeStringPosition.BELOW_BARCODE
        barcodeSetting.position = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        try {
            cmd.append(cmd.getBarcodeCmd(BarcodeType.CODE128, barcodeSetting, "TEST123456"))
        } catch (e: SdkException) {
            android.util.Log.e("PrintDebug", "CPCL test barcode error: ${e.message}")
        }
        yPosMm += 12f

        // Test QR
        val qrSetting = BarcodeSetting()
        qrSetting.printRotation = PrintRotation.Rotate0
        qrSetting.qrcodeDotSize = 6
        qrSetting.position = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        try {
            cmd.append(cmd.getBarcodeCmd(BarcodeType.QR_CODE, qrSetting, "https://www.example.com"))
        } catch (e: SdkException) {
            android.util.Log.e("PrintDebug", "CPCL test QR error: ${e.message}")
        }

        cmd.append(cmd.getEndCmd())
        return cmd
    }
/**
     * TSPL test page
     */
    private fun createTsplTestCommand(deviceName: String, width: Int, height: Int, settings: com.gp.q733.data.local.AppSettings): Cmd {
        val factory = TsplFactory()
        val cmd = factory.create()
        val commonSetting = CommonSetting()
        commonSetting.lableSizeBean = LableSizeBean(width, height)
        commonSetting.labelGap = settings.gapMm.toInt()
        commonSetting.printDirection = PrintDirection.NORMAL
        commonSetting.speedEnum = SpeedEnum.getEnumByString(settings.printSpeed.toString())
        cmd.append(cmd.headerCmd)
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        var yPosMm = 5f
        val lineHeightMm = 5f
        val leftMarginMm = 10f

        val titleSetting = TextSetting()
        titleSetting.tsplFontTypeEnum = TsplFontTypeEnum.Font_TSS24_BF2_For_Simple_Chinese
        titleSetting.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        titleSetting.printRotation = PrintRotation.Rotate0
        titleSetting.setxMultiplication(1)
        titleSetting.setyMultiplication(1)
        cmd.append(cmd.getTextCmd(titleSetting, "GP-Q733 \u6d4b\u8bd5\u9875", "GBK"))
        yPosMm += lineHeightMm + 3f

        val normalSetting = TextSetting()
        normalSetting.tsplFontTypeEnum = TsplFontTypeEnum.Font_TSS24_BF2_For_Simple_Chinese
        normalSetting.printRotation = PrintRotation.Rotate0
        normalSetting.setxMultiplication(1)
        normalSetting.setyMultiplication(1)

        normalSetting.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(normalSetting, "\u8bbe\u5907: $deviceName", "GBK"))
        yPosMm += lineHeightMm

        normalSetting.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(normalSetting, "\u534f\u8bae: TSPL", "GBK"))
        yPosMm += lineHeightMm

        normalSetting.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(normalSetting, "\u6807\u7b7e: ${width}x${height}mm", "GBK"))
        yPosMm += lineHeightMm

        normalSetting.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        cmd.append(cmd.getTextCmd(normalSetting, "\u5bc6\u5ea6: ${settings.printDensity} \u901f\u5ea6: ${settings.printSpeed}", "GBK"))
        yPosMm += lineHeightMm + 3f

        val barcodeSetting = BarcodeSetting()
        barcodeSetting.narrowInDot = 2
        barcodeSetting.wideInDot = 4
        barcodeSetting.heightInDot = mmToDots(10f)
        barcodeSetting.barcodeStringPosition = BarcodeStringPosition.BELOW_BARCODE
        barcodeSetting.printRotation = PrintRotation.Rotate0
        barcodeSetting.position = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        try {
            cmd.append(cmd.getBarcodeCmd(BarcodeType.CODE128, barcodeSetting, "TEST123456"))
        } catch (e: SdkException) {
            android.util.Log.e("PrintDebug", "TSPL test barcode error: ${e.message}")
        }
        yPosMm += 15f

        val qrSetting = BarcodeSetting()
        qrSetting.qrcodeDotSize = 6
        qrSetting.printRotation = PrintRotation.Rotate0
        qrSetting.position = Position(mmToDots(leftMarginMm), mmToDots(yPosMm))
        try {
            cmd.append(cmd.getBarcodeCmd(BarcodeType.QR_CODE, qrSetting, "https://www.example.com"))
        } catch (e: SdkException) {
            android.util.Log.e("PrintDebug", "TSPL test QR error: ${e.message}")
        }

        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        normalSetting.txtPrintPosition = Position(mmToDots(leftMarginMm), mmToDots(yPosMm + 15f))
        cmd.append(cmd.getTextCmd(normalSetting, "\u65f6\u95f4: $timestamp", "GBK"))

        try {
            cmd.append(cmd.getPrintCopies(1))
        } catch (e: SdkException) { }
        cmd.append(cmd.endCmd)
        return cmd
    }

    private fun createEscTestCommand(deviceName: String, settings: com.gp.q733.data.local.AppSettings): Cmd {
        val factory = EscFactory()
        val cmd = factory.create()
        cmd.append(cmd.headerCmd)
        cmd.setChartsetName("GBK")

        val centerSetting = TextSetting()
        centerSetting.escFontType = com.rt.printerlibrary.enumerate.ESCFontTypeEnum.FONT_A_12x24
        centerSetting.align = CommonEnum.ALIGN_MIDDLE
        cmd.append(cmd.getTextCmd(centerSetting, "GP-Q733 TEST PAGE", "GBK"))
        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.getLFCRCmd())

        val leftSetting = TextSetting()
        leftSetting.escFontType = com.rt.printerlibrary.enumerate.ESCFontTypeEnum.FONT_A_12x24
        leftSetting.align = CommonEnum.ALIGN_LEFT
        cmd.append(cmd.getTextCmd(leftSetting, "Device: $deviceName", "GBK"))
        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.getTextCmd(leftSetting, "Protocol: ESC/POS", "GBK"))
        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.getTextCmd(leftSetting, "Density: ${settings.printDensity}", "GBK"))
        cmd.append(cmd.getLFCRCmd())

        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        cmd.append(cmd.getTextCmd(leftSetting, "Time: $timestamp", "GBK"))
        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.headerCmd)
        return cmd
    }
}
