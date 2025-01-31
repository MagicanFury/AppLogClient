package com.ztechno.applogclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.location.ActivityTransitionResult
import com.google.gson.Gson
import com.ztechno.applogclient.http.ZApi
import com.ztechno.applogclient.http.ZApi.KEY_AIRPLANE_MODE
import com.ztechno.applogclient.http.ZApi.KEY_CONNECTION
import com.ztechno.applogclient.http.ZApi.KEY_LOCATION_MODE
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ActivityTransitionUtil
import com.ztechno.applogclient.utils.ZDevice.genActivityData
import com.ztechno.applogclient.utils.ZDevice.genAirplaneOnData
import com.ztechno.applogclient.utils.ZDevice.genBootActionData
import com.ztechno.applogclient.utils.ZDevice.genConnectionData
import com.ztechno.applogclient.utils.ZDevice.genLocationChangeData
import com.ztechno.applogclient.http.ZHttp
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZTime
import com.ztechno.applogclient.utils.debounce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.O)
class MainReceiver(private val locationService: LocationService) : BroadcastReceiver() {
  
  private val scope = CoroutineScope(Dispatchers.IO)
  
  private val handleConnectionChange = debounce(1000, scope) { context: Context, intent: Intent, id: String ->
    val networkInfo = intent.extras?.get("networkInfo") as NetworkInfo?
    val data = genConnectionData(context!!, networkInfo)
    ZLog.write("[$id] Network state: ${networkInfo!!.detailedState} ${Gson().toJson(data)}\n\n")
    ZHttp.send(KEY_CONNECTION, data)
  }
  
  override fun onReceive(context: Context?, intent: Intent?) {
//    ZLog.write("MainReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
    when (intent?.action) {
      "android.net.wifi.STATE_CHANGE" -> handleConnectionChange(context, intent)
      "android.net.wifi.WIFI_STATE_CHANGED" -> {}
      ConnectivityManager.CONNECTIVITY_ACTION -> handleConnectionChange(context, intent)
      Intent.ACTION_AIRPLANE_MODE_CHANGED -> handleAirplaneModeChange(context, intent)
      "android.location.PROVIDERS_CHANGED" -> handleLocationChange(context, intent)
      "android.intent.action.BOOT_COMPLETED" -> handleBootAction(context, intent, true)
      "android.intent.action.LOCKED_BOOT_COMPLETED" -> handleBootAction(context, intent, true)
      "android.intent.action.REBOOT" -> handleBootAction(context, intent, false)
      "android.intent.action.ACTION_SHUTDOWN" -> handleBootAction(context, intent, false)
      "ACTION_PROCESS_ACTIVITY" -> handleActivityTransition(context, intent)
      ACTION_PROCESS_ACTIVITY -> handleActivityTransition(context, intent)
//      Intent.ACTION_SCREEN_ON -> handleScreenToggle(context, intent, true)
//      Intent.ACTION_SCREEN_OFF -> handleScreenToggle(context, intent, false)
//      Intent.ACTION_USER_PRESENT -> handleScreenToggle(context, intent, true)
      else -> ZLog.error("[MainReceiver] Unhandled Receiver (intent.action: ${intent?.action ?: "?"})")
    }
  }
  
  private fun handleConnectionChange(context: Context?, intent: Intent) {
    val time = ZTime.groupBySecond()
    handleConnectionChange.invoke(context!!, intent, time)
    ZLog.write("[$time] handleConnectionChange")
//    scope.launch {
//
//    }
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
  
  private fun handleActivityTransition(context: Context?, intent: Intent) {
    ZLog.write("mainReceiver.handleActivityTransition() action: ${intent.action}\n\t" +
        ZLog.extrasToString(intent.extras)
    )
    
    if (ActivityTransitionResult.hasResult(intent)) {
      val result = ActivityTransitionResult.extractResult(intent)
      if (result != null) {
        for (event in result.transitionEvents) {
          val activityType = ActivityTransitionUtil.toActivityString(event.activityType)
          val transitionType = ActivityTransitionUtil.toTransitionType(event.transitionType)
          
          ZLog.write("[MainReceiver] Activity Transition: activityType: $activityType transitionType: $transitionType")
          try {
            locationService.handleActivityTransition(event,
              genActivityData(context!!, activityType, transitionType, "DEPENDENCY-INJECTION")
            )
          } catch (err: Throwable) {
            ZLog.error(err)
          }

//          val newIntent = Intent(ACTION_PROCESS_ACTIVITY)
//          newIntent.putExtra("activityType", activityType)
//          newIntent.putExtra("transitionType", transitionType)
//          newIntent.putExtra("data", Gson().toJson(genActivityData(context!!, activityType, transitionType, "SEND-BROADCAST")))
//          context.sendBroadcast(newIntent)
        }
      }
    }
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
  
  companion object {
    const val ACTION_PROCESS_ACTIVITY = "com.ztechno.applogclient.ACTION_PROCESS_ACTIVITY_TRANSITIONS" // "ACTION_PROCESS_ACTIVITY"
    
    fun filters(): IntentFilter {
      val filter = IntentFilter()
      filter.addAction(ACTION_PROCESS_ACTIVITY)
      filter.addAction("ACTION_PROCESS_ACTIVITY")
      filter.addAction("android.intent.action.ACTION_SHUTDOWN")
      filter.addAction("android.intent.action.AIRPLANE_MODE")
      filter.addAction("android.intent.action.BOOT_COMPLETED")
      filter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED")
      filter.addAction("android.intent.action.QUICKBOOT_POWERON")
      filter.addAction("android.intent.action.REBOOT")
      filter.addAction("android.location.PROVIDERS_CHANGED")
      filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
      filter.addAction("android.net.wifi.STATE_CHANGE")
      filter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
      return filter
    }
  }
  
}