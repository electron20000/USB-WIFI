package com.example.usbwifimonitor.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_USB_PERMISSION) return
        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
        listener?.invoke(device, granted)
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.usbwifimonitor.USB_PERMISSION"
        var listener: ((UsbDevice?, Boolean) -> Unit)? = null
    }
}
