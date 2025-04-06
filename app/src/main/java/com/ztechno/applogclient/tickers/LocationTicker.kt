package com.ztechno.applogclient.tickers

import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.services.LocationService.Companion.NOTIFICATION_ID
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_TRAVEL_INTERVAL
import com.ztechno.applogclient.utils.ZLog
import kotlinx.coroutines.CoroutineScope

@RequiresApi(Build.VERSION_CODES.O)
class LocationTicker(scope: CoroutineScope, private val locationService: LocationService) : ZTickerBase(scope, TICKER_TRAVEL_INTERVAL) {
  
  private var notifManager: NotificationManager? = null
  private var notifText: String = "STARTED"
  
  fun setNotificationManager(notifManager: NotificationManager) {
    this.notifManager = notifManager
  }
  
  private fun updateNotification(txt: String = notifText) {
    notifText = txt
//    ZLog.write("isActive= $isActive isCancelled= $isCancelled")
    val updatedNotification = locationService.notifBuilder
      .setContentTitle(txt)
      .setOngoing(isActive)
    notifManager?.notify(NOTIFICATION_ID, updatedNotification.build())
  }
  
  override fun tick(prevTime: Long): Boolean {
//    val ctx = LocationApp.applicationContext()
    locationService.fetchLocation("LocationTicker") {
      updateNotification(it?.lat.toString().takeLast(3) + ", " + it?.lng.toString().takeLast(3))
    }
//    if (timeSinceLastGps > gpsIntervalThreshold) {
//      fetchLocation("tickJob (tickInterval = $interval, gpsInterval = $gpsIntervalThreshold)")
//    }
    return true
  }
  
  override fun start(forceRestart: Boolean) {
    super.start(forceRestart)
    updateNotification()
  }
  
  override fun cancel(reason: String?) {
    super.cancel(reason)
    updateNotification()
  }
  
  override fun onStarted() {
    super.onStarted()
    updateNotification()
  }
  
  override fun onCompletion(err: Throwable?) {
    super.onCompletion(err)
    updateNotification()
  }
  
  fun checkIfIntervalChanged() {
    val interval = locationService.calcInterval()
    if (this.Interval != interval) {
      this.restartWithInterval(interval, "[LocationTicker] Interval Changed ${this.Interval} -> $interval")
    }
  }
  
}