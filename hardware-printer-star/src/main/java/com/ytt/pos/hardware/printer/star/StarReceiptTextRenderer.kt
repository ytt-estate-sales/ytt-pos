package com.ytt.pos.hardware.printer.star

import com.ytt.pos.domain.receipt.ReceiptContent

object StarReceiptCommandBuilder {
    private const val ESC = 0x1B
    private const val GS = 0x1D

    fun build(receipt: ReceiptContent): ByteArray {
        val out = ArrayList<Byte>()
        out += byteArrayOf(ESC, '@'.code)

        receipt.lines.forEachIndexed { index, line ->
            when {
                line.isBlank() -> out += byteArrayOf(0x0A)
                line.isSeparator() -> {
                    out += alignLeft()
                    out += encode(line)
                    out += byteArrayOf(0x0A)
                }
                line.isTaxExemptHeader() -> {
                    out += alignCenter()
                    out += boldOn()
                    out += encode(line)
                    out += boldOff()
                    out += byteArrayOf(0x0A)
                    out += alignLeft()
                }
                index == 0 -> {
                    out += alignCenter()
                    out += boldOn()
                    out += encode(line)
                    out += boldOff()
                    out += byteArrayOf(0x0A)
                    out += alignLeft()
                }
                else -> {
                    out += alignLeft()
                    out += encode(line)
                    out += byteArrayOf(0x0A)
                }
            }
        }

        out += byteArrayOf(0x0A, 0x0A, 0x0A)
        out += byteArrayOf(GS, 'V'.code, 0x00)
        return out.toByteArray()
    }

    fun buildDrawerKick(): ByteArray = byteArrayOf(ESC, 'p'.code, 0x00, 0x19, 0xFA.toByte())

    private fun alignLeft() = byteArrayOf(ESC, 'a'.code, 0x00)

    private fun alignCenter() = byteArrayOf(ESC, 'a'.code, 0x01)

    private fun boldOn() = byteArrayOf(ESC, 'E'.code, 0x01)

    private fun boldOff() = byteArrayOf(ESC, 'E'.code, 0x00)

    private fun encode(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)

    private fun String.isSeparator(): Boolean = all { it == '-' || it == '=' || it == '_' }

    private fun String.isTaxExemptHeader(): Boolean = contains("TAX EXEMPT", ignoreCase = true)
}
