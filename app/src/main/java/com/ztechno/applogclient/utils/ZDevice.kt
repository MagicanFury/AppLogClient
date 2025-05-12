package com.ztechno.applogclient.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.http.ZApi.ZAirplaneMode
import com.ztechno.applogclient.http.ZApi.ZBootOnOff
import com.ztechno.applogclient.http.ZApi.ZLocationMode
import com.ztechno.applogclient.http.ZApi.ZBattery
import com.ztechno.applogclient.http.ZApi.ZActivityTransition
import kotlin.math.floor

object ZDevice {
  
  @SuppressLint("HardwareIds")
  fun androidId(context: Context = LocationApp.applicationContext()): String {
    val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    return id
  }
  
  fun hasDeviceId(sharedPreferences: SharedPreferences): Boolean {
    return (sharedPreferences.contains("deviceId"))
  }
  
  fun getDeviceId(sharedPreferences: SharedPreferences): String? {
    var value = sharedPreferences.getString("deviceId", null)
    if (value != null) {
      return value
    }
    return null
  }
  
  fun getOrGenerateDeviceId(sharedPreferences: SharedPreferences): String {
    var value = sharedPreferences.getString("deviceId", null)
    if (value != null) {
      return value
    }
    return "Device ${floor(1 + Math.random() * 1000).toInt()}"
  }
  
  fun calcBatteryPercentage(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= 21) {
      val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
      bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
      
    } else {
      val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      val batteryStatus: Intent? = context.registerReceiver(null, iFilter)
      val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)!!
      val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1)!!
      val batteryPct = level / scale.toDouble()
      (batteryPct * 100).toInt()
    }
  }
  
  fun genAirplaneOnData(context: Context): ZAirplaneMode {
    val isTurnedOn = Settings.Global.getInt(
      context.contentResolver,
      Settings.Global.AIRPLANE_MODE_ON
    ) != 0
    return ZAirplaneMode(isTurnedOn)
  }
  
  fun genLocationChangeData(context: Context?): ZLocationMode {
    val mode = Settings.Secure.getInt(
      context?.contentResolver,
      Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF
    )
    val locEnabled = (mode != Settings.Secure.LOCATION_MODE_OFF)
    return ZLocationMode(locEnabled)
  }
  
  fun genBootActionData(context: Context, powerOn: Boolean): ZBootOnOff {
    return ZBootOnOff(powerOn, if (context == null) -1 else ZDevice.calcBatteryPercentage(context))
  }
  
  fun genBatteryData(context: Context, alert: String? = null): ZBattery {
    return ZBattery(calcBatteryPercentage(context), alert)
  }
  
  fun genActivityData(activityType: String, transitionType: String?, timestamp: String?, extraData: String?): ZActivityTransition {
    return ZActivityTransition(activityType, transitionType, timestamp, extraData)
  }
}