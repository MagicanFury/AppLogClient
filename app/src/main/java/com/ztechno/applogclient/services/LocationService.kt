package com.ztechno.applogclient.services

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.gson.Gson
import com.ztechno.applogclient.HistoryActivity
import com.ztechno.applogclient.R
import com.ztechno.applogclient.SetupActivity
import com.ztechno.applogclient.http.ZApi
import com.ztechno.applogclient.http.ZApi.KEY_LOCATION
import com.ztechno.applogclient.http.ZApi.ZActivityTransition
import com.ztechno.applogclient.http.ZApi.ZLocation
import com.ztechno.applogclient.utils.hasActivityRecognitionPermission
import com.ztechno.applogclient.utils.hasLocationPermission
import com.ztechno.applogclient.receivers.MainReceiver
import com.ztechno.applogclient.receivers.ScreenUnlockReceiver
import com.ztechno.applogclient.tickers.BatteryTicker
import com.ztechno.applogclient.tickers.ConnectionTicker
import com.ztechno.applogclient.utils.toData
import com.ztechno.applogclient.utils.ALatLng
import com.ztechno.applogclient.utils.ActivityTransitionUtil
import com.ztechno.applogclient.loc.DefaultLocationClient
import com.ztechno.applogclient.loc.LocationClient
import com.ztechno.applogclient.utils.ZDevice.genConnectionData
import com.ztechno.applogclient.utils.ZGps
import com.ztechno.applogclient.http.ZHttp
import com.ztechno.applogclient.http.ZPacket
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
import kotlin.math.floor
import kotlin.math.min


@RequiresApi(Build.VERSION_CODES.O)
class LocationService: Service() {
    
    private val logGps = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    //    private val locationTicker = LocationTicker(serviceScope, this)
    private val batteryTicker = BatteryTicker(serviceScope)
    private val connectionTicker = ConnectionTicker(serviceScope)
    private var locationJob: Job? = null
    
    private var mainReceiver: BroadcastReceiver = MainReceiver(this)
    private var screenReceiver: BroadcastReceiver = ScreenUnlockReceiver()
    
    private lateinit var locationClient: LocationClient
    
    private var activityPendingIntent: PendingIntent? = null
    
    private var thresholdDistanceHome: Double = 50.0
    
    
    private var userLocations: List<ALatLng> = mutableListOf(latLngKtown)
    private var distanceFromHome: Double = 0.0
    
    private var lastGpsUpdate: Long = 0
    private var prevLatLng: ALatLng? = null
    
    var isHome by mutableStateOf(false)
        private set
    var isTravelling by mutableStateOf(false)
        private set
    var currentActivity by mutableStateOf("STILL")
    
    private val gpsPriority: Int get() = Priority.PRIORITY_HIGH_ACCURACY
    
    private val timeSinceLastGps: Long get() = ZTime.msSince1970() - lastGpsUpdate // location.time
    
    var notifBuilder = NotificationCompat.Builder(this, "location")
        .setContentTitle("...")
        .setContentText("...")
        .setSmallIcon(R.drawable.ic_launcher_background)
        .setOngoing(true)

    fun getPacketHistory(): MutableList<ZPacket> {
        return ZHttp.history
    }
    
    fun getData(): ZApi.ZTmp {
        return ZApi.ZTmp(isHome, isTravelling, calcInterval(), "?", currentActivity)
    }
    
    override fun onBind(p0: Intent?): IBinder? {
        return LocalBinder()
    }
    
