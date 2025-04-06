package com.ztechno.applogclient

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ztechno.applogclient.http.ZPacket
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ZPacketWrapper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HistoryViewModel : ViewModel() {
  
  var packets = mutableStateListOf<ZPacketWrapper>()
//  var unsentPackets = mutableStateListOf<ZPacket>()
  
  @RequiresApi(Build.VERSION_CODES.O)
  fun loadValue(locationService: LocationService) {
    packets.addAll(locationService.getPacketHistory().map { ZPacketWrapper(locationService, it) }.toMutableStateList())
//    unsentPackets.addAll(locationService.getPacketHistory(unsent = true).toMutableStateList())
  }
}