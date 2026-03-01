package com.ytt.pos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionHelper @Inject constructor() {
    fun scanPermissions(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun missingScanPermissions(context: Context): List<String> = scanPermissions().filterNot { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasScanPermissions(context: Context): Boolean = missingScanPermissions(context).isEmpty()
}
