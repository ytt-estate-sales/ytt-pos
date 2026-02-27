package com.ytt.pos.domain.validation

import com.ytt.pos.domain.model.ResalePermit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResalePermitValidatorTest {
    private val validator = ResalePermitValidator()

    @Test
    fun `validate returns no errors for complete permit`() {
        val permit = ResalePermit(
            businessName = "Acme Supplies",
            permitNumber = "RP-12345",
            state = "CA",
            expiresOn = null,
        )

        val errors = validator.validate(permit)

        assertTrue(errors.isEmpty())
        assertTrue(validator.isValid(permit))
    }

    @Test
    fun `validate returns all missing-field errors`() {
        val permit = ResalePermit(
            businessName = "",
            permitNumber = "",
            state = "",
            expiresOn = null,
        )

        val errors = validator.validate(permit)

        assertEquals(
            listOf(
                PermitValidationError.MissingBusinessName,
                PermitValidationError.MissingPermitNumber,
                PermitValidationError.MissingState,
            ),
            errors,
        )
    }
}
