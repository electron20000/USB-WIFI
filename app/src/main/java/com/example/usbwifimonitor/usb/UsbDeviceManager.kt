package com.example.usbwifimonitor.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.example.usbwifimonitor.core.UsbStatus

class UsbDeviceManager(
    private val context: Context,
    private val onStatusChanged: (UsbStatus) -> Unit
) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val attachDetachReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> onStatusChanged(UsbStatus.PermissionRequired(intent.deviceName()))
                UsbManager.ACTION_USB_DEVICE_DETACHED -> onStatusChanged(UsbStatus.WaitingForDevice)
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(attachDetachReceiver, filter)
        refreshDevice()
    }

    fun stop() {
        context.unregisterReceiver(attachDetachReceiver)
    }

    fun refreshDevice() {
        val target = usbManager.deviceList.values.firstOrNull(::isTargetDevice)
        if (target == null) {
            onStatusChanged(UsbStatus.WaitingForDevice)
            return
        }
        if (usbManager.hasPermission(target)) {
            onStatusChanged(UsbStatus.Ready(target.deviceName))
        } else {
            requestPermission(target)
        }
    }

    private fun requestPermission(device: UsbDevice) {
        UsbPermissionReceiver.listener = { requested, granted ->
            if (requested?.deviceId == device.deviceId && granted) {
                onStatusChanged(UsbStatus.Ready(device.deviceName))
            } else {
                onStatusChanged(UsbStatus.Error("Brak uprawnień do USB"))
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(UsbPermissionReceiver.ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        usbManager.requestPermission(device, pendingIntent)
        onStatusChanged(UsbStatus.PermissionRequired(device.deviceName))
    }

    private fun isTargetDevice(device: UsbDevice): Boolean {
        return SUPPORTED_IDS.any { (vid, pid) -> device.vendorId == vid && device.productId == pid }
    }

    private fun Intent.deviceName(): String? = getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.deviceName

    companion object {
        private val SUPPORTED_IDS = setOf(
            // IDs from ath9k_htc driver table (hif_usb.c)
            0x0cf3 to 0x9271, // Atheros reference
            0x0cf3 to 0x1006, // Atheros
            0x0846 to 0x9030, // Netgear N150
            0x07b8 to 0x9271, // Altai WA1011N-GU
            0x07d1 to 0x3a10, // D-Link Wireless 150
            0x13d3 to 0x3327, // Azurewave
            0x13d3 to 0x3328, // Azurewave
            0x13d3 to 0x3346, // IMC Networks
            0x13d3 to 0x3348, // Azurewave
            0x13d3 to 0x3349, // Azurewave
            0x13d3 to 0x3350, // Azurewave
            0x04ca to 0x4605, // Liteon
            0x040d to 0x3801, // VIA
            0x0cf3 to 0xb003, // Ubiquiti WifiStation Ext
            0x0cf3 to 0xb002, // Ubiquiti WifiStation
            0x057c to 0x8403, // AVM FRITZ!WLAN 11N v2 USB
            0x0471 to 0x209e // Philips/NXP PTA01
        )
    }
}
