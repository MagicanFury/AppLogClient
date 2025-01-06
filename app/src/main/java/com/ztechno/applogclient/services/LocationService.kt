package com.ztechno.applogclient.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.ztechno.applogclient.utils.DefaultLocationClient
import com.ztechno.applogclient.utils.LocationClient
import com.ztechno.applogclient.receivers.MainReceiver
import com.ztechno.applogclient.R
import com.ztechno.applogclient.ZApi
import com.ztechno.applogclient.ZApi.KEY_BATTERY
import com.ztechno.applogclient.ZApi.KEY_CONNECTION
import com.ztechno.applogclient.ZApi.ZLocation
import com.ztechno.applogclient.ZApi.KEY_LOCATION
import com.ztechno.applogclient.launchPeriodicAsync
import com.ztechno.applogclient.receivers.ScreenUnlockReceiver
import com.ztechno.applogclient.utils.ZDevice.genBatteryData
import com.ztechno.applogclient.utils.ZDevice.genConnectionData
import com.ztechno.applogclient.utils.ZHttp
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job

@RequiresApi(Build.VERSION_CODES.O)
class LocationService: Service() {
    
    private var mainReceiver: BroadcastReceiver = MainReceiver()
    private var screenReceiver: BroadcastReceiver = ScreenUnlockReceiver()
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    
    private var serviceJob: Job? = null
    private var tickJob: Job? = null
    
    var notifBuilder = NotificationCompat.Builder(this, "location")
        .setContentTitle("...")
        .setSmallIcon(R.drawable.ic_launcher_background)
        .setOngoing(true)

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
        
        initMainReceiver()
        initScreenReceiver()
        
        if (startService(Intent(this, ForegroundEnablingService::class.java)) == null)
            throw RuntimeException("Couldn't find " + ForegroundEnablingService::class.java.simpleName)
    }
    
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initMainReceiver() {
        val filter = IntentFilter()
        filter.addAction("SOME_ACTION")
        filter.addAction("SOME_OTHER_ACTION")
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED)
        filter.addAction(Intent.ACTION_BOOT_COMPLETED)
        filter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
        filter.addAction(Intent.ACTION_REBOOT)
        filter.addAction(Intent.ACTION_SHUTDOWN)
        filter.addAction("android.location.PROVIDERS_CHANGED")
        registerReceiver(mainReceiver, filter)
    }
    
    private fun initScreenReceiver() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_USER_PRESENT)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            ACTION_MANUAL -> getLocationManually()
            Intent.ACTION_BOOT_COMPLETED -> start()
            else -> ZLog.write("LocationService.onStartCommand: $intent ${ZLog.extrasToString(intent?.extras)}")
        }
        return super.onStartCommand(intent, flags, startId)
    }
    
    private fun start() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (tickJob?.isActive == true) {
            ZLog.warn("Can't start tickJob because it's already running")
        } else {
            var prevBatteryData: ZApi.ZBattery? = null
            var prevConnectionData: ZApi.ZConnection? = null
            tickJob = serviceScope.launchPeriodicAsync(60_000) {
                ZLog.write("====================================== | TickJob | ======================================")
                val battery = genBatteryData(applicationContext)
                if (battery.battery != prevBatteryData?.battery) {
                    ZHttp.send(KEY_BATTERY, battery)
                    prevBatteryData = battery
                }
                val connection = genConnectionData(applicationContext, null)
                if (connection.dataEnabled != prevConnectionData?.dataEnabled ||
                    connection.wifiEnabled != prevConnectionData?.wifiEnabled) {
                    ZHttp.send(KEY_CONNECTION, connection)
                    prevConnectionData = connection
                }
            }.job
        }
        
        if (serviceJob?.isActive == true) {
            ZLog.warn("Can't start location-updates service because it's already running")
        } else {
            serviceJob = locationClient
                .getLocationUpdates(1000L * 60L * 10L) // Every 10 minutes
                .catch { e -> e.printStackTrace() }
                .onEach { location ->
                    val loc = ZLocation(location.latitude.toString(), location.longitude.toString(), ZTime.format(location.time), location.accuracy)
                    val lat = location.latitude.toString().takeLast(3)
                    val long = location.longitude.toString().takeLast(3)
                    val updatedNotification = notifBuilder.setContentTitle("$lat, $long")
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
                    
                    ZHttp.send(KEY_LOCATION, loc)
                }
                .launchIn(serviceScope)
        }

//        startForeground(NOTIFICATION_ID, notification.build())
    }
    
    private fun getLocationManually() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ZLog.write("Skipped getLocationManually() because of missing permissions")
            return
        }
        locationClient.getProvider().getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, object : CancellationToken() {
            override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token
            override fun isCancellationRequested() = false
        }).addOnSuccessListener { location ->
            if (location == null) {
                ZLog.write("Location is null! :(")
            }
            val loc = ZApi.ZLocation(location.latitude.toString(), location.longitude.toString(), ZTime.format(location.time), location.accuracy)
            ZHttp.send(ZApi.KEY_LOCATION, loc)
        }
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterReceiver(mainReceiver)
        unregisterReceiver(screenReceiver)
        instance = null
    }

    companion object {
        const val NOTIFICATION_ID = 1
        var instance: LocationService? = null
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_MANUAL = "ACTION_MANUAL"
    }
}