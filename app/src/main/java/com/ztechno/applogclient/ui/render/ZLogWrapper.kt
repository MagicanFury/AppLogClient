package com.ztechno.applogclient.ui.render

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ztechno.applogclient.ui.theme.WarnColor

class ZLogWrapper(private val type: String, private val msg: String) : ZCardItemInterface {
  
  @Composable
  override fun bgColor(): Color {
    return when (type) {
      "info" -> MaterialTheme.colors.surface
      "debug" -> MaterialTheme.colors.secondary
      "warn" -> WarnColor
      "error" -> MaterialTheme.colors.error
      else -> MaterialTheme.colors.surface
    }
  }
  
  override fun getKey(): String {
    return type
  }
  
  override fun getText(): String {
    return msg.trim()
  }
  
}