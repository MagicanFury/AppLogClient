package com.ztechno.applogclient.tickers

import android.os.Build
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_HOME_INTERVAL
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_STILL_INTERVAL
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_TRAVEL_INTERVAL
import kotlinx.coroutines.CoroutineScope

@RequiresApi(Build.VERSION_CODES.O)
class LocationTicker(scope: CoroutineScope, locationService: LocationService) : ZTickerBase(scope, TICKER_TRAVEL_INTERVAL) {
  
  
  fun recalcInterval(isHome: Boolean, isTravelling: Boolean): Long {
    if (isTravelling) {
      return TICKER_TRAVEL_INTERVAL
    }
    if (!isHome) {
      return TICKER_STILL_INTERVAL
    }
    return TICKER_HOME_INTERVAL
  }
  
  override fun tick(prevTime: Long): Boolean {
    val ctx = LocationApp.applicationContext()
    
    
//    if (timeSinceLastGps > gpsIntervalThreshold) {
//      fetchLocation("tickJob (tickInterval = $interval, gpsInterval = $gpsIntervalThreshold)")
//    }
    
    return true
  }
  
}