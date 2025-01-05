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
import com.ztechno.applogclient.utils.ZBattery
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
    val networkInfo: NetworkInfo? = intent.extras?.get("networkInfo") as NetworkInfo?
//    val wifi = context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    val connManager = context!!.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    val mWifi = connManager!!.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
    val mData = connManager!!.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
    var conn = ZConnection(mWifi?.isConnected, mData?.isConnected, networkInfo!!.state.toString())
    ZLog.write("Network state: ${Gson().toJson(conn)}\n============================================\n${networkInfo!!.detailedState}")
    ZHttp.send(KEY_CONNECTION, conn)
  }
  
  private fun handleAirplaneModeChange(context: Context?, intent: Intent) {
    val isTurnedOn = Settings.Global.getInt(
      context?.contentResolver,
      Settings.Global.AIRPLANE_MODE_ON
    ) != 0
    var data = ZAirplaneMode(isTurnedOn)
    ZLog.write("Is Airplane Mode On: $isTurnedOn")
    ZHttp.send(KEY_AIRPLANE_MODE, data)
  }
  
  private fun handleLocationChange(context: Context?, intent: Intent) {
    val mode = Settings.Secure.getInt(
      context?.contentResolver,
      Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF
    )
    val locEnabled = (mode != Settings.Secure.LOCATION_MODE_OFF)
    ZLog.write("Is Location Enabled: $locEnabled")
    ZHttp.send(KEY_LOCATION_MODE, ZApi.ZLocationMode(locEnabled))
  }
  
  private fun handleBootAction(context: Context?, intent: Intent, powerOn: Boolean) {
    ZLog.write(if (powerOn) "Boot Completed Received!" else "Phone is turning off!")
    ZHttp.send(ZApi.KEY_BOOT_ON_OFF, ZApi.ZBootOnOff(powerOn, if (context == null) -1 else ZBattery.getPercentage(context)))
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