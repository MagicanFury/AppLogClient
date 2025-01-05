package com.ztechno.applogclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ztechno.applogclient.utils.ZLog

class PressHomeReceiver : BroadcastReceiver() {
  
  override fun onReceive(context: Context?, intent: Intent?) {
    ZLog.write("PressHomeReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
  }
  
}