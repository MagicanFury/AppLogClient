package com.plcoding.backgroundlocationtracking.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.plcoding.backgroundlocationtracking.services.LocationService

@RequiresApi(Build.VERSION_CODES.O)
object ZLaunch {
  
  fun ensureServiceRunning(context: Context, cls: Class<*>) {
    if (!isMyServiceRunning(context.applicationContext, cls)) {
      ZLog.write("Service ${toSimpleName(cls)} is not running.")
      Intent(context.applicationContext, cls).apply {
        action = LocationService.ACTION_START
        context.startService(MainActivity@this)
      }
    } else {
      ZLog.write("Service ${toSimpleName(cls)} is running.")
    }
  }
  
  fun toSimpleName(cls: Class<*>): String {
    return cls.name.split("$").first().split(".").last()
  }
  
  fun isMyServiceRunning(context: Context, cls: Class<*>) : Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val services = manager.getRunningServices(Integer.MAX_VALUE)
    val searchStr = cls.name.split("$").first()
    for (service in services) {
      if (searchStr == service.service.className) {
        return true;
      }
    }
    return false;
  }
  
}