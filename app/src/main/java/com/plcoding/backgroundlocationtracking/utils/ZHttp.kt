package com.plcoding.backgroundlocationtracking.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpPut
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class ZPacket(var key: String, var data: Any, var timestamp: String)

@RequiresApi(Build.VERSION_CODES.O)
object ZHttp {
  
  private const val BASE_URL: String = "https://www.ztechno.nl/applog"
  
  private val retryQueue = mutableListOf<ZPacket>()
  
  
  fun put(endpoint: String, data: Any) {
    val packet = ZPacket("loc-v1", data, ZTime.timestamp())
    val (_, _, result) = "$BASE_URL$endpoint".httpPut()
      .jsonBody(Gson().toJson(packet).toString())
      .responseString()
    val (payload, error) = result
    ZLog.write(payload ?: error!!)
  }
  
  @RequiresApi(Build.VERSION_CODES.O)
  fun send(key: String, data: Any) {
    val packet = ZPacket(key, data, ZTime.timestamp())
    runBlocking {
      try {
        ZLog.write("Sending Packet: ${Gson().toJson(packet)}")
        launch(Dispatchers.IO) {
          val (_, _, result) = BASE_URL.httpPut()
            .jsonBody(Gson().toJson(packet).toString())
            .responseString()
          val (payload, error) = result
          ZLog.write(payload ?: error!!)
          
          if (error != null) {
            retryQueue.add(packet)
          } else {
            retrySend()
          }
        }
      } catch (e: Exception) {
        ZLog.write(e)
      }
    }
  }
  
  private fun retrySend() {
    runBlocking {
      try {
        launch(Dispatchers.IO) {
          var itt = retryQueue.iterator()
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
  
}