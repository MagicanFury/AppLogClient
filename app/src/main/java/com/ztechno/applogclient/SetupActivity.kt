package com.ztechno.applogclient

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.ztechno.applogclient.http.ZApi
import com.ztechno.applogclient.http.ZApi.KEY_ACCOUNT_SETUP
import com.ztechno.applogclient.http.ZApi.ZAccountSetup
import com.ztechno.applogclient.ui.ZTextField
import com.ztechno.applogclient.ui.theme.AppLogClientTheme
import com.ztechno.applogclient.http.ZHttp
import com.ztechno.applogclient.utils.ZDevice
import com.ztechno.applogclient.utils.ZLog

@RequiresApi(Build.VERSION_CODES.O)
class SetupActivity : ComponentActivity() {
  
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var viewModel: SetupViewModel
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    sharedPreferences = getPreferences(MODE_PRIVATE)
    viewModel = SetupViewModel(
      androidId = ZDevice.androidId(applicationContext),
      deviceId = ZDevice.getOrGenerateDeviceId(sharedPreferences),
    )
    
    val btnSize = Modifier.size(width = 360.dp, height = 40.dp)
    val inputSize = Modifier.width(360.dp)
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
}