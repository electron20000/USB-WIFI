# Android AR9271 Monitor Mode App (No Root)

## 1. Research Findings
### 1.1 AR9271 + ath9k_htc driver
- **Chipset summary:** Single-stream 802.11n USB part using AR7010 USB/HTC transport. Firmware executed on the on-chip CPU; host driver uploads firmware via USB control transfers.
- **Firmware loading path (Linux ath9k_htc):**
  - USB probe issues control requests to reset the device, load firmware header, then bulk-transfer firmware image (`htc_9271.fw`) through endpoint 0; validated in `ath9k_htc/core.c` and `ath9k_htc/hif_usb.c` state machine.
  - After firmware boot, driver exchanges HTC control messages (credits, endpoint setup) and WMI commands to configure MAC, channels, RX/TX filters, and monitor mode.
- **URB / USB endpoints:**
  - One control endpoint (0) for firmware and configuration; bulk IN/OUT endpoints for data; optional interrupt endpoint for events.
  - RX path: bulk IN URBs carry HTC frames → WMI events → 802.11 frames, with per-frame RX status (RSSI, channel flags) in HTC/WMI headers.
  - TX path: host builds HTC TX descriptors + 802.11 frame (or radiotap/monitor headers) and posts bulk OUT URBs; firmware assigns sequence control and performs encryption if enabled.
- **Monitor mode specifics:**
  - Driver disables hardware encryption, enables RX of control/mgmt/data, and allows radiotap reporting (`RX_STAT_AMPDU`, RSSI, rate).
  - Channel configuration uses `WMI_CMD_SET_CHANNEL` with HT params; firmware supports continuous RX with short/long preambles; DFS not supported on AR9271 firmware shipped with Linux.

### 1.2 Android USB Host constraints
- Requires `android.hardware.usb.host` feature, OTG cable, and per-device user permission via `UsbManager.requestPermission` (runtime prompt each attach).
- Power limits: many phones cap at 100–500 mA; TL‑WN721N can draw ~250–300 mA during TX, so powered hub or Y-cable is recommended. App should detect `UsbDeviceConnection.requestWait` detach/attach and warn about undervoltage.
- No kernel driver binding: app must implement user-space USB stack (bulk/control transfers) and replicate ath9k_htc logic via NDK. Isochronous endpoints unsupported; bulk/control supported.
- Background execution: long-running USB access requires foreground service with ongoing notification; doze/standby can suspend transfers unless using `PARTIAL_WAKE_LOCK` + `foregroundServiceType="dataSync"`.
- Scoped storage: PCAP output must use `MediaStore` or SAF; temporary captures in app-specific storage.

### 1.3 Prior work / references
- **liber80211 / monmob (XDA)**: user-space monitor mode over USB Wi‑Fi dongles on rooted/limited devices; shows feasibility of porting linux driver logic to user space with firmware upload and HTC/WMI handling.
- **nexmon / monitor firmware patches (Broadcom)**: demonstrate firmware patching for monitor/injection on mobile; informs risk that AR9271 firmware may need tweaks, though Linux firmware already monitor-capable.
- **Wireshark + usbmon traces on Linux**: provides canonical USB conversation for firmware load + monitor setup; essential for reproducing in Android NDK.
- **Upstream ath9k_htc + firmware docs**: VID/PID table in `hif_usb.c` (Netgear, Ubiquiti, D-Link, Azurewave variants) and firmware notes in the open-ath9k-htc-firmware README inform which dongles we should accept and which blob (`htc_9271.fw`) to ship or request. See `docs/RESEARCH.md`.

## 2. Proposed Architecture
### 2.1 High-level split
- **Kotlin UI layer (Android app, targetSdk 34)**
  - Handles permissions (USB, notifications, file write via SAF), device discovery, channel selection, capture controls, and user feedback.
  - Hosts a foreground service (`MonitorService`) that owns the USB connection and keeps the process alive; binds to activity for status updates.
