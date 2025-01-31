package com.ztechno.applogclient

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
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
import com.ztechno.applogclient.http.ZPacket
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.ui.ZCard
import com.ztechno.applogclient.ui.ZPacketList
import com.ztechno.applogclient.utils.ZDevice
import com.ztechno.applogclient.utils.ZLog

@RequiresApi(Build.VERSION_CODES.O)
class HistoryActivity : ComponentActivity() {
  
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var viewModel: HistoryViewModel
  
  private lateinit var locationService: LocationService
  private var bound: Boolean = false
  
  private val connection = object : ServiceConnection {
    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      // We've bound to LocalService, cast the IBinder and get LocalService instance.
      val binder = service as LocationService.LocalBinder
      locationService = binder.getService()
      bound = true
      viewModel.loadValue(locationService)
      
      ZLog.error("LocationService BOUND!")
    }
    override fun onServiceDisconnected(arg0: ComponentName) {
      bound = false
      ZLog.error("LocationService UNBOUND!!")
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
    viewModel = HistoryViewModel()
    
    val btnSize = Modifier.size(width = 360.dp, height = 40.dp)
    setContent {
      AppLogClientTheme {
        val mList = remember { viewModel.packets }
//        val ctx = LocalContext.current
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          
//          Column(
//            modifier = Modifier
//              .verticalScroll(rememberScrollState())
//              .weight(1f, false)
//          ) {
//          }
          
          ZPacketList(mList)
          
          Column(
            modifier = Modifier
              .padding(16.dp, 0.dp)
              .fillMaxWidth()
              .weight(2.0f)
          ) {
            Button(
              modifier = btnSize,
              onClick = {
                mList.clear()
                mList.addAll( locationService.getPacketHistory().toMutableStateList())
              }
            ) {
              Text("Refresh")
            }
          }
        }
        
//        Column(
//          modifier = Modifier.fillMaxSize(),
//          horizontalAlignment = Alignment.CenterHorizontally,
//        ) {
//
//          Button(modifier = btnSize, onClick = {
//            onCancelClicked()
//          }) { Text(text = "Cancel") }
//        }
      }
    }
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