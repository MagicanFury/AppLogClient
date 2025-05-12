package com.ztechno.applogclient.tickers

import android.os.Build
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.http.ZApi.KEY_CONNECTION
import com.ztechno.applogclient.http.ZApi.ZConnection
import com.ztechno.applogclient.http.ZHttp
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.stripQuotes
import kotlinx.coroutines.CoroutineScope

class ConnectionTicker(scope: CoroutineScope, interval: Long) : ZTickerBase(scope, interval) {
  
  private var prevConnectionData: ZConnection? = null
  
  val lastConnection: ZConnection? get() = prevConnectionData
  
  private val connectivityManager: ConnectivityManager = LocationApp.applicationContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  
  private val currSSID: String get() = cachedSSID ?: ZHttp.getSSID()
  private var cachedSSID: String? = ZHttp.getSSID()
  
  private var onConnectionChange: (() -> Unit)? = null
  
  private val networkCallback: NetworkCallback =
    @RequiresApi(Build.VERSION_CODES.S)
    object : NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
      override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
          val wifiInfo = networkCapabilities.transportInfo as WifiInfo
          cachedSSID = wifiInfo.ssid.stripQuotes()
        } else {
          cachedSSID = null
        }
        onConnectionChange?.invoke()
      }
      
      override fun onLost(network: Network) {
        super.onLost(network)
        // Network lost — likely no internet
        cachedSSID = null
        onConnectionChange?.invoke()
      }
      
      override fun onUnavailable() {
        super.onUnavailable()
        // Couldn't find any network to connect to
        cachedSSID = null
        onConnectionChange?.invoke()
      }
    }
  
  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()
      connectivityManager.registerNetworkCallback(request, networkCallback)
    }
  }
  
  fun bindEvent(eventName: String, callback: () -> Unit) {
    if (eventName != "WIFI_CHANGE") {
      ZLog.error("ConnectionTicker.bindEvent(eventName, callback) Received unknown eventName: $eventName")
      return
    }
    onConnectionChange = callback
  }
  
  override fun tick(prevTime: Long): Boolean {
    val ctx = LocationApp.applicationContext()
    val connection = genConnectionData(ctx, null)
    if (connection.ssid != prevConnectionData?.ssid || connection.hasInternet != prevConnectionData?.hasInternet) {
      ZHttp.send(KEY_CONNECTION, connection)
      ZLog.info("[ConnectionTicker]", "Sending $KEY_CONNECTION: $connection")
      prevConnectionData = connection
      return true
    }
    return false
  }
  
  fun fetchData(): ZConnection {
    val connection = genConnectionData(null, null)
    if (connection.ssid != prevConnectionData?.ssid || connection.hasInternet != prevConnectionData?.hasInternet) {
      ZHttp.send(KEY_CONNECTION, connection)
      ZLog.info("[ConnectionTicker]", "Sending $KEY_CONNECTION: $connection")
      prevConnectionData = connection
    }
    return connection
  }
  
  private fun genConnectionData(context: Context?, networkInfo: NetworkInfo?): ZConnection {
    val wifi = ZHttp.getWifiSettings()
    val data = ZHttp.getDataSettings()
    val hasInternet = (wifi?.state == NetworkInfo.State.CONNECTED || data?.state == NetworkInfo.State.CONNECTED)
    return ZConnection(currSSID, hasInternet)
  }
  
  override fun onCompletion(err: Throwable?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    onConnectionChange = null
    super.onCompletion(err)
  }
  
}