package com.ztechno.applogclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.http.ZApi.KEY_BOOT_ON_OFF
import com.ztechno.applogclient.http.ZApi.KEY_AIRPLANE_MODE
import com.ztechno.applogclient.http.ZApi.KEY_LOCATION_MODE
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ZDevice.genAirplaneOnData
import com.ztechno.applogclient.utils.ZDevice.genBootActionData
import com.ztechno.applogclient.utils.ZDevice.genLocationChangeData
import com.ztechno.applogclient.http.ZHttp
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZTime
import com.ztechno.applogclient.utils.debounce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainReceiver(private val locationService: LocationService?) : BroadcastReceiver() {
  
  private val scope = CoroutineScope(Dispatchers.IO)
  
  init {
    if (locationService != null) {
      val conTicker = locationService.connectionTicker
      conTicker.bindEvent("WIFI_CHANGE") {
        handleConnectionChange.invoke(null, null, ZTime.timestamp())
      }
    } else {
      ZLog.error("MainReceiver locationService is null")
    }
  }
  
  override fun onReceive(context: Context?, intent: Intent?) {
//    ZLog.write("MainReceiver.onReceive: $intent ${ZLog.extrasToString(intent?.extras)}")
    val invoker = intent?.getStringExtra("invoker")
    when (intent?.action) {
      "android.net.wifi.WIFI_STATE_CHANGED" -> {}
      "android.net.wifi.STATE_CHANGE" -> handleConnectionChange.invoke(context, intent, ZTime.timestamp())
      "android.location.PROVIDERS_CHANGED" -> handleLocationChange.invoke(context, intent, ZTime.timestamp())
      ConnectivityManager.CONNECTIVITY_ACTION -> handleConnectionChange.invoke(context, intent, ZTime.timestamp())
      Intent.ACTION_AIRPLANE_MODE_CHANGED -> handleAirplaneModeChange(context, intent)
      Intent.ACTION_BOOT_COMPLETED -> handleBootAction(context, true, invoker)
      Intent.ACTION_LOCKED_BOOT_COMPLETED -> handleBootAction(context, true, invoker)
      Intent.ACTION_REBOOT -> handleBootAction(context, false, invoker)
      Intent.ACTION_SHUTDOWN -> handleBootAction(context, false, invoker)
//      ACTION_PROCESS_ACTIVITY -> {
//        ZLog.write("ACTION_PROCESS_ACTIVITY 1")
//        handleActivityTransition(context, intent)
//      }
//      "ACTION_PROCESS_ACTIVITY" -> {
//        ZLog.write("ACTION_PROCESS_ACTIVITY 2")
//        handleActivityTransition(context, intent)
//      }
      
      Intent.ACTION_BATTERY_LOW -> locationService?.batteryTicker?.sendBatteryData("BATTERY_LOW")
      Intent.ACTION_BATTERY_OKAY -> locationService?.batteryTicker?.sendBatteryData()
//      Intent.ACTION_SCREEN_ON -> handleScreenToggle(context, intent, true)
//      Intent.ACTION_SCREEN_OFF -> handleScreenToggle(context, intent, false)
//      Intent.ACTION_USER_PRESENT -> handleScreenToggle(context, intent, true)
      else -> ZLog.error("[MainReceiver] Unhandled Receiver (intent.action: ${intent?.action ?: "?"})")
    }
  }
  
  private val handleConnectionChange = debounce(5000, scope) { context: Context?, intent: Intent?, id: String ->
    if (locationService == null) ZLog.write("> handleConnectionChange locationService is null")
    
    val networkInfo = intent?.extras?.get("networkInfo") as NetworkInfo?
    val data = locationService?.connectionTicker?.fetchData()
    if (data != null) {
//      ZLog.write("[$id] (debounced) Network state: ${networkInfo?.detailedState} ${Gson().toJson(data)}\n\n")
//      ZHttp.send(KEY_CONNECTION, data)
      if (arrayOf("CELLULAR", "VPN", "NONE").contains(data.ssid)) {
        planLocationFetch(context, id)
      } else {
        clearPlanLocationJobs()
      }
      if (!data.hasInternet) {
        locationService?.fetchLocation("handleConnectionChange")
      }
    }
  }
  
  private var planLocationJobs = mutableListOf<Job>()
  private fun clearPlanLocationJobs() {
    try {
      planLocationJobs.forEach {
        if (it.isActive) {
          it.cancel("planLocationFetch overwrite!")
        }
      }
      planLocationJobs.clear()
    } catch (e: Throwable) {
      ZLog.error(e)
      planLocationJobs = mutableListOf()
    }
  }
  
  private val planLocationFetch = debounce(5_000, scope) { context: Context?, id: String ->
    ZLog.write("[$id] (debounced) planLocationFetch executed!")
    clearPlanLocationJobs()
    val ctx = context ?: LocationApp.applicationContext()
    planLocationJobs.addAll(
      arrayOf(
        scope.launch {
          delay(30_000)
          Intent(ctx, LocationService::class.java).apply {
            action = LocationService.ACTION_PLANNED_LOCATION
            putExtra("invoker", "planLocationFetch 0:30 min")
            ctx.startForegroundService(this)
          }
        },
        scope.launch {
          delay(60_000)
          Intent(ctx, LocationService::class.java).apply {
            action = LocationService.ACTION_PLANNED_LOCATION
            putExtra("invoker", "planLocationFetch 1:00 min")
            ctx.startForegroundService(this)
          }
        },
        scope.launch {
          delay(60_000 * 3)
          Intent(ctx, LocationService::class.java).apply {
            action = LocationService.ACTION_PLANNED_LOCATION
            putExtra("invoker", "planLocationFetch 3:00 min")
            ctx.startForegroundService(this)
          }
        },
        scope.launch {
          delay(60_000 * 5)
          Intent(ctx, LocationService::class.java).apply {
            action = LocationService.ACTION_PLANNED_LOCATION
            putExtra("invoker", "planLocationFetch 5:00 min")
            ctx.startForegroundService(this)
          }
        }
      )
    )
  }
  
  private val handleLocationChange = debounce(1000, scope) { context: Context?, intent: Intent, id: String ->
    val locationChangeData = genLocationChangeData(context)
    ZLog.write("[$id] (debounced) Is Location Enabled: ${locationChangeData.enabled}")
    ZHttp.send(KEY_LOCATION_MODE, locationChangeData)
    if (locationChangeData.enabled) {
      locationService?.fetchLocation("handleLocationChange")
    }
  }
  
  private fun handleAirplaneModeChange(context: Context?, intent: Intent) {
    val data = genAirplaneOnData(context!!)
    ZLog.write("Is Airplane Mode On: ${data.enabled}")
    ZHttp.send(KEY_AIRPLANE_MODE, data)
    if (!data.enabled) {
      locationService?.fetchLocation("handleAirplaneModeChange")
    }
  }
  
  private fun handleBootAction(context: Context?, powerOn: Boolean, invoker: String?) {
    val data = genBootActionData(context!!, powerOn)
    if (invoker != null) {
      ZLog.write("${if (powerOn) "Boot Completed Received!" else "Phone is turning off!"} invoker: $invoker")
    } else {
      ZLog.write(if (powerOn) "Boot Completed Received!" else "Phone is turning off!")
    }
    ZHttp.send(KEY_BOOT_ON_OFF, data)
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
    const val ACTION_TRANSITION_ACTIVITY_PROXY = "ACTION_TRANSITION_ACTIVITY_PROXY"
    
    fun filters(): IntentFilter {
      val filter = IntentFilter()
//      filter.addAction(ACTION_PROCESS_ACTIVITY)
      filter.addAction("ACTION_PROCESS_ACTIVITY")
      filter.addAction("android.intent.action.ACTION_SHUTDOWN")
      filter.addAction("android.intent.action.AIRPLANE_MODE")
      filter.addAction(Intent.ACTION_BOOT_COMPLETED)
      filter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
      filter.addAction("android.intent.action.QUICKBOOT_POWERON")
      filter.addAction("android.intent.action.REBOOT")
      filter.addAction("android.location.PROVIDERS_CHANGED")
      filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
      filter.addAction("android.net.wifi.STATE_CHANGE")
      filter.addAction("android.net.wifi.WIFI_STATE_CHANGED") // WifiManager.WIFI_STATE_CHANGED_ACTION
      filter.addAction(Intent.ACTION_BATTERY_LOW)
      filter.addAction(Intent.ACTION_BATTERY_OKAY)
      filter.addAction(ACTION_TRANSITION_ACTIVITY_PROXY)
      return filter
    }
  }
  
}