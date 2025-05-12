package com.ztechno.applogclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ZLaunch
import com.ztechno.applogclient.utils.ZLog

open class BootUpReceiver : BroadcastReceiver() {
  
  override fun onReceive(context: Context?, intent: Intent?) {
    ZLog.write("BootUpReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
    ZLaunch.ensureServiceRunning(context!!, LocationService::class.java)
    
    val bootUpAction = intent?.action
    
    Intent(context.applicationContext, LocationService::class.java).apply {
      action = LocationService.ACTION_BOOT_COMPLETED
      putExtra("invoker", bootUpAction ?: "Unknown")
      context.startForegroundService(this)
    }
  }
  
}