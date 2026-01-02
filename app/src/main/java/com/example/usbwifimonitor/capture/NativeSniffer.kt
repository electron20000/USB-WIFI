package com.example.usbwifimonitor.capture

import com.example.usbwifimonitor.core.FrameLog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Placeholder bridge to the future NDK-based AR9271 sniffer.
 * In production this object will load the JNI library and expose a Flow of frames
 * coming from bulk IN URBs. For now it returns null so the UI falls back to
 * the synthetic generator.
 */
object NativeSniffer {
    private var nativeLoaded = false

    init {
        runCatching {
            System.loadLibrary("ar9271sniffer")
            nativeLoaded = true
        }
    }

    fun tryNativeStream(channel: Int?): Flow<FrameLog>? {
        if (!nativeLoaded) return null
        val channelFlow = Channel<FrameLog>(capacity = Channel.BUFFERED)
        if (startNative(channel ?: 1, channelFlow)) {
            return channelFlow.receiveAsFlow()
        }
        return null
    }

    // Stub entry point for JNI glue; return true when native thread started successfully
    private external fun startNative(channel: Int, sink: Channel<FrameLog>): Boolean
}
