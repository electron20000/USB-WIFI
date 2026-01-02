package com.example.usbwifimonitor.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.usbwifimonitor.capture.CaptureBus
import com.example.usbwifimonitor.capture.CaptureForegroundService
import com.example.usbwifimonitor.core.UsbStatus
import com.example.usbwifimonitor.databinding.ActivityMainBinding
import com.example.usbwifimonitor.usb.UsbDeviceManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var usbDeviceManager: UsbDeviceManager
    private val adapter = FrameLogAdapter()
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        binding.frameList.adapter = adapter
        usbDeviceManager = UsbDeviceManager(this, ::renderUsbState)
        binding.startButton.setOnClickListener {
            val channel = binding.channelInput.text.toString().toIntOrNull() ?: 1
            CaptureForegroundService.start(this, channel)
            toggleButtons(true)
        }
        binding.stopButton.setOnClickListener {
            CaptureForegroundService.stop(this)
            toggleButtons(false)
        }

        lifecycleScope.launch {
            CaptureBus.frames.collectLatest { frame ->
                adapter.submit(frame)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        usbDeviceManager.start()
    }

    override fun onPause() {
        super.onPause()
        usbDeviceManager.stop()
    }

    private fun renderUsbState(status: UsbStatus) {
        when (status) {
            UsbStatus.Unknown -> binding.usbStatus.text = getString(com.example.usbwifimonitor.R.string.usb_status_unknown)
            UsbStatus.WaitingForDevice -> binding.usbStatus.text = getString(com.example.usbwifimonitor.R.string.usb_not_ready)
            is UsbStatus.PermissionRequired -> binding.usbStatus.text = "Wymagane pozwolenie na ${status.deviceName ?: "USB"}"
            is UsbStatus.Ready -> binding.usbStatus.text = "Gotowe: ${status.deviceName}"
            is UsbStatus.Error -> {
                binding.usbStatus.text = status.reason
                Toast.makeText(this, status.reason, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleButtons(capturing: Boolean) {
        binding.startButton.isEnabled = !capturing
        binding.stopButton.isEnabled = capturing
    }
}
