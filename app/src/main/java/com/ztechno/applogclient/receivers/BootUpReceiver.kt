package com.ztechno.applogclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ZLaunch
import com.ztechno.applogclient.utils.ZLog

open class BootUpReceiver : BroadcastReceiver() {
  
  @RequiresApi(Build.VERSION_CODES.O)
  override fun onReceive(context: Context?, intent: Intent?) {
    ZLog.write("BootUpReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
    
    ZLaunch.ensureServiceRunning(context!!,  LocationService::class.java)
  }
  
}