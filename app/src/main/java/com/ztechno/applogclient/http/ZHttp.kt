package com.ztechno.applogclient.http

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.google.gson.Gson
import com.ztechno.applogclient.LocationApp
import com.ztechno.applogclient.utils.ZDevice
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZTime
import com.ztechno.applogclient.utils.stripQuotes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URLEncoder


data class ZPacket(var key: String, var data: Any, var timestamp: String, var id: String)

@RequiresApi(Build.VERSION_CODES.O)
object ZHttp {
  
  private const val BASE_URL: String = "https://www.ztechno.nl/applog"
  
  val history = mutableListOf<ZPacket>()
  
  private val planQueue = mutableListOf<ZPacket>()
  private val retryQueue = mutableListOf<ZPacket>()
  private val connectivityManager: ConnectivityManager
  private val wifiManager: WifiManager
  
  init {
    val ctx = LocationApp.applicationContext()
    val appCtx = ctx.applicationContext
    connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    wifiManager = appCtx.getSystemService(Context.WIFI_SERVICE) as WifiManager
  }
  
  fun fetch(endpoint: String, params: MutableMap<String, String>? = null): String? {
    val data = params ?: mutableMapOf("androidId" to ZDevice.androidId())
    val queryStr = data.keys.joinToString("&") {
      "$it=${URLEncoder.encode(data[it], "utf-8")}"
    }
    val url = "$BASE_URL$endpoint" + if(queryStr.isNotEmpty()) "?${queryStr}" else ""
    val (_, _, result) = url.httpGet().responseString()
    val (dataStr, error) = result
    if (error != null) {
      ZLog.write(dataStr ?: error)
      throw error
    } else {
      return dataStr
    }
  }
  
  fun send(key: String, data: Any, callback: ((payload: String) -> Any)? = null) {
    val packet = ZPacket(key, data, ZTime.timestamp(), ZDevice.androidId())
    history.add(packet)
    if (history.size > 100) {
      history.removeAt(0)
    }
    if (!isOnline()) {
      planQueue.add(packet)
      return
    }
    if (planQueue.isNotEmpty()) {
      sendPlanned(planQueue)
    }
    runBlocking {
      try {
//        ZLog.write("Sending Packet: ${Gson().toJson(packet)}")
        launch(Dispatchers.IO) {
          TrafficStats.setThreadStatsTag(Thread.currentThread().id.toInt())
          
          val (_, _, result) = BASE_URL.httpPut()
            .jsonBody(Gson().toJson(packet).toString())
            .responseString()
          val (payload, error) = result
          if (error != null) {
            ZLog.write(payload ?: error!!)
          } else {
            callback?.invoke(payload!!)
          }
          
          if (error != null) {
            retryQueue.add(packet)
          } else {
            sendPlanned(retryQueue)
          }
        }
      } catch (e: Exception) {
        ZLog.write(e)
      }
    }
  }
  
  private fun sendPlanned(list: MutableList<ZPacket>) {
    runBlocking {
      try {
        launch(Dispatchers.IO) {
          var itt = list.iterator()
          while (itt.hasNext()) {
            val packet = itt.next()
            ZLog.write("Retry Sending Packet: ${Gson().toJson(packet)}")
            val (_, _, result) = "$BASE_URL".httpPut()
              .jsonBody(Gson().toJson(packet).toString())
              .responseString()
            val (payload, error) = result
            ZLog.write(payload ?: error!!)
            
            if (error == null) {
              itt.remove()
            }
          }
          
        }
      } catch (e: Exception) {
        ZLog.write(e)
      }
    }
  }
  
  private fun isOnline(): Boolean {
    val capabilities =
      connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
      if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
//        ZLog.info("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
        return true
      } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
//        ZLog.info("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
        return true
      } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
//        ZLog.info("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
        return true
      }
    }
    return false
  }
  
  fun getWifiSettings(): NetworkInfo? {
    return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
  }
  
  fun getDataSettings(): NetworkInfo? {
    return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
  }
  
  fun getSSID(): String {
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
      if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
        return "CELLULAR"
      } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        val info = wifiManager.connectionInfo
        return if (info?.ssid != null) info.ssid?.stripQuotes() ?: "WIFI_UNKNOWN" else "WIFI_UNKNOWN"
      } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
        return "ETHERNET"
      } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
        return "VPN"
      }
    }
    return "NONE"
  }
  
  fun put(endpoint: String, data: Any) {
//    val packet = ZPacket("loc-v1", data, ZTime.timestamp())
//    val (_, _, result) = "$BASE_URL$endpoint".httpPut()
//      .jsonBody(Gson().toJson(packet).toString())
//      .responseString()
//    val (payload, error) = result
//    ZLog.write(payload ?: error!!)
  }
  
  
}