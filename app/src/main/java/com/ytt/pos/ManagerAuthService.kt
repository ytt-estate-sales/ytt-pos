package com.ytt.pos

interface ManagerAuthService {
    fun validatePin(pin: String): Boolean
}
