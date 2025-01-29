package com.ztechno.applogclient.utils

import com.google.android.gms.location.Priority
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ALatLng(val lat: Double, val lng: Double)

object ZGps {
  
  private fun rad(x: Double): Double {
    return x * Math.PI / 180.0
  }
  
  /**
   * Calculates Distance In Meters
   */
  fun distancePrecise(a: ALatLng, b: ALatLng?): Double {
    if (b == null) return -1.0;
    var r = 6378137 // Earth’s mean radius in meter
    var dLat = rad(b.lat - a.lat)
    var dLong = rad(b.lng - a.lng)
    var tmp = sin(dLat / 2) * sin(dLat / 2) +
        cos(rad(a.lat)) * cos(rad(b.lat)) *
        sin(dLong / 2) * sin(dLong / 2)
    var c = 2 * atan2(sqrt(tmp), sqrt(1 - tmp))
    var d = r * c
    return d
  }
  
  fun priorityToString(priority: Int): String {
    return when (priority) {
      Priority.PRIORITY_HIGH_ACCURACY -> "HIGH_ACCURACY"
      Priority.PRIORITY_BALANCED_POWER_ACCURACY -> "BALANCED_POWER_ACCURACY"
      Priority.PRIORITY_LOW_POWER -> "LOW_POWER"
      Priority.PRIORITY_PASSIVE -> "PASSIVE"
      else -> "UNKNOWN (value=${priority})"
    }
  }
}