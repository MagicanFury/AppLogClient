package com.ztechno.applogclient.utils

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.ztechno.applogclient.ZCardItemInterface

class ZLogWrapper(private val type: String, private val msg: String) : ZCardItemInterface {
  
  override fun bgColor(): Color {
    when (type) {
      "" ->
    }
  }
  
  override fun getKey(): String {
    return type
  }
  
  override fun getText(): String {
    return msg
  }
  
}