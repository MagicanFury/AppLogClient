package com.ztechno.applogclient.tickers

import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.http.ZApi
import com.ztechno.applogclient.http.ZApi.KEY_BATTERY
import com.ztechno.applogclient.utils.ZDevice.genBatteryData
import com.ztechno.applogclient.http.ZHttp
import kotlinx.coroutines.CoroutineScope

class BatteryTicker(scope: CoroutineScope, interval: Long) : ZTickerBase(scope, interval) {
  
  private var prevBatteryData: ZApi.ZBattery? = null
  
  override fun tick(prevTime: Long): Boolean {
    return sendBatteryData()
  }
  
  fun sendBatteryData(alert: String? = null): Boolean {
    val ctx = LocationApp.applicationContext()
    val battery = genBatteryData(ctx, alert)
    if (battery.battery != prevBatteryData?.battery) {
      ZHttp.send(KEY_BATTERY, battery)
      prevBatteryData = battery
      return true
    }
    return false
  }
  
}