    inner class LocalBinder : Binder() {
        fun getService() = this@LocationService
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        instance = this
        ZLog.info("[LocationService]", "Settings gpsPriority: ${ZGps.priorityToString(gpsPriority)}, isHome: $isHome, isTravelling: $isTravelling")
        ZLog.info("[LocationService]", "Registering MainReceiver")
        registerReceiver(mainReceiver, MainReceiver.filters())
        ZLog.info("[LocationService]", "Registering ScreenUnlockReceiver")
        registerReceiver(screenReceiver, ScreenUnlockReceiver.filters())
        ZLog.info("[LocationService]", "Registering FusedLocationProviderClient")
        val client = LocationServices.getFusedLocationProviderClient(applicationContext)
        ZLog.info("[LocationService]", "Registering DefaultLocationClient")
        locationClient = DefaultLocationClient(applicationContext, client, gpsPriority)
        ZLog.info("[LocationService]", "Registering User Locations")
        userLocations = ZApi.fetchUserLocations() ?: userLocations
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ZLog.write("[LocationService] onStartCommand()")
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> start()
            ACTION_START -> start()
            ACTION_STOP -> stop()
            ACTION_RESTART -> {
                isTravelling = !isTravelling
                start(forceRestart = true)
            }
            
            ACTION_MANUAL -> fetchLocation("USER_INPUT")
            
            ACTION_SET_HOME_WEESP -> {
                ZLog.write("[LocationService] Setting Home to Weesp")
                userLocations = mutableListOf(latLngWeesp)
                fetchLocation("HOME_CHANGED")
            }
            ACTION_SET_HOME_KTOWN -> {
                ZLog.write("[LocationService] Setting Home to KTown")
                userLocations = mutableListOf(latLngKtown)
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
                val intent = Intent(applicationContext, HistoryActivity::class.java)
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
                applicationContext.startActivity(intent)
            }
            else -> ZLog.warn("[LocationService] onStartCommand: $intent ${ZLog.extrasToString(intent?.extras)}")
        }
        return super.onStartCommand(intent, flags, startId)
    }
    
    private fun start(forceRestart: Boolean = false) {
        ZLog.warn("[LocationService] start()")
        startActivityRecognition()
        startLocationJob(forceRestart)
        batteryTicker.start(forceRestart)
        connectionTicker.start(forceRestart)
//        startForeground(NOTIFICATION_ID, notification.build())
    }
    
