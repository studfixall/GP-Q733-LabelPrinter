package com.gp.q733.domain.print

import android.bluetooth.BluetoothDevice
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.rt.printerlibrary.bean.LableSizeBean
import com.rt.printerlibrary.bean.Position
import com.rt.printerlibrary.cmd.Cmd
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
import com.rt.printerlibrary.factory.connect.BluetoothFactory
import com.rt.printerlibrary.factory.printer.LabelPrinterFactory
import com.rt.printerlibrary.factory.printer.PrinterFactory
import com.rt.printerlibrary.factory.printer.ThermalPrinterFactory
import com.rt.printerlibrary.printer.RTPrinter
import com.rt.printerlibrary.setting.BarcodeSetting
import com.rt.printerlibrary.setting.CommonSetting
import com.rt.printerlibrary.setting.TextSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpPrinterService @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        private const val DPI = 203
        private fun mmToDots(mm: Number): Int = (mm.toFloat() * DPI / 25.4f).toInt()
    }

    private var rtPrinter: RTPrinter<*>? = null
    private var currentDevice: BluetoothDevice? = null

    /**
     * Connect to Bluetooth printer using official SDK
     */
    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // If already connected to same device, skip
            if (currentDevice?.address == device.address && isConnected()) {
                android.util.Log.d("PrintDebug", "GpPrinterService - already connected to ${device.name}")
                return@withContext Result.success(Unit)
            }

            // Disconnect if connected to different device
            disconnect()
            currentDevice = device

            // Get current protocol type
            val settings = settingsDataStore.settingsFlow.first()
            val cmdType = when (settings.printProtocol) {
                PrintProtocol.CPCL -> BaseEnum.CMD_CPCL
                PrintProtocol.TSPL -> BaseEnum.CMD_TSPL
                PrintProtocol.ESCPOS -> BaseEnum.CMD_ESC
            }

            // Create printer factory based on protocol
            val printerFactory: PrinterFactory = when (cmdType) {
                BaseEnum.CMD_ESC -> ThermalPrinterFactory()
                else -> LabelPrinterFactory()
            }
            rtPrinter = printerFactory.create()

            // Create Bluetooth connection
            val bluetoothFactory = BluetoothFactory()
            val bluetoothEdrConfigBean = com.rt.printerlibrary.bean.BluetoothEdrConfigBean(device)
            val printerInterface = bluetoothFactory.create()
            printerInterface.setConfigObject(bluetoothEdrConfigBean)

            // Connect (async - need to wait)
            rtPrinter?.setPrinterInterface(printerInterface)
            rtPrinter?.connect(bluetoothEdrConfigBean)

            android.util.Log.d("PrintDebug", "GpPrinterService.connect() - device: ${device.name}, address: ${device.address}")

            // Wait for connection (SDK connect is async, poll until connected or timeout)
            var retries = 0
            val maxRetries = 20 // 20 * 200ms = 4 seconds max
            while (!isConnected() && retries < maxRetries) {
                delay(200)
                retries++
            }
            val connected = isConnected()
            android.util.Log.d("PrintDebug", "GpPrinterService - connected: $connected (waited ${retries * 200}ms)")

            if (connected) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Connection timeout - printer did not connect within 4 seconds"))
            }
        } catch (e: Exception) {
            android.util.Log.d("PrintDebug", "GpPrinterService.connect() - error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Disconnect from printer
     */
    fun disconnect() {
        try {
            rtPrinter?.let { printer ->
                // Close connection if needed
            }
        } catch (e: Exception) {
            // Ignore close errors
        }
        rtPrinter = null
        currentDevice = null
    }

    /**
     * Check if printer is connected
     */
    fun isConnected(): Boolean {
        return rtPrinter?.printerInterface?.connectState == com.rt.printerlibrary.enumerate.ConnectStateEnum.Connected
    }

    /**
     * Print a label using official SDK
     */
    suspend fun print(label: Label): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val printer = rtPrinter ?: return@withContext Result.failure(Exception("Printer not connected"))
            val settings = settingsDataStore.settingsFlow.first()

            val cmdType = when (settings.printProtocol) {
                PrintProtocol.CPCL -> BaseEnum.CMD_CPCL
                PrintProtocol.TSPL -> BaseEnum.CMD_TSPL
                PrintProtocol.ESCPOS -> BaseEnum.CMD_ESC
            }

            val cmd = createPrintCommand(cmdType, label, settings)
            printer.writeMsg(cmd.getAppendCmds())
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.d("PrintDebug", "GpPrinterService.print() - error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Print test page using official SDK
     */
    suspend fun printTestPage(deviceName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val printer = rtPrinter ?: return@withContext Result.failure(Exception("Printer not connected"))
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

            android.util.Log.d("PrintDebug", "GpPrinterService.printTestPage() - protocol: ${settings.printProtocol}, size: ${labelWidth}x${labelHeight}mm")
            printer.writeMsg(cmd.getAppendCmds())
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.d("PrintDebug", "GpPrinterService.printTestPage() - error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun createPrintCommand(cmdType: Int, label: Label, settings: com.gp.q733.data.local.AppSettings): Cmd {
        return when (cmdType) {
            BaseEnum.CMD_CPCL -> createCpclCommand(label, settings)
            BaseEnum.CMD_TSPL -> createTsplCommand(label, settings)
            else -> createEscCommand(label, settings)
        }
    }

    /**
     * CPCL label print - fully aligned with SDK example usage
     * Key: getCpclHeaderCmd takes mm values, Position takes dots values
     * NOTE: CPCL coordinates start from 0,0 (left-top corner of printable area)
     */
    private fun createCpclCommand(label: Label, settings: com.gp.q733.data.local.AppSettings): Cmd {
        val factory = CpclFactory()
        val cmd = factory.create()

        val width = settings.labelWidth.toInt()
        val height = settings.labelHeight.toInt()
        val offset = 0

        // Header: width/height in mm (SDK internally converts to dots)
        cmd.append(cmd.getCpclHeaderCmd(width, height, 1, offset))

        // Common settings
        val commonSetting = CommonSetting()
        commonSetting.speedEnum = SpeedEnum.getEnumByString(settings.printSpeed.toString())
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        // Add elements using SDK methods
        label.elements.forEach { element ->
            when (element) {
                is LabelElement.Text -> {
                    val textSetting = TextSetting()
                    textSetting.cpclFontTypeEnum = CpclFontTypeEnum.Font_Chinese_24x24
                    // Position in dots - convert from mm, ensure non-negative
                    val xPos = mmToDots(element.x).coerceAtLeast(0)
                    val yPos = mmToDots(element.y).coerceAtLeast(0)
                    textSetting.txtPrintPosition = Position(xPos, yPos)
                    textSetting.printRotation = PrintRotation.Rotate0
                    textSetting.setxMultiplication(1)
                    textSetting.setyMultiplication(1)
                    textSetting.bold = if (element.isBold) SettingEnum.Enable else SettingEnum.Disable
                    cmd.append(cmd.getTextCmd(textSetting, element.text, "GBK"))
                }
                is LabelElement.Barcode -> {
                    // Use SDK getBarcodeCmd() instead of manual string assembly
                    val barcodeSetting = BarcodeSetting()
                    barcodeSetting.printRotation = PrintRotation.Rotate0
                    barcodeSetting.narrowInDot = 2
                    barcodeSetting.wideInDot = 4
                    barcodeSetting.barcodeStringPosition = BarcodeStringPosition.BELOW_BARCODE
                    barcodeSetting.heightInDot = mmToDots(element.height)
                    // Ensure non-negative coordinates
                    val xPos = mmToDots(element.x).coerceAtLeast(0)
                    val yPos = mmToDots(element.y).coerceAtLeast(0)
                    barcodeSetting.position = Position(xPos, yPos)

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
                    // Use SDK getBarcodeCmd() for QR
                    val barcodeSetting = BarcodeSetting()
                    barcodeSetting.printRotation = PrintRotation.Rotate0
                    val qrSize = (element.size / 4).toInt().coerceIn(1, 15)
                    barcodeSetting.qrcodeDotSize = qrSize
                    // Ensure non-negative coordinates
                    val xPos = mmToDots(element.x).coerceAtLeast(0)
                    val yPos = mmToDots(element.y).coerceAtLeast(0)
                    barcodeSetting.position = Position(xPos, yPos)
                    try {
                        cmd.append(cmd.getBarcodeCmd(BarcodeType.QR_CODE, barcodeSetting, element.content))
                    } catch (e: SdkException) {
                        android.util.Log.e("PrintDebug", "CPCL QR cmd error: ${e.message}")
                    }
                }
                is LabelElement.Line -> {
                    val x1 = mmToDots(element.x).coerceAtLeast(0)
                    val y1 = mmToDots(element.y).coerceAtLeast(0)
                    val x2 = mmToDots(element.x + element.width).coerceAtLeast(0)
                    val y2 = mmToDots(element.y + element.height).coerceAtLeast(0)
                    // BOX: x1 y1 x2 y2 thickness
                    cmd.append("BOX $x1 $y1 $x2 $y2 1\r\n".toByteArray(Charsets.UTF_8))
                }
            }
        }

        // End command (SDK handles FORM/PRINT internally)
        cmd.append(cmd.getEndCmd())
        return cmd
    }

    /**
     * TSPL label print - fully aligned with SDK example usage
     * Key: Position takes dots values, LableSizeBean takes mm values
     */
    private fun createTsplCommand(label: Label, settings: com.gp.q733.data.local.AppSettings): Cmd {
        val factory = TsplFactory()
        val cmd = factory.create()

        val width = settings.labelWidth.toInt()
        val height = settings.labelHeight.toInt()

        // Header and common settings
        val commonSetting = CommonSetting()
        commonSetting.lableSizeBean = LableSizeBean(width, height) // mm values
        commonSetting.labelGap = settings.gapMm.toInt()
        commonSetting.printDirection = PrintDirection.NORMAL
        commonSetting.speedEnum = SpeedEnum.getEnumByString(settings.printSpeed.toString())

        cmd.append(cmd.headerCmd)
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        // Add elements
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

        // ESC/POS doesn't support label format directly, print as receipt
        label.elements.forEach { element ->
            when (element) {
                is LabelElement.Text -> {
                    val textSetting = TextSetting()
                    textSetting.escFontType = com.rt.printerlibrary.enumerate.ESCFontTypeEnum.FONT_A_12x24
                    textSetting.align = CommonEnum.ALIGN_LEFT
                    cmd.append(cmd.getTextCmd(textSetting, element.text, "GBK"))
                    cmd.append(cmd.getLFCRCmd())
                }
                else -> {
                    // ESC/POS has limited support for barcodes/QR in this context
                }
            }
        }

        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.getLFCRCmd())
        cmd.append(cmd.headerCmd)
        return cmd
    }

    /**
     * CPCL test page - aligned with SDK cpclPrint3() example
     * Uses SDK's TextSetting/BarcodeSetting/Position correctly
     * NOTE: CPCL coordinates start from 0,0 (left-top corner of printable area)
     */
    private fun createCpclTestCommand(deviceName: String, width: Int, height: Int, settings: com.gp.q733.data.local.AppSettings): Cmd {
        val factory = CpclFactory()
        val cmd = factory.create()
        val offset = 0

        // Header: width/height in mm
        cmd.append(cmd.getCpclHeaderCmd(width, height, 1, offset))

        val commonSetting = CommonSetting()
        commonSetting.speedEnum = SpeedEnum.getEnumByString(settings.printSpeed.toString())
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        // Use dots for position (SDK convention), start from 0 for left alignment
        var yPos = 0
        val lineHeight = 30
        val leftMargin = 0  // Start from left edge

        // Title
        val titleSetting = TextSetting()
        titleSetting.cpclFontTypeEnum = CpclFontTypeEnum.Font_Chinese_24x24
        titleSetting.txtPrintPosition = Position(leftMargin, yPos)
        titleSetting.printRotation = PrintRotation.Rotate0
        titleSetting.setxMultiplication(1)
        titleSetting.setyMultiplication(1)
        cmd.append(cmd.getTextCmd(titleSetting, "GP-Q733 测试页", "GBK"))

        yPos += lineHeight + 10

        // Device info lines
        val normalSetting = TextSetting()
        normalSetting.cpclFontTypeEnum = CpclFontTypeEnum.Font_Chinese_24x24
        normalSetting.printRotation = PrintRotation.Rotate0
        normalSetting.setxMultiplication(1)
        normalSetting.setyMultiplication(1)

        normalSetting.txtPrintPosition = Position(leftMargin, yPos)
        cmd.append(cmd.getTextCmd(normalSetting, "设备: $deviceName", "GBK"))
        yPos += lineHeight

        normalSetting.txtPrintPosition = Position(leftMargin, yPos)
        cmd.append(cmd.getTextCmd(normalSetting, "协议: CPCL", "GBK"))
        yPos += lineHeight

        normalSetting.txtPrintPosition = Position(leftMargin, yPos)
        cmd.append(cmd.getTextCmd(normalSetting, "标签: ${width}x${height}mm", "GBK"))
        yPos += lineHeight

        normalSetting.txtPrintPosition = Position(leftMargin, yPos)
        cmd.append(cmd.getTextCmd(normalSetting, "密度: ${settings.printDensity} 速度: ${settings.printSpeed}", "GBK"))
        yPos += lineHeight + 10

        // Test barcode using SDK method
        val barcodeSetting = BarcodeSetting()
        barcodeSetting.printRotation = PrintRotation.Rotate0
        barcodeSetting.narrowInDot = 2
        barcodeSetting.wideInDot = 4
        barcodeSetting.heightInDot = 56
        barcodeSetting.barcodeStringPosition = BarcodeStringPosition.BELOW_BARCODE
        barcodeSetting.position = Position(leftMargin, yPos)
        try {
            cmd.append(cmd.getBarcodeCmd(BarcodeType.CODE128, barcodeSetting, "TEST123456"))
        } catch (e: SdkException) {
            android.util.Log.e("PrintDebug", "CPCL test barcode error: ${e.message}")
        }
        yPos += 80

        // Test QR using SDK method
        val qrSetting = BarcodeSetting()
        qrSetting.printRotation = PrintRotation.Rotate0
        qrSetting.qrcodeDotSize = 6
        qrSetting.position = Position(leftMargin, yPos)
        try {
            cmd.append(cmd.getBarcodeCmd(BarcodeType.QR_CODE, qrSetting, "https://www.example.com"))
        } catch (e: SdkException) {
            android.util.Log.e("PrintDebug", "CPCL test QR error: ${e.message}")
        }
        yPos += 80

        // Timestamp
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        normalSetting.txtPrintPosition = Position(leftMargin, yPos)
        cmd.append(cmd.getTextCmd(normalSetting, "时间: $timestamp", "GBK"))

        // End command (SDK handles FORM/PRINT)
        cmd.append(cmd.getEndCmd())
        return cmd
    }

    /**
     * TSPL test page - aligned with SDK tsplPrint() example
     */
    private fun createTsplTestCommand(deviceName: String, width: Int, height: Int, settings: com.gp.q733.data.local.AppSettings): Cmd {
        val factory = TsplFactory()
        val cmd = factory.create()

        val commonSetting = CommonSetting()
        commonSetting.lableSizeBean = LableSizeBean(width, height) // mm values
        commonSetting.labelGap = settings.gapMm.toInt()
        commonSetting.printDirection = PrintDirection.NORMAL
        commonSetting.speedEnum = SpeedEnum.getEnumByString(settings.printSpeed.toString())

        cmd.append(cmd.headerCmd)
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        var yPos = 40
        val lineHeight = 40

        // Title
        val titleSetting = TextSetting()
        titleSetting.tsplFontTypeEnum = TsplFontTypeEnum.Font_TSS24_BF2_For_Simple_Chinese
        titleSetting.txtPrintPosition = Position(80, yPos)
        titleSetting.printRotation = PrintRotation.Rotate0
        titleSetting.setxMultiplication(1)
        titleSetting.setyMultiplication(1)
        cmd.append(cmd.getTextCmd(titleSetting, "GP-Q733 测试页", "GBK"))
        yPos += lineHeight + 20

        // Device info
        val normalSetting = TextSetting()
        normalSetting.tsplFontTypeEnum = TsplFontTypeEnum.Font_TSS24_BF2_For_Simple_Chinese
        normalSetting.printRotation = PrintRotation.Rotate0
        normalSetting.setxMultiplication(1)
        normalSetting.setyMultiplication(1)

        normalSetting.txtPrintPosition = Position(80, yPos)
        cmd.append(cmd.getTextCmd(normalSetting, "设备: $deviceName", "GBK"))
        yPos += lineHeight

        normalSetting.txtPrintPosition = Position(80, yPos)
        cmd.append(cmd.getTextCmd(normalSetting, "协议: TSPL", "GBK"))
        yPos += lineHeight

        normalSetting.txtPrintPosition = Position(80, yPos)
        cmd.append(cmd.getTextCmd(normalSetting, "标签: ${width}x${height}mm", "GBK"))
        yPos += lineHeight

        normalSetting.txtPrintPosition = Position(80, yPos)
        cmd.append(cmd.getTextCmd(normalSetting, "密度: ${settings.printDensity} 速度: ${settings.printSpeed}", "GBK"))
        yPos += lineHeight + 20

        // Test barcode
        val barcodeSetting = BarcodeSetting()
        barcodeSetting.narrowInDot = 2
        barcodeSetting.wideInDot = 4
        barcodeSetting.heightInDot = 60
        barcodeSetting.barcodeStringPosition = BarcodeStringPosition.BELOW_BARCODE
        barcodeSetting.printRotation = PrintRotation.Rotate0
        barcodeSetting.position = Position(80, yPos)
        try {
            cmd.append(cmd.getBarcodeCmd(BarcodeType.CODE128, barcodeSetting, "TEST123456"))
        } catch (e: SdkException) {
            android.util.Log.e("PrintDebug", "TSPL test barcode error: ${e.message}")
        }
        yPos += 80

        // Test QR
        val qrSetting = BarcodeSetting()
        qrSetting.qrcodeDotSize = 6
        qrSetting.printRotation = PrintRotation.Rotate0
        qrSetting.position = Position(80, yPos)
        try {
            cmd.append(cmd.getBarcodeCmd(BarcodeType.QR_CODE, qrSetting, "https://www.example.com"))
        } catch (e: SdkException) {
            android.util.Log.e("PrintDebug", "TSPL test QR error: ${e.message}")
        }

        // Timestamp
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        normalSetting.txtPrintPosition = Position(80, yPos + 100)
        cmd.append(cmd.getTextCmd(normalSetting, "时间: $timestamp", "GBK"))

        try {
            cmd.append(cmd.getPrintCopies(1))
        } catch (e: SdkException) {
            // Ignore
        }
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
