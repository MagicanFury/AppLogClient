package com.ztechno.applogclient.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.floor

@RequiresApi(Build.VERSION_CODES.O)
object ZTime {
  
  fun msSince1970(): Long {
    return System.currentTimeMillis()
  }
  
  fun groupBySecond(): String {
    val now = msSince1970().toString()
    
    return now.substring(0, now.length - 3)
  }
  
  fun timestamp(): String {
    return DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
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