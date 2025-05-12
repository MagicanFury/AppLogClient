package com.ztechno.applogclient.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.ztechno.applogclient.HistoryActivity
import com.ztechno.applogclient.R
import com.ztechno.applogclient.SetupActivity
import com.ztechno.applogclient.http.ZApi
import com.ztechno.applogclient.http.ZApi.KEY_ACTIVITY
import com.ztechno.applogclient.http.ZApi.KEY_LOCATION
import com.ztechno.applogclient.http.ZApi.ZLocation
import com.ztechno.applogclient.receivers.MainReceiver
import com.ztechno.applogclient.receivers.ScreenUnlockReceiver
import com.ztechno.applogclient.tickers.BatteryTicker
import com.ztechno.applogclient.tickers.ConnectionTicker
import com.ztechno.applogclient.utils.toData
import com.ztechno.applogclient.utils.ALatLng
import com.ztechno.applogclient.utils.ActivityTransitionUtil
import com.ztechno.applogclient.loc.DefaultLocationClient
import com.ztechno.applogclient.loc.LocationClient
import com.ztechno.applogclient.utils.ZGps
import com.ztechno.applogclient.http.ZHttp
import com.ztechno.applogclient.http.ZPacket
import com.ztechno.applogclient.receivers.ActivityTransitionReceiver
import com.ztechno.applogclient.receivers.MainReceiver.Companion.ACTION_TRANSITION_ACTIVITY_PROXY
import com.ztechno.applogclient.tickers.HeartbeatTicker
import com.ztechno.applogclient.tickers.LocationTicker
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel


class LocationService: Service() {
    
    private val logGps = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private lateinit var notifManager: NotificationManager
    
    val locationTicker = LocationTicker(serviceScope, this)
    val connectionTicker = ConnectionTicker(serviceScope, TICKER_METADATA_TIMEOUT)
    val batteryTicker = BatteryTicker(serviceScope, TICKER_METADATA_TIMEOUT)
    private val heartbeatTicker = HeartbeatTicker(serviceScope)
    
    private var mainReceiver: BroadcastReceiver = MainReceiver(this)
    private var screenReceiver: BroadcastReceiver = ScreenUnlockReceiver()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationClient: LocationClient
    private lateinit var locationCallback: LocationCallback
    
    var userLocations: List<ZApi.ZUserLocation> = mutableListOf(ZApi.ZUserLocation(latLngKtown.lat, latLngKtown.lng, "KTOWN"))
        private set
    var lastKnownLocation: Location? = null
    private var lastGpsUpdate: Long = 0
    val isConnectedToUserLocWifi: Boolean get() = locationClient.isAtUserLocUsingWifi()
    var isCloseToUserLoc by mutableStateOf(false)
        private set
    var isTravelling by mutableStateOf(false)
    var currentActivity by mutableStateOf("STILL")
    
    var locationUpdatesEnabled: Boolean = false
    
    private val gpsPriority: Int get() = Priority.PRIORITY_HIGH_ACCURACY
    private val timeSinceLastGps: Long get() = ZTime.msSince1970() - lastGpsUpdate // location.time
    lateinit var notifBuilder: NotificationCompat.Builder

    fun clearPacketHistory() {
        ZHttp.history.clear()
    }
    
    fun getPacketHistory(unsent: Boolean = false): List<ZPacket> {
        return if (unsent) (ZHttp.retryQueue + ZHttp.planQueue).toTypedArray().toList() else ZHttp.history
    }
    
    fun getData(): ZApi.ZActivity {
        return ZApi.ZActivity(locationTicker.isActive, isCloseToUserLoc, isTravelling, locationTicker.currInterval, "?", currentActivity)
    }
    
    override fun onBind(intent: Intent?): IBinder {
        ZLog.info("[Service]", "onBind")
        start(false)
        return LocalBinder()
    }
    
