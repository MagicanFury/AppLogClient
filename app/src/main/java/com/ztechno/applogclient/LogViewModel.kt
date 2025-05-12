package com.ztechno.applogclient

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.ztechno.applogclient.ui.LoadingViewModel
import com.ztechno.applogclient.ui.ZComponentActivity
import com.ztechno.applogclient.ui.render.ZCardItemInterface
import com.ztechno.applogclient.utils.ZLog
import kotlinx.coroutines.delay

class LogViewModel(private val loadingViewModel: LoadingViewModel) : ViewModel() {
  
  var items = mutableStateListOf<ZCardItemInterface>()
  
  fun loadValue(): LogViewModel {
    loadingViewModel.waitFor {
      delay(300)
      items.addAll(ZLog.getLogHistory().toMutableStateList())
    }
    return this
  }
}