package com.example.usbwifimonitor.core

data class FrameLog(
    val timestampMillis: Long,
    val summary: String,
    val rssi: Int? = null,
    val channel: Int? = null
)