- **NDK/C++ core**
  - Implements minimal ath9k_htc-equivalent: firmware loader, HTC/WMI protocol, RX/TX queues, radiotap framing, PCAP writer.
  - Exposes JNI surface (`NativeSniffer`) for start/stop, channel set, stats, and frame callbacks.
- **Optional Rust helper** (via CMake external project) for safe PCAP generation and ring buffers; can be omitted if toolchain complexity is a risk.

### 2.2 Initialization flow (device attach → monitor mode)
1. **USB detection:** Kotlin registers `UsbManager.ACTION_USB_DEVICE_ATTACHED`; filters for VID:PID of TL‑WN721N (0cf3:9271). Prompt user permission.
2. **Open connection:** In foreground service, claim interfaces, set configuration 1, locate bulk IN/OUT endpoints.
3. **Firmware upload (NDK):**
   - Issue control transfer reset; download firmware (`htc_9271.fw`) from assets → temp file → mmap.
   - Send firmware via control endpoint chunks respecting max packet size (typically 512 bytes) with checksum; wait for device re-enumeration or firmware ready event.
4. **HTC init:** exchange credits, map endpoints, set block sizes; configure mailbox/interrupt endpoint if present.
5. **WMI setup:**
   - Set MAC address (random locally administered), disable crypto, configure RX filters to accept promisc/mgmt/control/data, enable RX status reporting.
   - Configure channel (center freq, HT20/HT40), set TX power conservative, enable beacon filtering off.
6. **Start RX:** Submit N bulk-IN URBs; each completion posts to lock-free queue for JNI callback. Add radiotap header before delivering to Kotlin/PCAP.
7. **Optional TX/injection:** Accept crafted 802.11 frames from UI/automation, prepend radiotap/HTC headers, submit bulk OUT URB.
8. **Monitoring & teardown:** Heartbeat with firmware (keep-alive WMI), handle USB detach, stop service gracefully.

### 2.3 Data path diagrams
```
[USB Dongle] ⇄ (Bulk IN/OUT URBs) ⇄ [NDK Core]
    RX: URB → HTC frame → WMI event → 802.11 + radiotap → ring buffer → JNI → UI/PCAP
    TX: UI/automation → JNI → radiotap/HTC → bulk OUT URB → air
```

### 2.4 Concurrency model
- Single USB worker thread for blocking bulk transfers (libusb on Android or `UsbDeviceConnection.bulkTransfer`).
- RX completion thread pushes frames to native ring buffer; Kotlin polls via `Flow`/callback on a background dispatcher.
- Foreground service manages wake locks; stop on errors/detach.

### 2.5 Error handling & resilience
- Detect firmware timeouts, URB stalls; attempt re-upload once.
- Surface underrun/undervolt hints when repeated USB resets occur.
- Persist captures safely: rotate PCAP files, guard against storage full.

