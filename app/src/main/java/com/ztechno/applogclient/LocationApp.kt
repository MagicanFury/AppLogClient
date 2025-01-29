package com.ztechno.applogclient

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.TrafficStats
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import com.ztechno.applogclient.utils.ZLog

class LocationApp: Application() {
    init {
        instance = this
    }
    companion object {
        private var instance: LocationApp? = null
        
        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        ZLog.write("LocationApp.onCreate()")
        StrictMode.setThreadPolicy(ThreadPolicy.Builder().permitAll().build())
//        StrictMode.enableDefaults()
//        StrictMode.setThreadPolicy(
//            StrictMode.ThreadPolicy.Builder()
//                .detectAll()
//                .penaltyLog()
//                .build()
//        )
//        StrictMode.setVmPolicy(
//            VmPolicy.Builder()
//                .detectAll()
//                .penaltyLog()
//                .build()
//        )
        
        TrafficStats.setThreadStatsTag(Thread.currentThread().id.toInt())
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location",
                "Location",
                NotificationManager.IMPORTANCE_MIN
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}