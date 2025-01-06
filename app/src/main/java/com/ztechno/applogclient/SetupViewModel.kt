package com.ztechno.applogclient

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SetupViewModel(androidId: String = "", deviceId: String = "") : ViewModel() {
  val androidId by mutableStateOf(androidId)
  var deviceId by mutableStateOf(deviceId)
    private set
  
  fun updateDeviceId(input: String) {
    deviceId = input
  }
}