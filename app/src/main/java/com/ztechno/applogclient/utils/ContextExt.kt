package com.ztechno.applogclient.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.ztechno.applogclient.http.ZApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun Context.hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasActivityRecognitionPermission(): Boolean {
  return (ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.ACTIVITY_RECOGNITION
  ) == PackageManager.PERMISSION_GRANTED)
}

fun CoroutineScope.launchPeriodicAsync(
  repeatMillis: Long,
  action: () -> Unit
) = this.async {
  if (repeatMillis > 0) {
    while (isActive) {
      action()
      delay(repeatMillis)
    }
  } else {
    action()
  }
}

fun <T> debounce(
  waitMs: Long = 300L,
  coroutineScope: CoroutineScope,
  destinationFunction: (T) -> Unit
): (T) -> Unit {
  var debounceJob: Job? = null
  return { param: T ->
    debounceJob?.cancel()
    debounceJob = coroutineScope.launch {
      delay(waitMs)
      destinationFunction(param)
    }
  }
}

fun <T, U> debounce(
  waitMs: Long = 300L,
  coroutineScope: CoroutineScope,
  destinationFunction: (T, U) -> Unit
): (T, U) -> Unit {
  var debounceJob: Job? = null
  return { t: T, u: U ->
    debounceJob?.cancel()
    debounceJob = coroutineScope.launch {
      delay(waitMs)
      destinationFunction(t, u)
    }
  }
}

fun <T, U, V> debounce(
  waitMs: Long = 300L,
  coroutineScope: CoroutineScope,
  destinationFunction: (T, U, V) -> Unit
): (T, U, V) -> Unit {
  var debounceJob: Job? = null
  return { t: T, u: U, v: V ->
    debounceJob?.cancel()
    debounceJob = coroutineScope.launch {
      delay(waitMs)
      destinationFunction(t, u, v)
    }
  }
}

fun String.stripQuotes(): String {
  if (this[0] == '"' && this[this.length-1] == '"') {
    return this.substring(1, this.length - 1)
  }
  return this
}

@RequiresApi(Build.VERSION_CODES.O)
fun Location.toData(): ZApi.ZLocation {
  val speed = if (hasSpeed()) (speed * 3600 / 1000) else null
  return ZApi.ZLocation(latitude, longitude, ZTime.format(time), accuracy, speed)
}