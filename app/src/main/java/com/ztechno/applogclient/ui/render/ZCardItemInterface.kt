package com.ztechno.applogclient.ui.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

interface ZCardItemInterface {
  
  @Composable
  fun bgColor(): Color
  fun getKey(): String
  fun getText(): String
}