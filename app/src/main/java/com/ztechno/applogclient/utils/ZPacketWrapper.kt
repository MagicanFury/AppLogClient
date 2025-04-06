package com.ztechno.applogclient.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.http.ZPacket
import com.ztechno.applogclient.services.LocationService

@RequiresApi(Build.VERSION_CODES.O)
class ZPacketWrapper(private val locationService: LocationService, val packet: ZPacket) {
  
  fun isSent(): Boolean {
    return !(locationService.getPacketHistory(true).contains(packet))
  }
  
}