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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ztechno.applogclient.ZApi.KEY_ACCOUNT_SETUP
import com.ztechno.applogclient.ZApi.ZAccountSetup
import com.ztechno.applogclient.ui.ZTextField
import com.ztechno.applogclient.ui.theme.AppLogClientTheme
import com.ztechno.applogclient.utils.ZHttp
import com.ztechno.applogclient.utils.ZDevice
import com.ztechno.applogclient.utils.ZLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@RequiresApi(Build.VERSION_CODES.O)
class SetupActivity : ComponentActivity() {
  
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var viewModel: SetupViewModel
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    sharedPreferences = getPreferences(MODE_PRIVATE)
    viewModel = SetupViewModel(
      androidId = ZDevice.androidId(applicationContext),
      deviceId = ZDevice.getOrGenerateDeviceId(sharedPreferences)
    )
    
    val btnSize = Modifier.size(width = 360.dp, height = 40.dp)
    val inputSize = Modifier.width(360.dp)
    setContent {
      AppLogClientTheme {
        val ctx = LocalContext.current
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
    
    ZHttp.send(
      KEY_ACCOUNT_SETUP,
      ZAccountSetup(viewModel.androidId, viewModel.deviceId)
    )
    runBlocking {
      delay(2000)
      finish()
    }
  }
  
  private fun onCancelClicked() {
    finish()
  }
}