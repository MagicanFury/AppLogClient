package com.ztechno.applogclient.tickers

import android.app.NotificationManager
import com.google.android.gms.location.DetectedActivity
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.services.LocationService.Companion.NOTIFICATION_ID
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_HOME_INTERVAL
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_MOVING_INTERVAL
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_STILL_INTERVAL
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_TRAVEL_INTERVAL
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_WALK_INTERVAL
import com.ztechno.applogclient.utils.ActivityTransitionUtil
import com.ztechno.applogclient.utils.toData
import kotlinx.coroutines.CoroutineScope

class LocationTicker(scope: CoroutineScope, private val locationService: LocationService) : ZTickerBase(scope, TICKER_TRAVEL_INTERVAL) {
  
  private var notifManager: NotificationManager? = null
  private var notifText: String = "STARTED"
  
  fun setNotificationManager(notifManager: NotificationManager) {
    this.notifManager = notifManager
  }
  
  fun updateNotification() {
    updateNotification(null)
  }
  
  private fun updateNotification(txt: String?) {
    if (txt == null) {
      val pos = locationService.lastKnownLocation?.toData()
      notifText = pos?.lat.toString().takeLast(3) + ", " + pos?.lng.toString().takeLast(3)
    } else if (txt != notifText) {
      notifText = txt
    }
//    ZLog.write("isActive= $isActive isCancelled= $isCancelled")
    val updatedNotification = locationService.notifBuilder
      .setContentTitle(notifText)
      .setContentText(locationService.currentActivity)
      .setOngoing(isActive)
    notifManager?.notify(NOTIFICATION_ID, updatedNotification.build())
  }
  
  override fun tick(prevTime: Long): Boolean {
//    val ctx = LocationApp.applicationContext()
    if (!locationService.locationUpdatesEnabled) {
      locationService.fetchLocation("LocationTicker") {
        val pos = it ?: locationService.lastKnownLocation?.toData()
        updateNotification(pos?.lat.toString().takeLast(3) + ", " + pos?.lng.toString().takeLast(3))
      }
    }
//    if (timeSinceLastGps > gpsIntervalThreshold) {
//      fetchLocation("tickJob (tickInterval = $interval, gpsInterval = $gpsIntervalThreshold)")
//    }
    return true
  }
  
  override fun start(forceRestart: Boolean) {
    super.start(forceRestart)
    if (locationService.currentActivity == "STILL") {
      locationService.stopLocationUpdates()
    } else {
      locationService.updateLocationInterval(interval)
    }
    locationService.fetchLocation("LocationTicker.start()") {
      updateNotification(it?.lat.toString().takeLast(3) + ", " + it?.lng.toString().takeLast(3))
    }
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
    locationService.fetchLocation("LocationTicker.onCompletion()") {
      updateNotification(it?.lat.toString().takeLast(3) + ", " + it?.lng.toString().takeLast(3))
    }
    updateNotification()
  }
  
  fun checkIfIntervalChanged() {
    val newInterval = calcInterval()
    if (currInterval != newInterval) {
      this.restartWithInterval(newInterval, "Interval Changed $currInterval -> $newInterval")
    } else {
      updateNotification()
    }
  }
  
  private fun calcInterval(): Long {
    if (locationService.isTravelling) {
      return TICKER_TRAVEL_INTERVAL
    }
    if (locationService.isConnectedToUserLocWifi) {
      return TICKER_HOME_INTERVAL
    }
    when (ActivityTransitionUtil.toActivityInt(locationService.currentActivity)) {
      DetectedActivity.WALKING ->  if (locationService.isCloseToUserLoc) TICKER_HOME_INTERVAL else TICKER_WALK_INTERVAL
      DetectedActivity.RUNNING -> if (locationService.isCloseToUserLoc) TICKER_HOME_INTERVAL else TICKER_MOVING_INTERVAL
      DetectedActivity.ON_BICYCLE -> return TICKER_MOVING_INTERVAL
    }
    if (locationService.isCloseToUserLoc) {
      return TICKER_HOME_INTERVAL
    }
    return TICKER_STILL_INTERVAL
  }
  
  fun isTravelling(): Boolean {
    when (ActivityTransitionUtil.toActivityInt(locationService.currentActivity)) {
      DetectedActivity.STILL -> return false
      DetectedActivity.WALKING -> return false
      DetectedActivity.RUNNING -> return false
      DetectedActivity.ON_BICYCLE -> return true
      DetectedActivity.IN_VEHICLE -> return true
    }
    return false
  }
  
}