    inner class LocalBinder : Binder() {
        fun getService() = this@LocationService
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "MissingPermission")
    override fun onCreate() {
        super.onCreate()
        instance = this
        notifBuilder = NotificationCompat.Builder(applicationContext, "location")
            .setContentTitle("...")
            .setContentText("...")
            .setSmallIcon(R.mipmap.ic_launcher_blue)
            .setOngoing(true)
        notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(NOTIFICATION_ID, notifBuilder.build())
        locationTicker.setNotificationManager(notifManager)
        ZLog.info("[Service]", "Settings gpsPriority: ${ZGps.priorityToString(gpsPriority)}, isCloseToUserLoc: $isCloseToUserLoc, isTravelling: $isTravelling")
        ZLog.info("[Service]", "Registering FusedLocationProviderClient")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
//                val location = locationClient.getUserLocUsingWifi() ?: result.lastLocation
                var i = 1
                result.locations.forEach { location ->
                    isCloseToUserLoc = locationClient.isCloseToUserLocations(location) // Check if home
                    handleLocation("FUsedClient->requestLocationUpdates ${i++}", location)
                }
            }
        }
        ZLog.info("[Service]", "Registering DefaultLocationClient")
        locationClient = DefaultLocationClient(this, applicationContext, fusedLocationClient, gpsPriority)
        ZLog.info("[Service]", "Registering User Locations")
        userLocations = ZApi.fetchUserLocations() ?: userLocations
        ZLog.info("[Service]", "Registering MainReceiver")
        registerReceiver(mainReceiver, MainReceiver.filters())
        ZLog.info("[Service]", "Registering ScreenUnlockReceiver")
        registerReceiver(screenReceiver, ScreenUnlockReceiver.filters())
        
        val context = applicationContext
        val intent = Intent(context, ActivityTransitionReceiver::class.java).apply {
            action = ActivityTransitionReceiver.ACTION_PROCESS_ACTIVITY
        }
        val pIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val request = ActivityTransitionRequest(ActivityTransitionUtil.getTransitions())
        val task = ActivityRecognition.getClient(applicationContext).requestActivityTransitionUpdates(request, pIntent)
