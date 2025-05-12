package com.ztechno.applogclient.tickers

import com.ztechno.applogclient.http.ZApi.KEY_HEARTBEAT
import com.ztechno.applogclient.http.ZApi.ZHeartbeat
import com.ztechno.applogclient.http.ZHttp
import com.ztechno.applogclient.utils.ZTime
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_HEARTBEAT_INTERVAL
import kotlinx.coroutines.CoroutineScope

class HeartbeatTicker(scope: CoroutineScope) : ZTickerBase(scope, TICKER_HEARTBEAT_INTERVAL) {
  
  override fun tick(prevTime: Long): Boolean {
    ZHttp.sendOnce(KEY_HEARTBEAT, ZHeartbeat(ZTime.msSince1970()))
    return true
  }
  
}