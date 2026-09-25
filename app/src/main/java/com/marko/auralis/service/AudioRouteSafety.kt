package com.marko.auralis.service

import android.media.AudioDeviceInfo
import android.os.Build

object AudioRouteSafety {
    const val HEADPHONE_START_GAIN = 0.50f

    fun isPrivateListeningDevice(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
        type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        type == AudioDeviceInfo.TYPE_USB_HEADSET ||
        type == AudioDeviceInfo.TYPE_HEARING_AID ||
        (Build.VERSION.SDK_INT >= 31 && type == AudioDeviceInfo.TYPE_BLE_HEADSET)
}
