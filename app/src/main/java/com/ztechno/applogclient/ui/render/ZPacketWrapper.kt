package com.ztechno.applogclient.ui.render

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ztechno.applogclient.http.ZPacket
import com.ztechno.applogclient.services.LocationService

class ZPacketWrapper(private val locationService: LocationService, private val packet: ZPacket) : ZCardItemInterface {
  
  @Composable
  override fun bgColor(): Color {
    return if (isSent()) MaterialTheme.colors.surface else MaterialTheme.colors.error
  }
  
  override fun getKey(): String {
    return packet.key
  }
  
  override fun getText(): String {
    return packet.data.toString().trim()
  }
  
  fun isSent(): Boolean {
    return !(locationService.getPacketHistory(true).contains(packet))
  }
  
}