//    private fun startTickJob(forceRestart: Boolean) {
//        val interval = if (isTravelling) TICKER_TRAVEL_INTERVAL else TICKER_HOME_INTERVAL
//        if (forceRestart) {
//            tickJob?.cancel()
//        } else if (tickJob?.isActive == true) {
//            ZLog.warn("Can't start tickJob because it's already running")
//            return
//        }
//        ZLog.write("tickJob interval = $interval isTraveling = $isTravelling")
//        tickJob = serviceScope.launchPeriodicAsync(interval) {
//            val now = ZTime.msSince1970()
//
//            if (timeSinceLastGps > gpsIntervalThreshold) {
//                fetchLocation("tickJob (tickInterval = $interval, gpsInterval = $gpsIntervalThreshold)")
//            }
//        }.job
//    }
    
    private fun calcInterval(): Long {
        if (isTravelling) {
            return TICKER_TRAVEL_INTERVAL
        }
        if (!isHome) {
            return TICKER_STILL_INTERVAL
        }
        return TICKER_HOME_INTERVAL
    }
    
    private fun startLocationJob(forceRestart: Boolean) {
        if (forceRestart) {
            locationJob?.cancel("[LocationService] onDestroy()")
            return
        } else if (locationJob?.isActive == true) {
            ZLog.warn("Can't start location-updates service because it's already running")
        }
        ZLog.write("StartLocationServiceJob")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        locationJob = locationClient
            .getLocationUpdates(calcInterval(), onClose = {
                val updatedNotification = notifBuilder.setOngoing(false)
                notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
                startLocationJob(forceRestart = false)
            })
            .catch { e -> ZLog.error(e) }
            .onEach { location ->
                val loc = handleGpsData(location)
                val updatedNotification = notifBuilder
                    .setOngoing(true)
                    .setContentTitle(location.latitude.toString().takeLast(3) + ", " + location.longitude.toString().takeLast(3))
                notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
                if (logGps) {
                    ZLog.warn("getLocationUpdates Invoked by serviceJob (const interval = ?)")
                }
                ZHttp.send(KEY_LOCATION, loc)
            }
            .launchIn(serviceScope)
    }
    
    
    @SuppressLint("MissingPermission", "MutableImplicitPendingIntent")
    fun startActivityRecognition() {
        val context = applicationContext
        if (!hasActivityRecognitionPermission()) {
            ZLog.error("[LocationService] ActivityRecognitionSetup Failed!")
            return
        }
        tryStopActivityRecognition()
        val client = ActivityRecognition.getClient(context);
        val intent = Intent(MainReceiver.ACTION_PROCESS_ACTIVITY)
        val pIntent = PendingIntent.getBroadcast(context, NOTIFICATION_ID, intent, FLAG_MUTABLE or FLAG_UPDATE_CURRENT)
        
        val request = ActivityTransitionRequest(ActivityTransitionUtil.getTransitions())
        val task = client.requestActivityTransitionUpdates(request, pIntent)
//        val task = client.requestActivityUpdates(5000L, pIntent)
        task.addOnSuccessListener {
            ZLog.write("[LocationService] Starting Activity Recognition!")
//            context.registerReceiver(broadcastReceiver, IntentFilter(TRANSITIONS_RECEIVER_ACTION))
        }.addOnFailureListener { ZLog.error("[LocationService] Activity Recognition Error: ${it.stackTraceToString()}") }
        activityPendingIntent = pIntent
    }
    
    private var lastActivityTransitionTime: Long = 0
    fun handleActivityTransition(event: ActivityTransitionEvent, data: ZActivityTransition) {
        lastActivityTransitionTime = ZTime.msSince1970()
        
        val prevActivity = currentActivity
        if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            currentActivity = ActivityTransitionUtil.toActivityString(event.activityType)
            ZHttp.send(ZApi.KEY_TMP, ZApi.ZTmp(isHome, isTravelling, calcInterval(), prevActivity, currentActivity))
        }
        
        val isTravelling = ActivityTransitionUtil.startedOrStoppedMoving(event)
        if (isTravelling != null) {
            this.isTravelling = isTravelling
            if (locationClient.isIntervalChanged(calcInterval())) {
                startLocationJob(forceRestart = true)
                ZLog.write("isTravelling changed to $isTravelling, restarting LocationJob")
            }
        }
        val connection = genConnectionData(applicationContext, null)
