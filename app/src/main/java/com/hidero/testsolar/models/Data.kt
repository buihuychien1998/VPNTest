package com.hidero.testsolar.models

import android.databinding.ObservableField

class Data {
    var wifiName = ObservableField<String>()
    var wifiStatus = ObservableField<Int>()
    var ipAddress = ObservableField<String>()
    var connectTime = ObservableField<String>()
    var country = ObservableField<String>()
    var ping = ObservableField<String>()
    var macAddress = ObservableField<String>()
    var deviceName = ObservableField<String>()
    var versionAndroid = ObservableField<String>()
}