package com.ztechno.applogclient

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZLogWrapper

class LogActivity : ViewModel() {
  
  var items = mutableStateListOf<ZLogWrapper>()
  
  @RequiresApi(Build.VERSION_CODES.O)
  fun loadValue(locationService: LocationService) {
    items.addAll(ZLog.getLogHistory().toMutableStateList())
//    unsentPackets.addAll(locationService.getPacketHistory(unsent = true).toMutableStateList())
  }
}