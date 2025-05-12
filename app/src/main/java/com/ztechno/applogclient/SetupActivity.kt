package com.ztechno.applogclient

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
//import com.spr.jetpack_loading.components.indicators.LineSpinFadeLoaderIndicator
import com.ztechno.applogclient.http.ZApi
import com.ztechno.applogclient.http.ZApi.KEY_ACCOUNT_SETUP
import com.ztechno.applogclient.http.ZApi.ZAccountSetup
import com.ztechno.applogclient.ui.ZTextField
import com.ztechno.applogclient.ui.theme.AppLogClientTheme
import com.ztechno.applogclient.http.ZHttp
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ZDevice
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.showLoading
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SetupActivity : ComponentActivity() {
  
  private val btnSize = Modifier.size(width = 360.dp, height = 40.dp)
  private val inputSize = Modifier.width(360.dp)
  
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var viewModel: SetupViewModel
  
  private lateinit var locationService: LocationService
  private var bound: Boolean = false
  private val connection = object : ServiceConnection {
    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      // We've bound to LocalService, cast the IBinder and get LocalService instance.
      val binder = service as LocationService.LocalBinder
      locationService = binder.getService()
      bound = true
    }
    override fun onServiceDisconnected(c: ComponentName) {
      bound = false
    }
  }
  
  override fun onResume() {
    super.onResume()
    if (!bound) {
      // Bind to LocationService
      Intent(this, LocationService::class.java).also { intent ->
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
      }
    }
  }
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    sharedPreferences = getPreferences(MODE_PRIVATE)
    viewModel = SetupViewModel(
      androidId = ZDevice.androidId(applicationContext),
      deviceId = ZDevice.getOrGenerateDeviceId(sharedPreferences),
    )
    
    setContent {
      AppLogClientTheme {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
//          LineSpinFadeLoaderIndicator(color = MaterialTheme.colors.primary)
          Text("Loading", color = MaterialTheme.colors.primary)
        }
      }
    }
    render()
  }
  
  fun render() {
    showLoading()
    lifecycleScope.launch {
      val nick = ZDevice.getDeviceId(sharedPreferences)
      if (nick == null) {
        val usr = ZApi.fetchUserInfo()
        if (usr != null) {
          viewModel.updateDeviceId(usr)
        }
      }
      delay(1_000)
      
      setContent {
        AppLogClientTheme {
//        val ctx = LocalContext.current
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            
            ZTextField(label = "Device Id", viewModel = viewModel, modifier = inputSize)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(modifier = btnSize, onClick = {
              onSaveClicked()
            }) { Text(text = "Save") }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(modifier = btnSize, onClick = {
              onCancelClicked()
            }) { Text(text = "Cancel") }
          }
        }
      }
    }
  }
  
  private fun onSaveClicked() {
    val newDeviceId = viewModel.deviceId
    ZLog.write("Device Id Changed to: '${newDeviceId}'")
    val editor = sharedPreferences.edit()
    editor.putString("androidId", viewModel.androidId)
    editor.putString("deviceId", newDeviceId)
    editor.apply()
    
    
    val jsonStr = if (intent.hasExtra("location")) intent.getStringExtra("location") else null
    val pos = if (jsonStr != null) Gson().fromJson(jsonStr, ZApi.ZLocation::class.java) else null
    
    ZHttp.send(
      KEY_ACCOUNT_SETUP,
      ZAccountSetup(viewModel.androidId, viewModel.deviceId, pos?.lat, pos?.lng),
      callback = {
        ZLog.write("ACCOUNT_SETUP response:\n$it")
        finish()
      }
    )
//    runBlocking {
//      delay(80)1
//      finish()
//    }
  }
  
  private fun onCancelClicked() {
    finish()
  }
  
  override fun onStop() {
    super.onStop()
    unbindService(connection)
    bound = false
  }
}