//        if (connection.wifiEnabled == false && connection.dataEnabled == true) {
        if (connection.ssid != "NONE") {
            fetchLocation("ActivityTransition (activity = ${data.activityType}, transition = ${data.transitionType})")
        }
        ZLog.write("[LocationService] Activity Transition: " +
            "activityType: ${data.activityType} " +
            "transitionType: ${data.transitionType}\n\t" +
            "(extraData: ${data.extraData})")
        ZHttp.send(ZApi.KEY_ACTIVITY_TRANSITION, data)
        
    }
    private fun handleGpsData(location: Location): ZLocation {
        val loc = location.toData()
        val latLng = ALatLng(location.latitude, location.longitude)
        
        // Check if home
        var minDist: Double = Double.MAX_VALUE
        var isCloseToUserLoc = false
        userLocations.map {
            val dist = ZGps.distancePrecise(latLng, it)
            minDist = min(minDist, dist)
            if (dist <= thresholdDistanceHome) {
                isCloseToUserLoc = true
            }
        }
        if (isHome != isCloseToUserLoc) {
            isHome = isCloseToUserLoc
            val priority = gpsPriority
//            ZLog.write("[LocationService] setting gpsIntervalThreshold: $gpsIntervalThreshold ms gpsPriority: ${ZGps.priorityToString(priority)} (isHome=$isHome)")
            locationClient.setGpsAccuracy(priority)
            if (locationClient.isIntervalChanged(calcInterval())) {
                startLocationJob(forceRestart = true)
            }
        }
        distanceFromHome = minDist
        
        if (logGps) {
            ZLog.write("[LocationService] " +
                "timeSinceLastGps: ${floor(timeSinceLastGps/1000.0)} s, " +
                "distanceFromHome: ${floor(distanceFromHome)} m, " +
                "gpsPriority: ${ZGps.priorityToString(gpsPriority)}, " +
                "isHome: $isHome")
        }
        lastGpsUpdate = ZTime.msSince1970() // location.time
        prevLatLng = latLng
        return loc
    }
    
    @SuppressLint("MissingPermission")
    private fun fetchLocation(invoker: String, callback: ((loc: ZLocation?) -> Any)? = null) {
        if (!hasLocationPermission()) {
            ZLog.write("fetchLocation has no permission! :(")
            return
        }
        locationClient.getProvider().getCurrentLocation(gpsPriority, object : CancellationToken() {
            override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token
            override fun isCancellationRequested() = false
        }).addOnSuccessListener { location ->
            if (location == null) {
                ZLog.warn("Location is null! :(")
                return@addOnSuccessListener
            }
            ZLog.error("fetchLocation Invoked by $invoker")
            val loc = handleGpsData(location)
            ZHttp.send(KEY_LOCATION, loc)
            callback?.invoke(loc)
        }.addOnFailureListener {
            ZLog.error(it)
            callback?.invoke(null)
        }
    }
    
    @SuppressLint("MissingPermission")
    fun tryStopActivityRecognition() {
        if (activityPendingIntent == null) {
            return
        }
        try {
            val pIntent = activityPendingIntent!!
            if (!hasActivityRecognitionPermission()) {
                ZLog.write("[LocationService] Stopping Activity Recognition")
                pIntent.cancel()
                return
            }
            val task = ActivityRecognition.getClient(applicationContext)
                .removeActivityTransitionUpdates(pIntent)
            task.addOnSuccessListener {
                ZLog.write("[LocationService] Stopped Activity Recognition <TASK>")
                pIntent.cancel()
            }
            task.addOnFailureListener { e: Exception ->
                ZLog.write("[LocationService] Activity Recognition Exception: ${e.message}")
            }
        } catch (err: Exception) {
            ZLog.error(err)
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun stop() {
        ZLog.warn("[LocationService] stop()")
        tryStopActivityRecognition()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        ZLog.warn("[LocationService] onDestroy()")
        super.onDestroy()
        tryStopActivityRecognition()
//        locationJob?.cancel("[LocationService] onDestroy()")
//        batteryTicker.cancel("[LocationService] onDestroy()")
//        connectionTicker.cancel("[LocationService] onDestroy()")
        serviceScope.cancel()
        ZLog.info("[LocationService]", "Unregistering MainReceiver")
        unregisterReceiver(mainReceiver)
        ZLog.info("[LocationService]", "Unregistering ScreenUnlockReceiver")
        unregisterReceiver(screenReceiver)
        instance = null
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
        
        val latLngWeesp = ALatLng(52.3114443, 5.0226505)
        val latLngKtown = ALatLng(52.24263440, 5.11889450)
        
        const val ACTIVITY_TRANSITION_TIMEOUT = 1000L * 5L // 5 seconds
        const val TICKER_METADATA_TIMEOUT = 1000L * 60L * 10L // 10 minutes
        
        const val LOCATION_HOME_INTERVAL = 1000L * 60L * 5L // Every 5 minutes
        const val LOCATION_INTERVAL = 1000L * 30L // Every 30 seconds

        // Prod
//        const val LOCATION_CLIENT_INTERVAL = 1000L * 60L * 10L // Every 10 minutes
        
        const val TICKER_HOME_INTERVAL = 1000L * 60L * 5L // Every 5 minutes
        const val TICKER_STILL_INTERVAL = 1000L * 60L // Every 60 seconds
        const val TICKER_TRAVEL_INTERVAL = 1000L * 15L // Every 15 seconds
        
    }
}