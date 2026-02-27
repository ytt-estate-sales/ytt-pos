package com.ytt.pos.domain.receipt

import com.ytt.pos.domain.model.PermitSnapshot
import com.ytt.pos.domain.model.TaxStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReceiptRendererTest {
    private val renderer = ReceiptRenderer()

    @Test
    fun `render taxable receipt excludes exempt resale block`() {
        val receipt = renderer.render(
            ReceiptModel(
                transactionId = "TX-1",
                lines = listOf(ReceiptLine(name = "Widget", qty = 1, unitPriceMinor = 1250)),
                subtotalMinor = 1250,
                taxMinor = 100,
                totalMinor = 1350,
                taxStatus = TaxStatus.TAXABLE,
                permitSnapshot = null,
            ),
        )

        assertFalse(receipt.lines.contains("TAX EXEMPT - RESALE"))
    }

    @Test
    fun `render exempt resale receipt includes permit details`() {
        val receipt = renderer.render(
            ReceiptModel(
                transactionId = "TX-2",
                lines = listOf(ReceiptLine(name = "Bulk Bolts", qty = 2, unitPriceMinor = 5000)),
                subtotalMinor = 10000,
                taxMinor = 0,
                totalMinor = 10000,
                taxStatus = TaxStatus.EXEMPT_RESALE,
                permitSnapshot = PermitSnapshot(
                    businessName = "Builder Bros",
                    permitNumber = "PERMIT-77",
                    state = "TX",
                    capturedAtEpochMs = 1700000000000,
                ),
            ),
        )

        assertTrue(receipt.lines.contains("TAX EXEMPT - RESALE"))
        assertTrue(receipt.lines.contains("Builder Bros"))
        assertTrue(receipt.lines.contains("Permit # PERMIT-77  TX"))
    }
}
