package com.example.usbwifimonitor.capture

import com.example.usbwifimonitor.core.FrameLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Produces frames either from the native AR9271 sniffer (if available) or a synthetic
 * generator used for UI/demo purposes.
 */
object PacketSource {

    fun monitorStream(channel: Int?): Flow<FrameLog> =
        NativeSniffer.tryNativeStream(channel) ?: fakeMonitorStream(channel)

    private fun fakeMonitorStream(channel: Int?): Flow<FrameLog> = flow {
        var counter = 0
        while (true) {
            delay(500)
            counter++
            emit(
                FrameLog(
                    timestampMillis = System.currentTimeMillis(),
                    summary = "Symulowana ramka #$counter na kanale ${channel ?: 0}",
                    rssi = Random.nextInt(-90, -40),
                    channel = channel
                )
            )
        }
    }
}
