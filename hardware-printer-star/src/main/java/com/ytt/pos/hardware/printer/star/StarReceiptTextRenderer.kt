package com.ytt.pos.hardware.printer.star

import com.ytt.pos.domain.receipt.ReceiptContent

object StarReceiptCommandBuilder {
    private const val ESC = 0x1B
    private const val GS = 0x1D

    private fun b(i: Int): Byte = i.toByte()

    private fun ArrayList<Byte>.addBytes(vararg ints: Int) {
        for (i in ints) add(i.toByte())
    }

    private fun ArrayList<Byte>.addBytes(ints: List<Int>) {
        for (i in ints) add(i.toByte())
    }

    fun build(receipt: ReceiptContent): ByteArray {
        val bytes = arrayListOf<Byte>()
        bytes.addAll(byteArrayOf(b(ESC), b('@'.code)).toList())

        receipt.lines.forEachIndexed { index, line ->
            when {
                line.isBlank() -> bytes.addBytes(0x0A)
                line.isSeparator() -> {
                    bytes.addAll(alignLeft().toList())
                    bytes.addAll(encode(line).toList())
                    bytes.addBytes(0x0A)
                }

                line.isTaxExemptHeader() -> {
                    bytes.addAll(alignCenter().toList())
                    bytes.addAll(boldOn().toList())
                    bytes.addAll(encode(line).toList())
                    bytes.addAll(boldOff().toList())
                    bytes.addBytes(0x0A)
                    bytes.addAll(alignLeft().toList())
                }

                index == 0 -> {
                    bytes.addAll(alignCenter().toList())
                    bytes.addAll(boldOn().toList())
                    bytes.addAll(encode(line).toList())
                    bytes.addAll(boldOff().toList())
                    bytes.addBytes(0x0A)
                    bytes.addAll(alignLeft().toList())
                }

                else -> {
                    bytes.addAll(alignLeft().toList())
                    bytes.addAll(encode(line).toList())
                    bytes.addBytes(0x0A)
                }
            }
        }

        bytes.addBytes(listOf(0x0A, 0x0A, 0x0A))
        bytes.addBytes(listOf(GS, 'V'.code, 0x00))
        return bytes.toByteArray()
    }

    fun buildDrawerKick(): ByteArray = byteArrayOf(b(ESC), b('p'.code), b(0x00), b(0x19), b(0xFA))

    private fun alignLeft() = byteArrayOf(b(ESC), b('a'.code), b(0x00))

    private fun alignCenter() = byteArrayOf(b(ESC), b('a'.code), b(0x01))

    private fun boldOn() = byteArrayOf(b(ESC), b('E'.code), b(0x01))

    private fun boldOff() = byteArrayOf(b(ESC), b('E'.code), b(0x00))

    private fun encode(value: String): ByteArray = value.toByteArray(Charsets.UTF_8)

    private fun String.isSeparator(): Boolean = all { it == '-' || it == '=' || it == '_' }

    private fun String.isTaxExemptHeader(): Boolean = contains("TAX EXEMPT", ignoreCase = true)
}
