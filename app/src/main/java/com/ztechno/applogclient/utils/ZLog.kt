package com.ztechno.applogclient.utils

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.BuildConfig
import com.ztechno.applogclient.ui.render.ZLogWrapper
import java.lang.reflect.Field
import java.lang.reflect.Method

object ZLog {
  
  private const val TAG = "ZTECHNO"
  
  private val isDev = false // BuildConfig.DEBUG
  
  private val history = mutableListOf<ZLogWrapper>()
  
  init {
    Log.e(TAG, "Logging is ${ if (isDev) "enabled" else "disabled" }")
  }
  
  fun clearLogHistory() {
    history.clear()
  }
  
  fun getLogHistory(): List<ZLogWrapper> {
    return history
  }
  
  fun write(data: Any, simplifyStack: Boolean = false) {
    // TODO: Implement max size instead of clear
    if (!isDev && history.size > 1000) {
      history.clear()
    }
    
    var str: String
    if (data is Exception) {
      str = if (simplifyStack) data.message ?: data.stackTraceToString().take(200) else data.stackTraceToString()
      history.add(ZLogWrapper("error", str))
      Log.e(TAG, str)
    } else if (isDev) {
      str = "$data"
      history.add(ZLogWrapper("info", str))
      Log.i(TAG, str)
    }
  }
  
  fun info(key: Any, value: Any) {
    if (!isDev) return
    val str = "$key $value"
    history.add(ZLogWrapper("debug", str))
    Log.d(TAG, str)
  }
  
  fun warn(msg: Any) {
    if (!isDev) return
    history.add(ZLogWrapper("warn", "$msg"))
    Log.w(TAG, "$msg")
  }
  
  fun error(err: Any) {
    val str = when (err) {
      is Throwable -> err.stackTraceToString()
      else -> "$err"
    }
    history.add(ZLogWrapper("error", str))
    Log.e(TAG, str)
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
  
  fun intentToString(intent: Intent?): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return ""
    }
    if (intent == null) return ""
    return ("" +
        "\t intent: ${toStringReflect(intent, includeMembers = true, includeMethods = false)}\n" +
        "\t package: ${intent?.`package` ?: "null"}\n" +
        "\t action: ${intent?.action ?: "null"}\n" +
        "\t type: ${intent?.type ?: "null"}\n" +
        "\t data: ${intent?.data ?: "null"}\n" +
        "\t dataString: ${intent?.dataString ?: "null"}\n" +
        "\t extras: ${if (intent.extras != null) extrasToString(intent.extras) else "null"}\n"
    )
  }
  
  
  @JvmName("toStrReflect")
  private fun toStringReflect(clsInstance: Any?, includeMembers: Boolean = true, includeMethods: Boolean = false): String {
    return toStringReflect(clsInstance, clsInstance?.javaClass, includeMembers, includeMethods)
  }
  
  private fun toStringReflect(clsInstance: Any?, cls: Class<*>?, includeMembers: Boolean = true, includeMethods: Boolean = false): String {
    if (!isDev) {
      return "?"
    }
    if (clsInstance == null) {
      return "<null>"
    }
    if (clsInstance is String) {
      return clsInstance
    }
    val jCls = cls ?: clsInstance.javaClass
    val fields: Collection<Field> = if (includeMembers) jCls.declaredFields.toList() else emptyList()
    val sb = StringBuilder()
    sb.append("<${clsInstance::class.simpleName}>")
    fields.forEach { field ->
      try {
        if (!field.isAccessible) {
          field.isAccessible = true
        }
        if (!java.lang.reflect.Modifier.isStatic(field.modifiers)) {
          sb.append("\n\t\t").append(field.name).append(": ").append(field.get(clsInstance))
        }
      } catch (_: Throwable) {
      }
    }
    try {
      val members: Collection<Method> = if (includeMethods) jCls.declaredMethods.toList() else emptyList()
      members.forEach { m ->
        try {
          if (m.name.startsWith("get") && m.parameters.size == 1) {
            if (!m.isAccessible) {
              m.isAccessible = true
            }
//            if (!java.lang.reflect.Modifier.isStatic(m.)) {
            val value = m.invoke(clsInstance)
//            val value = m.call(clsInstance)
            sb.append("\n\t\t").append(m.name).append(": ")
            sb.append(value)
            sb.append(" <${m.returnType}>")
          } else {
            sb.append("\n\t\t").append(m.toString())
          }
        } catch (_: Throwable) {
        }
//    YLog.error("member: ${m.name} \t Type: ${m.returnType} \t cls: ${m.javaClass.name} \t toString: $m")
      }
    } catch (_: Throwable) {}
    return sb.toString()
  }
  
}