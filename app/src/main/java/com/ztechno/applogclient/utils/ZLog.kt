package com.ztechno.applogclient.utils

import android.os.Bundle
import android.util.Log
import com.ztechno.applogclient.BuildConfig

object ZLog {
  
  private const val TAG = "ZTECHNO"
  
  private val isDev = BuildConfig.DEBUG
  
  fun write(data: Any) {
    if (data is Exception)
      Log.e(TAG, data.stackTraceToString())
    else if (isDev)
      Log.i(TAG, "$data")
  }
  
  fun info(key: Any, value: Any) {
    if (!isDev) return
    Log.d(TAG, "$key $value")
  }
  
  fun warn(msg: Any) {
    if (!isDev) return
    Log.w(TAG, "$msg")
  }
  
  fun error(err: Any) {
    when (err) {
      is Throwable -> {
        Log.e(TAG, err.stackTraceToString())
      }
      else -> {
        Log.e(TAG, "$err")
      }
    }
  }
  
  fun extrasToString(extras: Bundle?): String {
    if (!isDev || extras == null) {
      return ""
    }
    val output: MutableList<String> = mutableListOf()
    extras.keySet().map { key ->

//      Z.debug(String.format("%s %s (%s)", key,
//        value.toString(), value.getClass().getName()));
//      Log.e(CustomHooker.TAG, key + " : " + (if (extras.get(key) != null) extras.get(key) else "NULL"))
      var value: Any? = null
      if (extras.get(key) != null) {
        value = extras.get(key)
      }
      output.add("$key: $value <${value?.javaClass?.name ?: "NULL"}>")
      
      if (value?.javaClass?.name.equals("android.os.Bundle")) {
        val subBundle = extras.getBundle(key)
        subBundle?.keySet()?.map { subKey ->
          var subValue: Any? = null
          subValue = subBundle.get(subKey)
          output.add("    $subKey: $subValue <${subValue?.javaClass?.name ?: "NULL"}>")
        }
      }
    }
    return "{\n  ${output.joinToString("\n  ")}\n}"
  }
}