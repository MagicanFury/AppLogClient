package com.ztechno.applogclient

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@RequiresApi(Build.VERSION_CODES.O)
class MainViewModel : ViewModel() {
  
  var mutableValue by mutableStateOf("...")
    private set
  
  private val updateData = debounce(300, viewModelScope) { locationService: LocationService ->
    val data = locationService.getData()
    mutableValue = "State: \t${data.currActivity} \nHom: \t${data.isHome} \nMov: \t${data.isTravelling} \nInt: \t${"%.2f".format(data.tickJobInterval / 1000f)}s"
    ZLog.error("setting Mutable Value to $mutableValue")
  }
  
  fun loadValue(locationService: LocationService) {
    snapshotFlow { locationService.isTravelling }.onEach {
      updateData(locationService)
    }.launchIn(viewModelScope)
    
    snapshotFlow { locationService.isHome }.onEach {
      updateData(locationService)
    }.launchIn(viewModelScope)
    
    snapshotFlow { locationService.currentActivity }.onEach {
      updateData(locationService)
    }.launchIn(viewModelScope)
  }
  
}