//        val task = client.requestActivityUpdates(5000L, pIntent)
        task.addOnSuccessListener {
            ZLog.write("[Service] Starting Activity Recognition!")
//            context.registerReceiver(broadcastReceiver, IntentFilter(TRANSITIONS_RECEIVER_ACTION))
        }.addOnFailureListener {
            ZLog.error("[Service] Activity Recognition Error: ${it.stackTraceToString()}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ZLog.write("[Service] onStartCommand() $flags $startId")
        when (intent?.action) {
            // intent is null upon unexpecting crash
            null -> start()
            Intent.ACTION_BOOT_COMPLETED -> start()
            ACTION_BOOT_COMPLETED -> {
                start()
                mainReceiver.onReceive(applicationContext, Intent(Intent.ACTION_BOOT_COMPLETED).apply {
                    putExtra("invoker", intent.getStringExtra("invoker"))
                })
            }
            ACTION_START -> start()
            ACTION_STOP -> stop()
            ACTION_RESTART -> {
                isTravelling = !isTravelling
                start(forceRestart = true)
            }
            
            ACTION_PLANNED_LOCATION -> {
                val invoker = intent.getStringExtra("invoker") ?: "ACTION_PLANNED_LOCATION"
                fetchLocation(invoker)
            }
            
            ACTION_TRANSITION_ACTIVITY_PROXY -> {
                handleActivityTransition(intent)
            }
            
            ACTION_MANUAL -> {
                ActivityTransitionReceiver.sendFakeIntent()
                fetchLocation("USER_INPUT")
            }
            
            ACTION_SET_HOME_WEESP -> {
                ZLog.write("[Service] Setting Home to Weesp")
                userLocations = mutableListOf(ZApi.ZUserLocation(latLngWeesp.lat, latLngWeesp.lng, "WEESP"))
                fetchLocation("HOME_CHANGED")
            }
            ACTION_SET_HOME_KTOWN -> {
                ZLog.write("[Service] Setting Home to KTown")
                userLocations = mutableListOf(ZApi.ZUserLocation(latLngKtown.lat, latLngKtown.lng, "KTOWN"))
                fetchLocation("HOME_CHANGED")
            }
            ACTION_SETUP_DEVICE -> {
                fetchLocation("DEVICE_SETUP") {
                    val intent2 = Intent(applicationContext, SetupActivity::class.java)
                    intent2.setFlags(FLAG_ACTIVITY_NEW_TASK)
                    if (it != null) intent2.putExtra("location", Gson().toJson(it))
                    applicationContext.startActivity(intent2)
                }
            }
            ACTION_SHOW_HISTORY -> {
                val intent3 = Intent(applicationContext, HistoryActivity::class.java)
                intent3.setFlags(FLAG_ACTIVITY_NEW_TASK)
                applicationContext.startActivity(intent3)
            }
            else -> ZLog.error("[Service] unknown onStartCommand: $intent ${ZLog.intentToString(intent)}")
        }
        return super.onStartCommand(intent, flags, startId)
    }
    
    private fun start(forceRestart: Boolean = false) {
        ZLog.warn("[Service] start()")
//        startActivityRecognition(forceRestart)
//        startLocationJob(forceRestart)
        locationTicker.start(forceRestart)
        batteryTicker.start(forceRestart)
        heartbeatTicker.start(forceRestart)
        connectionTicker.start(forceRestart)
//        ensureLocationTicker.start(forceRestart = false)
        
//        startForeground(NOTIFICATION_ID, notification.build())
    }
    
    fun fetchLocation(invoker: String, callback: ((loc: ZLocation?) -> Unit?)? = null) {
        locationClient.fetchLocationSmart(serviceScope) { location ->
            handleLocation(invoker, location, callback)
        }
    }
    
    fun handleLocation(invoker: String, location: Location?, callback: ((loc: ZLocation?) -> Unit?)? = null) {
        ZLog.info("[Service]", "fetchLocation Invoked by $invoker")
        if (location == null) {
            ZLog.error("[LocationService] Location is null! :( invoker: $invoker")
            locationTicker.updateNotification()
            return
        }
        lastKnownLocation = location
        isCloseToUserLoc = locationClient.isCloseToUserLocations(location) // Check if home
        val closestUserLocDistance = locationClient.getClosestUserLocDistance(location)
        
        if (logGps) {
            ZLog.info("[Service]", "fetchLocation invoked " +
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "isMock: ${location.isMock}, " else "") +
                "timeSinceLastGps: ${"%.2f".format(timeSinceLastGps/1000.0)} s, " +
                "distanceFromUserLoc: ${if (closestUserLocDistance == Double.MAX_VALUE) "-1" else  "%.2f".format(closestUserLocDistance)} m, " +
                "isCloseToUserLoc: $isCloseToUserLoc"
            )
        }
        lastGpsUpdate = ZTime.msSince1970() // location.time
        
        val loc = location.toData(currentActivity)
        ZHttp.send(KEY_LOCATION, loc)
        callback?.invoke(loc)
        locationTicker.checkIfIntervalChanged()
    }
    
    fun updateLocationInterval(intervalMillis: Long) {
        stopLocationUpdates()
        
        val request = LocationRequest
            .Builder(gpsPriority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2L)
            .setMaxUpdateDelayMillis(intervalMillis * 2L)
            .build()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ZLog.error("[Service] updateLocationInterval doesn't have ACCESS_FINE_LOCATION permissions!")
            return
        }
        locationUpdatesEnabled = true
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
        ZLog.error("[Service] Location updates started with interval: ${"%.2f".format((intervalMillis / 1000f))}s")
    }
    
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationUpdatesEnabled = false
    }
    
    private fun handleActivityTransition(intent: Intent) {
        ZLog.error("[mainReceiver] Received broadcast intent!")
        val activityType = intent.getIntExtra("activityTypeInt", -1)
        val transitionType = intent.getIntExtra("transitionTypeInt", -1)
        val jsonStr = intent.getStringExtra("data")
        val data = if (jsonStr != null) Gson().fromJson(jsonStr, ZApi.ZActivityTransition::class.java) else null
        
        ZLog.write("[MainReceiver] handleActivityTransition(intent: Intent): \n\tactivityType: $activityType, \n\ttransitionType: $transitionType, \n\tactivityTransition: $data")
        
        if (data == null) {
            ZLog.error("[MainReceiver] handleActivityTransition(intent: Intent): \n\tdata is null!!!!")
            return
        }
    
        val prevActivity = currentActivity
        if (transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            currentActivity = data.activityType
            ZHttp.send(KEY_ACTIVITY, ZApi.ZActivity(locationTicker.isActive, isCloseToUserLoc, isTravelling, locationTicker.currInterval, prevActivity, currentActivity))
        }
        isTravelling = locationTicker.isTravelling()
        locationTicker.updateNotification()
        
        ZLog.write("[LocationService] Activity Transition: " +
            "activityType: ${data.activityType} " +
            "transitionType: ${data.transitionType}")
        
        fetchLocation("ActivityTransition (activity = ${data.activityType}, transition = ${data.transitionType})") {
            locationTicker.checkIfIntervalChanged()
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun stop() {
        ZLog.warn("[LocationService] stop()")
//        tryStopActivityRecognition()
        locationTicker.cancel("[LocationService] stop()")
        batteryTicker.cancel("[LocationService] stop()")
        heartbeatTicker.cancel("[LocationService] stop()")
        connectionTicker.cancel("[LocationService] stop()")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        ZLog.warn("[LocationService] onDestroy()")
        notifManager.cancel(NOTIFICATION_ID)
//        tryStopActivityRecognition()
        locationTicker.cancel("[LocationService] onDestroy()")
        heartbeatTicker.cancel("[LocationService] onDestroy()")
        batteryTicker.cancel("[LocationService] onDestroy()")
        connectionTicker.cancel("[LocationService] onDestroy()")
        stopLocationUpdates()
        serviceScope.cancel()
        ZLog.info("[LocationService]", "Unregistering MainReceiver")
        unregisterReceiver(mainReceiver)
        ZLog.info("[LocationService]", "Unregistering ScreenUnlockReceiver")
        unregisterReceiver(screenReceiver)
        instance = null
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        var instance: LocationService? = null
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESTART = "ACTION_RESTART"
        const val ACTION_MANUAL = "ACTION_MANUAL"
        const val ACTION_SETUP_DEVICE = "ACTION_SETUP_DEVICE"
        const val ACTION_SET_HOME_WEESP = "ACTION_SET_HOME_WEESP"
        const val ACTION_SET_HOME_KTOWN = "ACTION_SET_HOME_KTOWN"
        const val ACTION_SHOW_HISTORY = "ACTION_SHOW_HISTORY"
        const val ACTION_BOOT_COMPLETED = "ACTION_BOOT_COMPLETED"
        const val ACTION_PLANNED_LOCATION = "ACTION_PLANNED_LOCATION"
        
        val latLngWeesp = ALatLng(52.3114443, 5.0226505)
        val latLngKtown = ALatLng(52.24263440, 5.11889450)

        // Prod
        const val TICKER_HEARTBEAT_INTERVAL = 1000L * 60L * 5L  // Every 5 minutes
        const val TICKER_METADATA_TIMEOUT   = 1000L * 60L * 5L  // Every 5 minutes
        const val TICKER_HOME_INTERVAL      = 1000L * 60L * 15L // Every 15 minutes
        const val TICKER_STILL_INTERVAL     = 1000L * 60L * 10L // Every 10 minutes
        const val TICKER_WALK_INTERVAL      = 1000L * 60L * 2L  // Every 2 minutes
        const val TICKER_MOVING_INTERVAL    = 1000L * 60L       // Every 40 seconds
        const val TICKER_TRAVEL_INTERVAL    = 1000L * 30L       // Every 20 seconds
        
        // Dev
//        const val TICKER_HOME_INTERVAL = 1000L * 60L * 5L // Every 5 minutes
//        const val TICKER_STILL_INTERVAL = 1000L * 60L // Every 60 seconds
//        const val TICKER_TRAVEL_INTERVAL = 1000L * 15L // Every 15 seconds
        
    }
}