package com.gp.q733.domain.print

enum class PrintProtocol {
    CPCL,
    TSPL,
    ESCPOS
}

/**
 * 纸张类型
 *
 * 不同纸张类型的走纸/检纸逻辑不同：
 * - 标签纸：带间隙，靠间隙传感器检纸
 * - 黑标纸：带黑色标记，靠黑标传感器检纸
 * - 票据纸：连续纸，无间隙无标记
 */
enum class PaperType {
    /** 标签纸（带间隙） */
    LABEL,
    /** 黑标纸（带黑色标记） */
    BLACK_MARK,
    /** 票据纸/连续纸 */
    RECEIPT
}
