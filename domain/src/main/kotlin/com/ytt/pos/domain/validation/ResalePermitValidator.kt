package com.ytt.pos.domain.validation

import com.ytt.pos.domain.model.ResalePermit

sealed class PermitValidationError {
    data object MissingBusinessName : PermitValidationError()
    data object MissingPermitNumber : PermitValidationError()
    data object MissingState : PermitValidationError()
}

class ResalePermitValidator {
    fun validate(permit: ResalePermit): List<PermitValidationError> {
        val errors = mutableListOf<PermitValidationError>()

        if (permit.businessName.isBlank()) {
            errors += PermitValidationError.MissingBusinessName
        }
        if (permit.permitNumber.isBlank()) {
            errors += PermitValidationError.MissingPermitNumber
        }
        if (permit.state.isBlank()) {
            errors += PermitValidationError.MissingState
        }

        return errors
    }

    fun isValid(permit: ResalePermit): Boolean = validate(permit).isEmpty()
}
