package com.example.usbwifimonitor.capture

import com.example.usbwifimonitor.core.FrameLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object CaptureBus {
    private val _frames = MutableSharedFlow<FrameLog>(replay = 32, extraBufferCapacity = 32)
    val frames: SharedFlow<FrameLog> = _frames

    suspend fun emit(frameLog: FrameLog) {
        _frames.emit(frameLog)
    }
}