## 3. Android-specific requirements
- **Permissions:** `android.permission.FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `REQUEST_INSTALL_PACKAGES` not needed; runtime USB permission via intent, notification permission on Android 13+ for foreground service.
- **Device filter:** `res/xml/device_filter.xml` with AR9271 VID/PID plus class-subclass wildcard; dynamic handling for other Atheros clones.
- **Scoped storage:** use `MediaStore.Downloads` or `ACTION_CREATE_DOCUMENT` for PCAP destinations.
- **Background longevity:** start foreground service before opening USB; use `startForeground()` with persistent notification; consider `WorkManager` for restart after reboot (if allowed via `RECEIVE_BOOT_COMPLETED`).

## 4. UI/UX plan
- **Home screen:** device status, permission prompt, start/stop capture, channel dropdown, bandwidth toggle.
- **Capture view:** rolling log of frames (timestamp, type, RSSI, channel). Filter chips for mgmt/control/data/beacon. Counters for drops/CRC.
- **PCAP management:** choose output location, show file size, rotate/stop; share/export action.
- **Advanced:** optional frame injection console with hex/pcap import; warning about regulatory use.

## 5. Implementation steps & tools
1. **Baseline USB trace on Linux:** plug TL‑WN721N, capture usbmon/pcap (`usbmonX`, Wireshark) for firmware upload + monitor enable; store trace for replay reference.
2. **NDK prototype on Android x86/arm64 emulator with USB passthrough or real OTG device** using `libusb` (vendored) to validate transfers.
3. **Firmware loader port:** implement control/bulk sequence from trace; verify firmware responds to WMI `GET_VERSION`.
4. **HTC/WMI layer:** port minimal structs/enums from ath9k_htc; unit-test frame encode/decode on host with captured samples.
5. **Radiotap/PCAP:** integrate `libpcap` writer or tiny custom writer; add unit tests for header correctness.
6. **Foreground service + JNI glue:** Kotlin service drives lifecycle; JNI thread handles event loop; use `AtomicBoolean` for start/stop, structured concurrency.
7. **Channel control & monitor mode:** implement WMI channel set, RX filter, no-encrypt; test against Wi‑Fi AP beacons on 2.4 GHz.
8. **Performance tuning:** adjust URB queue depth, align buffers; evaluate power draw; add USB reset recovery.
9. **QA:** run captures against multiple phones, hubs; validate PCAP in Wireshark; fuzz malformed frames on TX.

### Tooling / dependencies
- Android Studio Giraffe/Koala, NDK r26+, CMake.
- `libusb` (vendored) or Android `UsbDeviceConnection` wrapper.
- `pcap-ng` writer or `libpcap` minimal subset.
- Testing: Wireshark, usbmon, `tshark`, `adb shell dumpsys usb`, `powertop` on Linux host.

## 6. Timeline & Risks
- **Week 1:** USB trace collection; NDK skeleton; firmware upload success on Linux replay harness.
- **Week 2:** HTC/WMI port + monitor RX on Android device; basic PCAP dump.
- **Week 3:** Kotlin UI + foreground service; channel control; stability fixes.
- **Week 4:** TX/injection (optional); performance tuning; documentation + release build.

**Risks & mitigations**
- OTG power limits → require powered hub/Y-cable; implement undervolt warnings.
- Firmware compatibility → fallback to Linux `htc_9271.fw`; if monitor flags missing, investigate firmware patching (risk: proprietary blobs).
- USB API latency → prefer libusb with asynchronous bulk; tune buffer sizes.
- Play Store policy/regulatory concerns → ship via side-load; display legal disclaimer.

## 7. Repository skeleton (implemented)
```
/ (root)
  settings.gradle, build.gradle, gradle/wrapper/
  app/
    build.gradle
    src/main/
      AndroidManifest.xml
      java/com/example/usbwifimonitor/
        ui/MainActivity.kt, FrameLogAdapter.kt
        capture/CaptureForegroundService.kt, CaptureBus.kt, PacketSource.kt
        core/FrameLog.kt, UsbStatus.kt
        usb/UsbDeviceManager.kt, UsbPermissionReceiver.kt
      res/layout/activity_main.xml, item_frame_log.xml
      res/xml/device_filter.xml
      res/values/*.xml, res/drawable/ic_launcher.xml
```

## 8. Reference links (recent online research)
- Linux wireless wiki: ath9k_htc overview, firmware expectations, and monitor mode notes: https://wireless.wiki.kernel.org/en/users/Drivers/ath9k_htc
- ath9k_htc source walk-through (state machine, firmware upload) in Linux kernel tree: https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/tree/drivers/net/wireless/ath/ath9k/htc
- USB Host API behaviors and power considerations (Android docs): https://developer.android.com/guide/topics/connectivity/usb/host
- Foreground services and notification requirements on Android 14/SDK 34: https://developer.android.com/guide/components/foreground-services
- Example community AR9271 monitor-mode efforts (liber80211/monmob): https://forum.xda-developers.com/t/app-for-wifi-monitor-mode.4181359/

The Kotlin module currently ships with a stub `PacketSource` that produces synthetic frames so the app builds and demonstrates the UI flow. Replace `PacketSource.fakeMonitorStream` with the JNI-backed AR9271 pipeline once the NDK stack is finished.
