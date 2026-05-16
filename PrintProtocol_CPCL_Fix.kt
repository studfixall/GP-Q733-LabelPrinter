// PrintProtocol.kt - CPCL 修复版本
// 修复内容：将 TTF 指令改为 TEXT 指令，修复中文乱码问题

sealed class PrintProtocol {
    abstract fun generate(
        widthMm: Float,
        heightMm: Float,
        density: Int,
        speed: Int,
        gapMm: Float = 2f
    ): ByteArray

    // CPCL 协议修复版本
    class Cpcl : PrintProtocol() {
        override fun generate(
            widthMm: Float,
            heightMm: Float,
            density: Int,
            speed: Int,
            gapMm: Float
        ): ByteArray {
            val output = ByteArrayOutputStream()
            
            // CPCL 头部 - 使用 UTF-8 编码
            output.write("! 0 200 200 ${heightMm.toInt()} 1\r\n".toByteArray(Charsets.UTF_8))
            output.write("PW ${widthMm.toInt()}\r\n".toByteArray(Charsets.UTF_8))
            output.write("DENSITY $density\r\n".toByteArray(Charsets.UTF_8))
            output.write("SPEED $speed\r\n".toByteArray(Charsets.UTF_8))
            
            return output.toByteArray()
        }
        
        // 修复后的文本打印方法 - 使用 TEXT 指令而非 TTF
        fun generateText(
            x: Int,
            y: Int,
            fontSize: Int,
            text: String,
            bold: Boolean = false,
            reverse: Boolean = false
        ): ByteArray {
            val output = ByteArrayOutputStream()
            
            // 使用 TEXT 指令而不是 TTF
            // 格式: TEXT x y font size content
            // font: 0=正常, 1=粗体
            val font = if (bold) "1" else "0"
            
            // 反转模式（黑底白字）
            if (reverse) {
                output.write("REVERSE ON\r\n".toByteArray(Charsets.UTF_8))
            }
            
            // 使用 TEXT 指令 - 这是 CPCL 的标准文本指令
            output.write("TEXT $x $y $font $fontSize ".toByteArray(Charsets.UTF_8))
            output.write(text.toByteArray(Charsets.UTF_8))
            output.write("\r\n".toByteArray(Charsets.UTF_8))
            
            if (reverse) {
                output.write("REVERSE OFF\r\n".toByteArray(Charsets.UTF_8))
            }
            
            return output.toByteArray()
        }
        
        // 中文文本打印 - 使用 24x24 中文字体
        fun generateChineseText(
            x: Int,
            y: Int,
            text: String
        ): ByteArray {
            val output = ByteArrayOutputStream()
            
            // 对于中文，使用 24x24 字体 (size=24)
            // CPCL 的 TEXT 指令支持中文，但需要打印机内置中文字体
            output.write("TEXT $x $y 0 24 ".toByteArray(Charsets.UTF_8))
            output.write(text.toByteArray(Charsets.UTF_8))  // UTF-8 编码
            output.write("\r\n".toByteArray(Charsets.UTF_8))
            
            return output.toByteArray()
        }
        
        // 结束打印
        fun generateEnd(): ByteArray {
            return "PRINT\r\n".toByteArray(Charsets.UTF_8)
        }
    }
}

// 使用示例（测试页生成）
fun generateTestPageCPCL(): ByteArray {
    val cpcl = PrintProtocol.Cpcl()
    val output = ByteArrayOutputStream()
    
    // 头部
    output.write(cpcl.generate(60f, 40f, 8, 4))
    
    // 标题 - 使用中文
    output.write(cpcl.generateChineseText(10, 10, "测试打印"))
    
    // 英文文本
    output.write(cpcl.generateText(10, 40, 20, "Test Page", bold = true))
    output.write(cpcl.generateText(10, 70, 16, "CPCL Protocol"))
    
    // 结束
    output.write(cpcl.generateEnd())
    
    return output.toByteArray()
}
