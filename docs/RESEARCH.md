# Online research log

## ath9k_htc USB table and firmware behavior
- Linux ath9k_htc `hif_usb.c` lists supported VID/PID pairs, including TL-WN721N (0cf3:9271) and branded clones like Netgear N150 (0846:9030) and Ubiquiti WifiStation (0cf3:b002/b003). Source: https://raw.githubusercontent.com/torvalds/linux/master/drivers/net/wireless/ath/ath9k/hif_usb.c
- Firmware for AR7010/AR9271 (htc_7010.fw, htc_9271.fw) is maintained by Qualcomm Atheros under ClearBSD/MIT/GPL mix; description and licensing in the open-ath9k-htc-firmware README: https://raw.githubusercontent.com/qca/open-ath9k-htc-firmware/master/README

## Android USB Host requirements
- Android USB host documentation highlights per-device user approval via `UsbManager.requestPermission`, OTG hardware requirement, and the need to handle limited bus power: https://developer.android.com/guide/topics/connectivity/usb/host

## Implications for this project
- We mirror the upstream VID/PID table so the app recognizes AR9271 clones without manual edits.
- The firmware README confirms that `htc_9271.fw` is the correct blob to package or request from the user for monitor-mode bringup.
- Android’s USB host constraints reinforce the need for a foreground service with runtime USB permission prompts and user-visible notifications.

## Build tooling provenance
- The Gradle Wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) was generated from the official Gradle distribution referenced in `gradle-wrapper.properties` (Gradle 8.6) via the upstream wrapper task. Source distribution: https://services.gradle.org/distributions/gradle-8.6-bin.zip
