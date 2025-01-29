package com.ztechno.applogclient.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import com.ztechno.applogclient.receivers.ScreenUnlockReceiver
import com.ztechno.applogclient.utils.ZLaunch
import com.ztechno.applogclient.utils.ZLog

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
  
  fun initTransitions() {
    val transitions = mutableListOf<ActivityTransition>()
    
    transitions +=
      ActivityTransition.Builder()
        .setActivityType(DetectedActivity.IN_VEHICLE)
        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        .build()
    
    transitions +=
      ActivityTransition.Builder()
        .setActivityType(DetectedActivity.IN_VEHICLE)
        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        .build()
    
    transitions +=
      ActivityTransition.Builder()
        .setActivityType(DetectedActivity.WALKING)
        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        .build()
  }
  
  override fun onInterrupt() {
    TODO("Not yet implemented")
  }
  
  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(screenReceiver)
  }
}