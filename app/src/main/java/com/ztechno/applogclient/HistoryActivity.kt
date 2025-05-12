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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
//import com.spr.jetpack_loading.components.indicators.LineSpinFadeLoaderIndicator
import com.ztechno.applogclient.ui.theme.AppLogClientTheme
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.ui.ScrollableWithBottomButton
import com.ztechno.applogclient.ui.ZCardList
import com.ztechno.applogclient.ui.ZComponentActivity
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.ui.render.ZPacketWrapper
import com.ztechno.applogclient.utils.showLoading
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HistoryActivity : ZComponentActivity() {
  
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
      
      ZLog.write("LocationService BOUND!")
    }
    override fun onServiceDisconnected(arg0: ComponentName) {
      bound = false
      ZLog.warn("LocationService UNBOUND!!")
    }
  }
  
  private val btnSize = Modifier.size(width = 360.dp, height = 40.dp)
  
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
    viewModel = HistoryViewModel(loadingViewModel)
    
    render()
  }
  
  private fun render() {
    showLoading()
    lifecycleScope.launch {
      delay(300)
      setContent {
        AppLogClientTheme {
          val mList = remember { viewModel.packets }
//        val ctx = LocalContext.current
          ScrollableWithBottomButton(
            scrollContent = {
              loadable {
                ZCardList(mList, modifier = Modifier.fillMaxHeight(1.0f))
              }
            },
            btnText = "Refresh",
            btnClick = {
              loadingViewModel.waitFor {
                mList.clear()
                mList.addAll( locationService.getPacketHistory().map { ZPacketWrapper(locationService, it) }.toMutableStateList())
//                viewModel.loadValue(locationService)
                render()
              }
            },
          )
        }
      }
    }
  }
  
  override fun onStop() {
    super.onStop()
    unbindService(connection)
    bound = false
  }
}