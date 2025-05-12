package com.ztechno.applogclient

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.ui.ScrollableWithBottomButton
import com.ztechno.applogclient.ui.ZCardList
import com.ztechno.applogclient.ui.ZComponentActivity
import com.ztechno.applogclient.ui.theme.AppLogClientTheme
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.showLoading
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LogActivity : ZComponentActivity() {
  
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var viewModel: LogViewModel
  
  private lateinit var locationService: LocationService
  private var bound: Boolean = false
  
  private val btnSize = Modifier.size(width = 360.dp, height = 40.dp)
  
  
  private val connection = object : ServiceConnection {
    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      // We've bound to LocalService, cast the IBinder and get LocalService instance.
      val binder = service as LocationService.LocalBinder
      locationService = binder.getService()
      bound = true
    }
    override fun onServiceDisconnected(arg0: ComponentName) {
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
    viewModel = LogViewModel(loadingViewModel).loadValue()
    
    render()
  }
  
  private fun render() {
    showLoading()
    lifecycleScope.launch {
      delay(300)
      setContent {
        AppLogClientTheme {
          val mList = remember { viewModel.items }
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
                mList.addAll(ZLog.getLogHistory())
//                viewModel.loadValue()
                render()
              }
            },
          )
          //        Box(modifier = Modifier.fillMaxSize()) {
          //          Column(
          //            modifier = Modifier
          //              .fillMaxSize()
          //              .padding(bottom = 80.dp) // give space for the button
          ////              .verticalScroll(rememberScrollState()),
          ////            verticalArrangement = Arrangement.SpaceBetween,
          ////            horizontalAlignment = Alignment.CenterHorizontally,
          //          ) {
          //
          //            loadable(loadingViewModel) {
          //              ZCardList(mList, modifier = Modifier.fillMaxHeight(1.0f))
          //            }
          //
          ////            Column(
          ////              modifier = Modifier
          ////                .padding(16.dp, 0.dp)
          ////                .fillMaxWidth()
          ////                .weight(2.0f)
          ////            ) {
          ////              Button(
          ////                modifier = btnSize,
          ////                onClick = {
          ////                  loadingViewModel.waitFor {
          ////                    mList.clear()
          ////                    delay(600)
          ////                    mList.addAll(ZLog.getLogHistory())
          ////                  }
          ////                }
          ////              ) {
          ////                Text("Refresh")
          ////              }
          ////            }
          //          }
          //
          //
          //        }
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