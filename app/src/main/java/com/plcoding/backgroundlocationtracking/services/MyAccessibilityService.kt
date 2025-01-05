package com.plcoding.backgroundlocationtracking.services

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.plcoding.backgroundlocationtracking.receivers.ScreenUnlockReceiver
import com.plcoding.backgroundlocationtracking.utils.ZLaunch
import com.plcoding.backgroundlocationtracking.utils.ZLog

@RequiresApi(Build.VERSION_CODES.O)
class MyAccessibilityService : AccessibilityService() {
  
  private var screenReceiver: BroadcastReceiver = ScreenUnlockReceiver()
  
  override fun onCreate() {
    super.onCreate()
    val filter = IntentFilter()
    filter.addAction(Intent.ACTION_USER_PRESENT)
    filter.addAction(Intent.ACTION_SCREEN_ON)
    filter.addAction(Intent.ACTION_SCREEN_OFF)
    registerReceiver(screenReceiver, filter)
  }
  
  override fun onServiceConnected() {
    serviceInfo.apply {
//      eventTypes = AccessibilityEvent.
    }
      
    Intent(applicationContext, LocationService::class.java).apply {
      action = LocationService.ACTION_START
      startService(this)
    }
  }
  
  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) {
      ZLog.write("Event: null")
      return
    }
//    ZLog.write("LocationService running: ${ZLaunch.isMyServiceRunning(applicationContext, LocationService.javaClass)}")
    ZLog.write("Event: ${event.packageName} -> ${AccessibilityEvent.eventTypeToString(event.eventType)} ${event.action}")
    ZLaunch.ensureServiceRunning(applicationContext, LocationService::class.java)
  }
  
  override fun onInterrupt() {
    TODO("Not yet implemented")
  }
}