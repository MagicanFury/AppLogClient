package com.ztechno.applogclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ZLaunch
import com.ztechno.applogclient.utils.ZLog

@RequiresApi(Build.VERSION_CODES.O)
class ScreenUnlockReceiver : BroadcastReceiver() {
  
  override fun onReceive(context: Context?, intent: Intent?) {
    ZLog.write("ScreenUnlockReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
    
    ZLaunch.ensureServiceRunning(context!!,  LocationService::class.java)
  }
  
  companion object {
    fun filters(): IntentFilter {
      val filter = IntentFilter()
      filter.addAction(Intent.ACTION_USER_PRESENT)
      filter.addAction(Intent.ACTION_SCREEN_ON)
      filter.addAction(Intent.ACTION_SCREEN_OFF)
      return filter
    }
  }
  
}