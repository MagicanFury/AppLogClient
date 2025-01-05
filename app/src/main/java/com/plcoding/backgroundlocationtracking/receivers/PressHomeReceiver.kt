package com.plcoding.backgroundlocationtracking.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.plcoding.backgroundlocationtracking.utils.ZLog

class PressHomeReceiver : BroadcastReceiver() {
  
  override fun onReceive(context: Context?, intent: Intent?) {
    ZLog.write("PressHomeReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
  }
  
}