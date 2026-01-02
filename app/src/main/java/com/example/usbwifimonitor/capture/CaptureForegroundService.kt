package com.example.usbwifimonitor.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.usbwifimonitor.R
import com.example.usbwifimonitor.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CaptureForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var captureJob: Job? = null
    private var activeChannel: Int = DEFAULT_CHANNEL

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val channel = intent?.getIntExtra(EXTRA_CHANNEL, DEFAULT_CHANNEL) ?: DEFAULT_CHANNEL
        startForeground(NOTIFICATION_ID, buildNotification(channel))
        if (captureJob == null || captureJob?.isActive == false || activeChannel != channel) {
            captureJob?.cancel()
            activeChannel = channel
            captureJob = serviceScope.launch {
                PacketSource.monitorStream(channel).collectLatest { frame ->
                    CaptureBus.emit(frame)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        captureJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(channel: Int): Notification {
        val channelId = ensureNotificationChannel()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Monitor mode aktywny na kanale $channel")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun ensureNotificationChannel(): String {
        val channelId = "usb_wifi_monitor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
        return channelId
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_CHANNEL = 1
        private const val EXTRA_CHANNEL = "channel"
        private const val ACTION_STOP = "com.example.usbwifimonitor.STOP_CAPTURE"

        fun start(context: Context, channel: Int) {
            val intent = Intent(context, CaptureForegroundService::class.java).apply {
                putExtra(EXTRA_CHANNEL, channel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CaptureForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
