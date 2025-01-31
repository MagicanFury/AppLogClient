package com.ztechno.applogclient.tickers

import android.os.Build
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.http.ZApi
import com.ztechno.applogclient.http.ZApi.KEY_CONNECTION
import com.ztechno.applogclient.services.LocationService.Companion.TICKER_METADATA_TIMEOUT
import com.ztechno.applogclient.utils.ZDevice.genConnectionData
import com.ztechno.applogclient.http.ZHttp
import kotlinx.coroutines.CoroutineScope

@RequiresApi(Build.VERSION_CODES.O)
class ConnectionTicker(scope: CoroutineScope) : ZTickerBase(scope, TICKER_METADATA_TIMEOUT) {
  
  private var prevConnectionData: ZApi.ZConnection? = null
  
  override fun tick(prevTime: Long): Boolean {
    val ctx = LocationApp.applicationContext()
    val connection = genConnectionData(ctx, null)
    if (connection.ssid != prevConnectionData?.ssid) {
      ZHttp.send(KEY_CONNECTION, connection)
      prevConnectionData = connection
      return true
    }
    return false
  }
  
}