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
import com.google.android.gms.location.DetectedActivity
import com.google.gson.Gson
import com.ztechno.applogclient.ZApi
import com.ztechno.applogclient.ZApi.KEY_AIRPLANE_MODE
import com.ztechno.applogclient.ZApi.KEY_CONNECTION
import com.ztechno.applogclient.ZApi.KEY_LOCATION_MODE
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.services.LocationService.Companion.ACTION_PROCESS_ACTIVITY
import com.ztechno.applogclient.services.LocationService.Companion.ACTION_PROCESS_ACTIVITY_TRANSITIONS
import com.ztechno.applogclient.utils.ActivityTransitionUtil
import com.ztechno.applogclient.utils.ZDevice.genActivityData
import com.ztechno.applogclient.utils.ZDevice.genAirplaneOnData
import com.ztechno.applogclient.utils.ZDevice.genBootActionData
import com.ztechno.applogclient.utils.ZDevice.genConnectionData
import com.ztechno.applogclient.utils.ZDevice.genLocationChangeData
import com.ztechno.applogclient.utils.ZHttp
import com.ztechno.applogclient.utils.ZLog


@RequiresApi(Build.VERSION_CODES.O)
class MainReceiver(private val locationService: LocationService) : BroadcastReceiver() {
  
  override fun onReceive(context: Context?, intent: Intent?) {
//    ZLog.write("MainReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
    when (intent?.action) {
      ConnectivityManager.CONNECTIVITY_ACTION -> handleConnectionChange(context, intent)
      Intent.ACTION_AIRPLANE_MODE_CHANGED -> handleAirplaneModeChange(context, intent)
      "android.location.PROVIDERS_CHANGED" -> handleLocationChange(context, intent)
      "android.intent.action.BOOT_COMPLETED" -> handleBootAction(context, intent, true)
      "android.intent.action.LOCKED_BOOT_COMPLETED" -> handleBootAction(context, intent, true)
      "android.intent.action.REBOOT" -> handleBootAction(context, intent, false)
      "android.intent.action.ACTION_SHUTDOWN" -> handleBootAction(context, intent, false)
      ACTION_PROCESS_ACTIVITY_TRANSITIONS -> handleActivityTransition(context, intent)
//      Intent.ACTION_SCREEN_ON -> handleScreenToggle(context, intent, true)
//      Intent.ACTION_SCREEN_OFF -> handleScreenToggle(context, intent, false)
//      Intent.ACTION_USER_PRESENT -> handleScreenToggle(context, intent, true)
      else -> ZLog.error("[MainReceiver] Unhandled Receiver (intent.action: ${intent?.action ?: "?"})")
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
    fun filters(): IntentFilter {
      val filter = IntentFilter()
      filter.addAction("SOME_ACTION")
      filter.addAction("SOME_OTHER_ACTION")
      filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
      filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
      filter.addAction(Intent.ACTION_PROVIDER_CHANGED)
      filter.addAction(Intent.ACTION_BOOT_COMPLETED)
      filter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
      filter.addAction(Intent.ACTION_REBOOT)
      filter.addAction(Intent.ACTION_SHUTDOWN)
      filter.addAction("android.location.PROVIDERS_CHANGED")
      filter.addAction(ACTION_PROCESS_ACTIVITY_TRANSITIONS)
      return filter
    }
  }
  
}