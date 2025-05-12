package com.ztechno.applogclient.http

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.ztechno.applogclient.utils.ZLog

object ZApi {
  const val KEY_HEARTBEAT = "heart"
  data class ZHeartbeat(var ms: Long)
  
  const val KEY_LOCATION = "loc-v3"
  data class ZLocation(var lat: Double, var lng: Double, var gpsTime: String, var accuracy: Float, var speed: Float?, var activityType: Int?) // accuracy in meters
  
  const val KEY_CONNECTION = "con-v5"
  data class ZConnection(var ssid: String, var hasInternet: Boolean)
  
  
  const val KEY_AIRPLANE_MODE = "airplane-v1"
  data class ZAirplaneMode(var enabled: Boolean)
  
  const val KEY_LOCATION_MODE = "location-toggle-v1"
  data class ZLocationMode(var enabled: Boolean)
  
  const val KEY_BATTERY = "battery-v2"
  data class ZBattery(var battery: Int, var alert: String?)
  
  const val KEY_BOOT_ON_OFF = "boot-on-off-v1"
  data class ZBootOnOff(var powerOn: Boolean, var battery: Int)
  
  const val KEY_ACCOUNT_SETUP = "account-setup-v2"
  data class ZAccountSetup(var androidId: String, var deviceId: String, var lat: Double?, var lng: Double?)
  
  data class ZUserLocation(var lat: Double, var lng: Double, var description: String, var wifi: String? = null)
  
  data class ZUserInfo(var nickname: String)
  
  const val KEY_ACTIVITY = "activity-v1"
  data class ZActivity(var serviceEnabled: Boolean, var isCloseToUserLoc: Boolean, var isTravelling: Boolean, val tickJobInterval: Long, val prevActivity: String, val currActivity: String)
  
  // For internal use only
  data class ZActivityTransition(var activityType: String, var transitionType: String?, var timestamp: String? = null, var extraData: String? = null)
  
  fun fetchUserLocations(): List<ZUserLocation>? {
    try {
      val strUserLocs = ZHttp.fetch("/userlocations") ?: return null
      ZLog.write("user-locations res: $strUserLocs")
      if (strUserLocs.isNotEmpty()) {
        val itemType = object : TypeToken<List<ZUserLocation>>() {}.type
        return Gson()
          .fromJson<List<ZUserLocation>>(strUserLocs, itemType)
          .toMutableList()
      }
    } catch (err: Throwable) {
      ZLog.error(err)
    }
    return null
  }
  
  fun fetchUserInfo(): String? {
    try {
      val strUserInfo = ZHttp.fetch("/userinfo") ?: return null
      ZLog.write("user-info res: $strUserInfo")
      if (strUserInfo.isNotEmpty()) {
        val itemType = object : TypeToken<List<ZUserInfo>>() {}.type
        val res = Gson()
          .fromJson<List<ZUserInfo>>(strUserInfo, itemType)
          .toMutableList()
        
        return res.first().nickname
      }
    } catch (err: Throwable) {
      ZLog.error(err)
    }
    return null
  }
}