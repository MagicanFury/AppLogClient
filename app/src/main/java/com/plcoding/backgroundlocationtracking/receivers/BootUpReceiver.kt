package com.plcoding.backgroundlocationtracking.receivers

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.plcoding.backgroundlocationtracking.services.LocationService
import com.plcoding.backgroundlocationtracking.utils.ZLaunch
import com.plcoding.backgroundlocationtracking.utils.ZLog

open class BootUpReceiver : BroadcastReceiver() {
  
  @RequiresApi(Build.VERSION_CODES.O)
  override fun onReceive(context: Context?, intent: Intent?) {
    ZLog.write("BootUpReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
    
    ZLaunch.ensureServiceRunning(context!!,  LocationService::class.java)
  }
  
}