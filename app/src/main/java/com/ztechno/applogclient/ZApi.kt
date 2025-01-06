package com.ztechno.applogclient

object ZApi {
  const val KEY_LOCATION = "loc-v1"
  data class ZLocation(var lat: String, var lng: String, var gpsTime: String, var accuracy: Float) // accuracy in meters
  
  const val KEY_CONNECTION = "con-v2"
  data class ZConnection(var wifiEnabled: Boolean?, var dataEnabled: Boolean?, var state: String)
  
  const val KEY_AIRPLANE_MODE = "airplane-v1"
  data class ZAirplaneMode(var enabled: Boolean)
  
  const val KEY_LOCATION_MODE = "location-toggle-v1"
  data class ZLocationMode(var enabled: Boolean)
  
  const val KEY_BATTERY = "battery-v1"
  data class ZBattery(var battery: Int)
  
  const val KEY_BOOT_ON_OFF = "boot-on-off-v1"
  data class ZBootOnOff(var powerOn: Boolean, var battery: Int)
  
  const val KEY_ACCOUNT_SETUP = "account-setup-v1"
  data class ZAccountSetup(var androidId: String, var deviceId: String)
}