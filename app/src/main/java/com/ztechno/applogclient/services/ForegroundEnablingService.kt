package com.ztechno.applogclient.services

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.ztechno.applogclient.R
import com.ztechno.applogclient.services.LocationService.Companion.NOTIFICATION_ID

@RequiresApi(Build.VERSION_CODES.O)
class ForegroundEnablingService : Service() {
  
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (LocationService.instance == null) throw RuntimeException(LocationService::class.java.simpleName + " not running")
    
    //Set both services to foreground using the same notification id, resulting in just one notification
    startForeground(LocationService.instance!!)
    startForeground(this)
    
    //Cancel this service's notification, resulting in zero notifications
    stopForeground(true)
    
    //Stop this service so we don't waste RAM.
    //Must only be called *after* doing the work or the notification won't be hidden.
    stopSelf()
    
    return START_NOT_STICKY
  }
  
  private fun startForeground(service: Service) {
//    val notification = Notification.Builder(service).notification
    
    val notification = NotificationCompat.Builder(this, "location")
      .setContentTitle("...")
      .setContentText("....")
      .setSmallIcon(R.drawable.ic_launcher_background)
      .setOngoing(true)
    LocationService.instance?.notifBuilder = notification
    
    service.startForeground(NOTIFICATION_ID, notification.build())
  }
  
  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
  
}