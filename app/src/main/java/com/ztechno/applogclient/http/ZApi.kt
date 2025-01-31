package com.ztechno.applogclient.http

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.ztechno.applogclient.utils.ALatLng
import com.ztechno.applogclient.utils.ZLog

@RequiresApi(Build.VERSION_CODES.O)
object ZApi {
  const val KEY_LOCATION = "loc-v2"
  data class ZLocation(var lat: Double, var lng: Double, var gpsTime: String, var accuracy: Float, var speed: Float?) // accuracy in meters
  
  const val KEY_CONNECTION = "con-v5"
  data class ZConnection(var ssid: String, var hasInternet: Boolean)
  
  const val KEY_AIRPLANE_MODE = "airplane-v1"
  data class ZAirplaneMode(var enabled: Boolean)
  
  const val KEY_LOCATION_MODE = "location-toggle-v1"
  data class ZLocationMode(var enabled: Boolean)
  
  const val KEY_BATTERY = "battery-v1"
  data class ZBattery(var battery: Int)
  
  const val KEY_BOOT_ON_OFF = "boot-on-off-v1"
  data class ZBootOnOff(var powerOn: Boolean, var battery: Int)
  
  const val KEY_ACCOUNT_SETUP = "account-setup-v2"
  data class ZAccountSetup(var androidId: String, var deviceId: String, var lat: Double?, var lng: Double?)
  
  const val KEY_ACTIVITY_TRANSITION = "activity-transition-v2"
  data class ZActivityTransition(var activityType: String, var transitionType: String?, var extraData: String? = null)
  
  data class ZUserLocation(var lat: Double, var lng: Double, var description: String)
  
  
  const val KEY_TMP = "tmp-v2"
  data class ZTmp(var isHome: Boolean, var isTravelling: Boolean, val tickJobInterval: Long, val prevActivity: String, val currActivity: String)
  
  @RequiresApi(Build.VERSION_CODES.O)
  fun fetchUserLocations(): List<ALatLng>? {
    try {
      val strUserLocs = ZHttp.fetch("/userlocations")
      ZLog.write("user-locations res: $strUserLocs")
      if (!strUserLocs.isNullOrEmpty()) {
        val itemType = object : TypeToken<List<ZUserLocation>>() {}.type
        return Gson()
          .fromJson<List<ZUserLocation>>(strUserLocs, itemType)
          .map { ALatLng(it.lat, it.lng)  }
          .toMutableList()
      }
    } catch (err: Throwable) {
      ZLog.error(err)
    }
    return null
  }
}