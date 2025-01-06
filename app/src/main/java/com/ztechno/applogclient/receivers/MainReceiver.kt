package com.ztechno.applogclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.ztechno.applogclient.ZApi
import com.ztechno.applogclient.ZApi.KEY_AIRPLANE_MODE
import com.ztechno.applogclient.ZApi.KEY_CONNECTION
import com.ztechno.applogclient.ZApi.KEY_LOCATION_MODE
import com.ztechno.applogclient.ZApi.ZConnection
import com.ztechno.applogclient.ZApi.ZAirplaneMode
import com.ztechno.applogclient.ZApi.ZLocationMode
import com.ztechno.applogclient.ZApi.ZBootOnOff
import com.ztechno.applogclient.utils.ZDevice.genAirplaneOnData
import com.ztechno.applogclient.utils.ZDevice.genBootActionData
import com.ztechno.applogclient.utils.ZDevice.genConnectionData
import com.ztechno.applogclient.utils.ZDevice.genLocationChangeData
import com.ztechno.applogclient.utils.ZHttp
import com.ztechno.applogclient.utils.ZLog

@RequiresApi(Build.VERSION_CODES.O)
class MainReceiver : BroadcastReceiver() {
  
  override fun onReceive(context: Context?, intent: Intent?) {
    ZLog.write("MainReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
    when (intent?.action) {
      ConnectivityManager.CONNECTIVITY_ACTION -> handleConnectionChange(context, intent)
      Intent.ACTION_AIRPLANE_MODE_CHANGED -> handleAirplaneModeChange(context, intent)
      "android.location.PROVIDERS_CHANGED" -> handleLocationChange(context, intent)
      "android.intent.action.BOOT_COMPLETED" -> handleBootAction(context, intent, true)
      "android.intent.action.LOCKED_BOOT_COMPLETED" -> handleBootAction(context, intent, true)
      "android.intent.action.REBOOT" -> handleBootAction(context, intent, false)
      "android.intent.action.ACTION_SHUTDOWN" -> handleBootAction(context, intent, false)
//      Intent.ACTION_SCREEN_ON -> handleScreenToggle(context, intent, true)
//      Intent.ACTION_SCREEN_OFF -> handleScreenToggle(context, intent, false)
//      Intent.ACTION_USER_PRESENT -> handleScreenToggle(context, intent, true)
      else -> ZLog.error("Unhandled Receiver")
    }
  }
  
  private fun handleConnectionChange(context: Context?, intent: Intent) {
    val networkInfo = intent.extras?.get("networkInfo") as NetworkInfo?
    val data = genConnectionData(context!!, networkInfo)
    ZLog.write("Network state: ${Gson().toJson(data)}\n============================================\n${networkInfo!!.detailedState}")
    ZHttp.send(KEY_CONNECTION, data)
  }
  
  private fun handleAirplaneModeChange(context: Context?, intent: Intent) {
    val data = genAirplaneOnData(context!!)
    ZLog.write("Is Airplane Mode On: ${data.enabled}")
    ZHttp.send(KEY_AIRPLANE_MODE, data)
  }
  
  private fun handleLocationChange(context: Context?, intent: Intent) {
    val data = genLocationChangeData((context!!))
    ZLog.write("Is Location Enabled: ${data.enabled}")
    ZHttp.send(KEY_LOCATION_MODE, data)
  }
  
  private fun handleBootAction(context: Context?, intent: Intent, powerOn: Boolean) {
    val data = genBootActionData(context!!, powerOn)
    ZLog.write(if (powerOn) "Boot Completed Received!" else "Phone is turning off!")
    ZHttp.send(ZApi.KEY_BOOT_ON_OFF, data)
  }
  
  private fun handleScreenToggle(context: Context?, intent: Intent, screenOn: Boolean) {
    ZLog.write(if (screenOn) "Phone Screen On" else "Phone Screen Off")
//    if (screenOn) {
//      Intent(context?.applicationContext, LocationService::class.java).apply {
//        action = LocationService.ACTION_START
//        context!!.startService(MainActivity@this)
//      }
//    }
//    ZHttp.send(ZKeys.KEY_BOOT_ON_OFF, ZBootOnOff(powerOn))
  }
  
}