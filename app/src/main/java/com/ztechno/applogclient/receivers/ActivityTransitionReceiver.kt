package com.ztechno.applogclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.gson.Gson
import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.receivers.MainReceiver.Companion.ACTION_TRANSITION_ACTIVITY_PROXY
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ActivityTransitionUtil
import com.ztechno.applogclient.utils.ZDevice.genActivityData
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZTime

class ActivityTransitionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    if (intent != null && ACTION_PROCESS_ACTIVITY == intent.action) {
      ZLog.write("[ActivityTransitionReceiver] handleActivityTransition() action: ${intent.action}\n\t" +
        ZLog.extrasToString(intent.extras)
      )
      if (!ActivityTransitionResult.hasResult(intent)) {
        ZLog.error("[ActivityTransitionReceiver] handleActivityTransition hasResult(intent) returned false")
        return
      }
      val result = ActivityTransitionResult.extractResult(intent)
      if (result == null) {
        ZLog.error("[ActivityTransitionReceiver] handleActivityTransition extractResult(intent) returned null")
        return
      }
//      ZLog.error("[ActivityTransitionReceiver] handleActivityTransition WORKS")
      for (event in result.transitionEvents) {
        val activityType = ActivityTransitionUtil.toActivityString(event.activityType)
        val transitionType = ActivityTransitionUtil.toTransitionString(event.transitionType)
        val timestamp = ZTime.timestamp(event.elapsedRealTimeNanos, true)
        
        ZLog.error("[ActivityTransitionReceiver] Activity Transition: activityType: $activityType transitionType: $transitionType")
        
        val ctx = context ?: LocationApp.applicationContext()
        Intent(ctx, LocationService::class.java).apply {
          action = ACTION_TRANSITION_ACTIVITY_PROXY
          putExtra("activityTypeInt", event.activityType)
          putExtra("transitionTypeInt", event.transitionType)
          putExtra("activityType", activityType)
          putExtra("transitionType", transitionType)
          putExtra("data", Gson().toJson(genActivityData(activityType, transitionType, timestamp, "ON-RECEIVE")))
          ctx.startForegroundService(this)
        }
      }
    }
  }
  
  companion object {
    const val ACTION_PROCESS_ACTIVITY = "com.ztechno.applogclient.ACTION_PROCESS_ACTIVITY_TRANSITIONS" // "ACTION_PROCESS_ACTIVITY"
    
    fun sendFakeIntent(context: Context? = null) {
//      ActivityTransitionUtil.getTransitions().forEach {
//        ZLog.write("type: ${it.activityType} => ${ActivityTransitionUtil.toActivityString(it.activityType)}")
//      }
      val activityTypeInt = ActivityTransitionUtil.getTransitions().random().activityType
      val transitionTypeInt = ActivityTransition.ACTIVITY_TRANSITION_ENTER
      
      val activityType = ActivityTransitionUtil.toActivityString(activityTypeInt)
      val transitionType = ActivityTransitionUtil.toTransitionString(transitionTypeInt)
      
//      val newIntent = Intent(ACTION_TRANSITION_ACTIVITY_PROXY)
//      newIntent.putExtra("activityTypeInt", activityTypeInt)
//      newIntent.putExtra("transitionTypeInt", transitionTypeInt)
//      newIntent.putExtra("activityType", activityType)
//      newIntent.putExtra("transitionType", transitionType)
//      newIntent.putExtra("data", Gson().toJson(genActivityData(activityType, transitionType, "SEND-BROADCAST")))
//      ZLog.write("[ActivityTransitionReceiver] Sending fake broadcast intent!")
//      ctx.sendBroadcast(newIntent)
      
      val timestamp = ZTime.timestamp(true)
      val ctx = context ?: LocationApp.applicationContext()
      Intent(ctx, LocationService::class.java).apply {
        action = ACTION_TRANSITION_ACTIVITY_PROXY
        putExtra("activityTypeInt", activityTypeInt)
        putExtra("transitionTypeInt", transitionTypeInt)
        putExtra("activityType", activityType)
        putExtra("transitionType", transitionType)
        putExtra("data", Gson().toJson(genActivityData(activityType, transitionType, timestamp, "Fake-Intent")))
        ctx.startForegroundService(this)
      }
    }
  }
}