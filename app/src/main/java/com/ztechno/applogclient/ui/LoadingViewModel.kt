package com.ztechno.applogclient.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ztechno.applogclient.utils.ZLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingViewModel : ViewModel() {
  
  var isLoading = mutableStateOf(true)
  
  init {
    load()
  }
  
  private fun load() {
    viewModelScope.launch {
      delay(1_500)
    }
  }
  
  private fun toggle(showLoading: Boolean) {
    viewModelScope.launch {
      isLoading.value = showLoading
    }
  }
  
  fun waitFor(func: suspend () -> Unit) {
    viewModelScope.launch {
      toggle(true)
      try {
        func()
      } catch (e: Throwable) {
        ZLog.error(e)
      } finally {
        toggle(false)
      }
    }
  }
}