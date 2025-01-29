package com.ztechno.applogclient.utils

import android.content.Context
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.DetectedActivity
import com.ztechno.applogclient.hasActivityRecognitionPermission

object ActivityTransitionUtil {
  
  fun isTravelling(event: ActivityTransitionEvent): Boolean {
    if (event.activityType == DetectedActivity.STILL && event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
      return false
    } else {
      return true
    }
//    if (event.activityType == DetectedActivity.STILL && event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
//      return true
//    }
//    if (event.activityType == DetectedActivity.ON_BICYCLE && event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
//      return true
//    }
//    if (event.activityType == DetectedActivity.IN_VEHICLE && event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
//      return true
//    }
//    return false
  }
  
  fun getTransitions(): MutableList<ActivityTransition> {
    val transitions = mutableListOf<ActivityTransition>()
    
    val activities = listOf(
//      DetectedActivity.STILL,
//      DetectedActivity.WALKING,
//      DetectedActivity.RUNNING,
      DetectedActivity.ON_BICYCLE,
      DetectedActivity.IN_VEHICLE
    )
    
    transitions.add(
      ActivityTransition.Builder()
        .setActivityType(DetectedActivity.STILL)
        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        .build()
    )
    for (activityType in activities) {
      transitions.add(
        ActivityTransition.Builder()
          .setActivityType(activityType)
          .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
          .build()
      )
      transitions.add(
        ActivityTransition.Builder()
          .setActivityType(activityType)
          .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
          .build()
      )
    }
    return transitions
  }
  
//  fun getActivityTransitionRequest() = ActivityTransitionRequest(getTransitions())
  
  fun hasActivityRecognitionPermission(context: Context): Boolean =
    context.hasActivityRecognitionPermission()
  
  fun toActivityString(activity: Int): String {
    return when (activity) {
      DetectedActivity.STILL -> "STILL"
      DetectedActivity.WALKING -> "WALKING"
      DetectedActivity.RUNNING -> "RUNNING"
      DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
      DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
      else -> "UNKNOWN"
    }
  }
  
  fun toTransitionType(transitionType: Int): String {
    return when (transitionType) {
      ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
      ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
      else -> "UNKNOWN"
    }
  }
}