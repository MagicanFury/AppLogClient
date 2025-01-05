package com.ztechno.applogclient.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
object ZTime {
  
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