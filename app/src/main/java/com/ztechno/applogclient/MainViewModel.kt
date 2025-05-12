package com.ztechno.applogclient

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

class MainViewModel : ViewModel() {
  
  var mutableValue by mutableStateOf("...")
    private set
  
  var serviceEnabled by mutableStateOf(false)
    private set
  var currentState by mutableStateOf("")
    private set
  var hom by mutableStateOf(false)
    private set
  var mov by mutableStateOf(false)
    private set
  var int by mutableStateOf("")
    private set
  
  private val updateData = debounce(300, viewModelScope) { locationService: LocationService ->
    val data = locationService.getData()
    mutableValue = "Enabled: ${data.serviceEnabled}\nState: \t${data.currActivity} \nHom: \t${data.isCloseToUserLoc} \nMov: \t${data.isTravelling} \nInt: \t${"%.2f".format(data.tickJobInterval / 1000f)}s"
//    ZLog.info("[MainViewModel]", "setting mutable UI value to:\n$mutableValue")
    int = "%.2f".format(data.tickJobInterval / 1000f) + "s"
  }
  
  fun loadValue(locationService: LocationService) {
    snapshotFlow { locationService.isTravelling }.onEach {
      updateData(locationService)
      mov = locationService.isTravelling
    }.launchIn(viewModelScope)
    
    snapshotFlow { locationService.isCloseToUserLoc }.onEach {
      updateData(locationService)
      hom = locationService.isCloseToUserLoc
    }.launchIn(viewModelScope)
    
    snapshotFlow { locationService.currentActivity }.onEach {
      updateData(locationService)
      currentState = locationService.currentActivity
    }.launchIn(viewModelScope)
    
    snapshotFlow { locationService.locationTicker.isActive }.onEach {
      updateData(locationService)
      serviceEnabled = locationService.locationTicker.isActive
    }.launchIn(viewModelScope)
  }
  
}