package com.ytt.pos

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardcodedManagerAuthService @Inject constructor() : ManagerAuthService {
    override fun validatePin(pin: String): Boolean {
        // TODO: Move manager PIN to secure storage.
        return pin == "1234"
    }
}
