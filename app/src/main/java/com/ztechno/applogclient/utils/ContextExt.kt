package com.ztechno.applogclient.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
//import com.spr.jetpack_loading.components.indicators.LineSpinFadeLoaderIndicator
import com.ztechno.applogclient.http.ZApi
import com.ztechno.applogclient.ui.theme.AppLogClientTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun Context.requestNoBatteryOptimization() {
  try {
    val intent = Intent()
    val packageName = this.packageName
    val pm = this.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
      intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
      intent.data = Uri.parse("package:$packageName")
      this.startActivity(intent)
    }
  } catch (e: Throwable) {
    ZLog.error(e)
  }
}

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
    if (isActive) {
      action()
    }
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

fun Location.toData(): ZApi.ZLocation {
  val speed = if (hasSpeed()) (speed * 3600 / 1000) else null
  return ZApi.ZLocation(latitude, longitude, ZTime.format(time), accuracy, speed, null)
}

fun Location.toData(activityString: String?): ZApi.ZLocation {
  val activityType = activityString?.let { ActivityTransitionUtil.toActivityInt(it) }
  val speed = if (hasSpeed()) (speed * 3600 / 1000) else null
  return ZApi.ZLocation(latitude, longitude, ZTime.format(time), accuracy, speed, activityType)
}

fun ComponentActivity.showLoading() {
  setContent {
    AppLogClientTheme {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
//        LineSpinFadeLoaderIndicator(color = MaterialTheme.colors.primary)
        Text("Loading", color = MaterialTheme.colors.primary)
      }
    }
  }
}