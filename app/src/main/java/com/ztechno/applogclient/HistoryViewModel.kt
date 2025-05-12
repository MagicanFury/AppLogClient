package com.ztechno.applogclient

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.ui.LoadingViewModel
import com.ztechno.applogclient.ui.render.ZCardItemInterface
import com.ztechno.applogclient.ui.render.ZPacketWrapper
import kotlinx.coroutines.delay

class HistoryViewModel(private val loadingViewModel: LoadingViewModel) : ViewModel() {
  
  var packets = mutableStateListOf<ZCardItemInterface>()
  
  fun loadValue(locationService: LocationService): HistoryViewModel {
    loadingViewModel.waitFor {
      delay(300)
      packets.addAll(locationService.getPacketHistory().map { ZPacketWrapper(locationService, it) }.toMutableStateList())
//    unsentPackets.addAll(locationService.getPacketHistory(unsent = true).toMutableStateList())
    }
    return this
  }
}