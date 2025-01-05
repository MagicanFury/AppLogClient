package com.plcoding.backgroundlocationtracking.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.plcoding.backgroundlocationtracking.MainActivity
import com.plcoding.backgroundlocationtracking.utils.ZLog

class FirstInstallReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    ZLog.write("FirstInstallReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
    if (context == null) {
      return
    }
    val p = context.packageManager
    p.setComponentEnabledSetting(
      ComponentName(context, MainActivity@this::class.java),
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      PackageManager.DONT_KILL_APP
    )
  }
}