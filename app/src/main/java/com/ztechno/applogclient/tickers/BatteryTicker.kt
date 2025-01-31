package com.ztechno.applogclient.tickers

import android.os.Build
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.http.ZApi
import com.ztechno.applogclient.http.ZApi.KEY_BATTERY
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_METADATA_TIMEOUT
import com.ztechno.applogclient.utils.ZDevice.genBatteryData
import com.ztechno.applogclient.http.ZHttp
import kotlinx.coroutines.CoroutineScope

@RequiresApi(Build.VERSION_CODES.O)
class BatteryTicker(scope: CoroutineScope) : ZTickerBase(scope, TICKER_METADATA_TIMEOUT) {
  
  private var prevBatteryData: ZApi.ZBattery? = null
  
  override fun tick(prevTime: Long): Boolean {
    val ctx = LocationApp.applicationContext()
    val battery = genBatteryData(ctx)
    if (battery.battery != prevBatteryData?.battery) {
      ZHttp.send(KEY_BATTERY, battery)
      prevBatteryData = battery
      return true
    }
    return false
  }
  
}