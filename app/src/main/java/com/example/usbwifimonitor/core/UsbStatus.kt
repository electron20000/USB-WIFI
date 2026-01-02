package com.example.usbwifimonitor.core

sealed class UsbStatus {
    object Unknown : UsbStatus()
    object WaitingForDevice : UsbStatus()
    data class PermissionRequired(val deviceName: String?) : UsbStatus()
    data class Ready(val deviceName: String?) : UsbStatus()
    data class Error(val reason: String) : UsbStatus()
}
