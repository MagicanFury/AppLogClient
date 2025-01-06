package com.ztechno.applogclient.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpPut
import com.google.gson.Gson
import com.ztechno.applogclient.LocationApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class ZPacket(var key: String, var data: Any, var timestamp: String, var id: String)

@RequiresApi(Build.VERSION_CODES.O)
object ZHttp {
  
  private const val BASE_URL: String = "https://www.ztechno.nl/applog"
  
  private val planQueue = mutableListOf<ZPacket>()
  private val retryQueue = mutableListOf<ZPacket>()
  
  fun send(key: String, data: Any) {
    val ctx = LocationApp.applicationContext()
    if (ctx == null) {
      throw Error("No Context")
    }
    val packet = ZPacket(key, data, ZTime.timestamp(), ZDevice.androidId())
    if (!isOnline(ctx)) {
      planQueue.add(packet)
      return
    }
    if (planQueue.isNotEmpty()) {
      sendPlanned(planQueue)
    }
    runBlocking {
      try {
        ZLog.write("Sending Packet: ${Gson().toJson(packet)}")
        launch(Dispatchers.IO) {
          val (_, _, result) = BASE_URL.httpPut()
            .jsonBody(Gson().toJson(packet).toString())
            .responseString()
          val (payload, error) = result
          if (error != null) {
            ZLog.write(payload ?: error!!)
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
  
  fun isOnline(context: Context): Boolean {
    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities =
      connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
      if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
        ZLog.info("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
        return true
      } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        ZLog.info("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
        return true
      } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
        ZLog.info("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
        return true
      }
    }
    return false
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