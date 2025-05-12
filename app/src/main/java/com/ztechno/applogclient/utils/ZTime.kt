package com.ztechno.applogclient.utils

import android.os.SystemClock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object ZTime {
  
  fun groupBySecond(): String {
    val now = msSince1970().toString()
    return now.substring(0, now.length - 3)
  }
  
  fun msSince1970(): Long {
    return System.currentTimeMillis()
  }
  
  fun timestamp(elapsedRealTimeNanos: Long, useMilliseconds: Boolean): String {
    val nowEpochMillis = System.currentTimeMillis()
    val nowElapsedNanos = SystemClock.elapsedRealtimeNanos()
    
    val deltaNanos = nowElapsedNanos - elapsedRealTimeNanos
    val deltaMillis = deltaNanos / 1_000_000
    
    val ms = nowEpochMillis - deltaMillis
    return DateTimeFormatter
      .ofPattern(if (useMilliseconds) "yyyy-MM-dd HH:mm:ss.SSSSSS" else "yyyy-MM-dd HH:mm:ss")
      .withZone(ZoneOffset.UTC)
      .format(Instant.ofEpochMilli(ms))
  }
  
  fun timestamp(useMilliseconds: Boolean = false): String {
    return DateTimeFormatter
      .ofPattern(if (useMilliseconds) "yyyy-MM-dd HH:mm:ss.SSSSSS" else "yyyy-MM-dd HH:mm:ss")
      .withZone(ZoneOffset.UTC)
      .format(Instant.now())
  }
 
 fun format(ms: Long): String {
   return DateTimeFormatter
     .ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
     .withZone(ZoneOffset.UTC)
     .format(Instant.ofEpochMilli(ms))
 }
 
}