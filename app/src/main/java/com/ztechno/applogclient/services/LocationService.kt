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
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.ztechno.applogclient.R
import com.ztechno.applogclient.SetupActivity
import com.ztechno.applogclient.ZApi
import com.ztechno.applogclient.ZApi.KEY_BATTERY
import com.ztechno.applogclient.ZApi.KEY_CONNECTION
import com.ztechno.applogclient.ZApi.KEY_LOCATION
import com.ztechno.applogclient.ZApi.ZActivityTransition
import com.ztechno.applogclient.ZApi.ZLocation
import com.ztechno.applogclient.ZApi.ZUserLocation
import com.ztechno.applogclient.hasActivityRecognitionPermission
import com.ztechno.applogclient.hasLocationPermission
import com.ztechno.applogclient.launchPeriodicAsync
import com.ztechno.applogclient.receivers.MainReceiver
import com.ztechno.applogclient.receivers.ScreenUnlockReceiver
import com.ztechno.applogclient.utils.ALatLng
import com.ztechno.applogclient.utils.ActivityTransitionUtil
import com.ztechno.applogclient.utils.DefaultLocationClient
import com.ztechno.applogclient.utils.LocationClient
import com.ztechno.applogclient.utils.ZDevice.genBatteryData
import com.ztechno.applogclient.utils.ZDevice.genConnectionData
import com.ztechno.applogclient.utils.ZGps
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
import kotlin.math.floor
import kotlin.math.min


@RequiresApi(Build.VERSION_CODES.O)
class LocationService: Service() {
    
    private val logGps = false
    
    private var mainReceiver: BroadcastReceiver = MainReceiver(this)
    private var screenReceiver: BroadcastReceiver = ScreenUnlockReceiver()
    
//    private var activityTransitionReceiver: BroadcastReceiver = createActivityReceiver()
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    
    private var serviceJob: Job? = null
    private var tickJob: Job? = null
    private var activityPendingIntent: PendingIntent? = null
    
    private var thresholdDistanceHome: Double = 50.0
    
    public var walkingState: String? = null
    private var isHome: Boolean = false
    
    private var userLocations: List<ALatLng> = mutableListOf(latLngKtown)
    private var distanceFromHome: Double = 0.0
    private var lastGpsUpdate: Long = 0
    private var prevLatLng: ALatLng? = null
    
    private var isTravelling: Boolean = true
    
    private val gpsPriority: Int
        get() = Priority.PRIORITY_HIGH_ACCURACY
//        get() = if (isHome && !isTravelling) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_HIGH_ACCURACY
    
    private val gpsIntervalThreshold: Long
        get() = if (isHome && !isTravelling) LOCATION_HOME_INTERVAL else LOCATION_INTERVAL
    
    private val timeSinceLastGps: Long
        get() = ZTime.msSince1970() - lastGpsUpdate // location.time
    
    var notifBuilder = NotificationCompat.Builder(this, "location")
        .setContentTitle("...")
        .setContentText("...")
        .setSmallIcon(R.drawable.ic_launcher_background)
        .setOngoing(true)

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        ZLog.write("[LocationService] LocationService onCreate()")
        ZLog.write("[LocationService] Service Created gpsPriority=${ZGps.priorityToString(gpsPriority)}, isHome=$isHome")
        
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext),
            gpsPriority,
        )
//            initActivityTransitionReceiver()
        registerReceiver(mainReceiver, MainReceiver.filters())
        registerReceiver(screenReceiver, ScreenUnlockReceiver.filters())
        
        userLocations = ZApi.fetchUserLocations() ?: userLocations
        // TODO: Uncomment
//        if (startService(Intent(this, ForegroundEnablingService::class.java)) == null)
//            throw RuntimeException("Couldn't find " + ForegroundEnablingService::class.java.simpleName)
    }
    
    
//    private fun createActivityReceiver(): BroadcastReceiver {
//        return object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                val str = intent.getStringExtra("data")
//                val data = Gson().fromJson(str, ZActivityTransition::class.java)
//                handleActivityTransition(data)
//            }
//        }
//    }
    
//    @SuppressLint("UnspecifiedRegisterReceiverFlag")
//    private fun initActivityTransitionReceiver() {
//        val filter = IntentFilter()
//        filter.addAction(ACTION_PROCESS_ACTIVITY)
//        registerReceiver(activityTransitionReceiver, filter)
//    }
    

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
            
            ACTION_SETUP_DEVICE -> {
                fetchLocation("DEVICE_SETUP") {
                    val intent2 = Intent(applicationContext, SetupActivity::class.java)
                    intent2.setFlags(FLAG_ACTIVITY_NEW_TASK)
                    if (it != null) intent2.putExtra("location", Gson().toJson(it))
                    applicationContext.startActivity(intent2)
                }
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
            else -> ZLog.warn("[LocationService] onStartCommand: $intent ${ZLog.extrasToString(intent?.extras)}")
        }
        return super.onStartCommand(intent, flags, startId)
    }
    
    private fun start(forceRestart: Boolean = false) {
        ZLog.warn("[LocationService] start()")
        startActivityRecognition()
        startTickJob(forceRestart)
        startServiceJob(forceRestart)
//        startForeground(NOTIFICATION_ID, notification.build())
    }
    
    private fun startTickJob(forceRestart: Boolean) {
        val interval = if (isTravelling) TICKER_TRAVEL_INTERVAL else TICKER_HOME_INTERVAL
        if (forceRestart) {
            tickJob?.cancel()
        } else if (tickJob?.isActive == true) {
            ZLog.warn("Can't start tickJob because it's already running")
            return
        }
        var lastBatteryTime = 0L
        var lastConnectionTime = 0L
        var prevBatteryData: ZApi.ZBattery? = null
        var prevConnectionData: ZApi.ZConnection? = null
        ZLog.write("tickJob interval = $interval isTraveling = $isTravelling")
        tickJob = serviceScope.launchPeriodicAsync(interval) {
            val now = ZTime.msSince1970()
            if (now - lastBatteryTime > TICKER_METADATA_TIMEOUT) {
                val battery = genBatteryData(applicationContext)
                if (battery.battery != prevBatteryData?.battery) {
                    ZHttp.send(KEY_BATTERY, battery)
                    prevBatteryData = battery
                    lastBatteryTime = now
                }
            }
            if (now - lastConnectionTime > TICKER_METADATA_TIMEOUT) {
                val connection = genConnectionData(applicationContext, null)
                if (connection.dataEnabled != prevConnectionData?.dataEnabled ||
                    connection.wifiEnabled != prevConnectionData?.wifiEnabled) {
                    ZHttp.send(KEY_CONNECTION, connection)
                    prevConnectionData = connection
                    lastConnectionTime = now
                }
            }
            
            if (timeSinceLastGps > gpsIntervalThreshold) {
                fetchLocation("tickJob (tickInterval = $interval, gpsInterval = $gpsIntervalThreshold)")
            }
        }.job
    }
    
    private fun startServiceJob(forceRestart: Boolean) {
        if (serviceJob?.isActive == true) {
            ZLog.warn("Can't start location-updates service because it's already running")
        } else {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            serviceJob = locationClient
                .getLocationUpdates(LOCATION_CLIENT_INTERVAL)
                .catch { e -> ZLog.error(e) }
                .onEach { location ->
                    val loc = handleGpsData(location)
                    val updatedNotification = notifBuilder.setContentTitle(
                        location.latitude.toString().takeLast(3) + ", " +
                            location.longitude.toString().takeLast(3)
                    )
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
                    if (logGps) {
                        ZLog.warn("getLocationUpdates Invoked by serviceJob (const interval = $LOCATION_CLIENT_INTERVAL)")
                    }
                    ZHttp.send(KEY_LOCATION, loc)
                }
                .launchIn(serviceScope)
        }
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
        val intent = Intent(ACTION_PROCESS_ACTIVITY_TRANSITIONS)
        val pIntent = PendingIntent.getBroadcast(context, NOTIFICATION_ID, intent, FLAG_MUTABLE or FLAG_UPDATE_CURRENT)
        
        val request = ActivityTransitionRequest(ActivityTransitionUtil.getTransitions())
        val task = client.requestActivityTransitionUpdates(request, pIntent)
//        val task = client.requestActivityUpdates(5000L, pIntent)
        task.addOnSuccessListener {
            ZLog.write("[LocationService] Activity Recognition Setup Completed!")
//            context.registerReceiver(broadcastReceiver, IntentFilter(TRANSITIONS_RECEIVER_ACTION))
        }.addOnFailureListener { ZLog.error("[LocationService] ActivityTransition Error: ${it.stackTraceToString()}") }
        activityPendingIntent = pIntent
    }
    
    private var lastActivityTransitionTime: Long = 0
    fun handleActivityTransition(event: ActivityTransitionEvent, data: ZActivityTransition) {
//        val now = ZTime.msSince1970()
//        if ((now - lastActivityTransitionTime) < ACTIVITY_TRANSITION_TIMEOUT) {
//            return
//        }
        lastActivityTransitionTime = ZTime.msSince1970()
        
        val isTravelling = ActivityTransitionUtil.isTravelling(event)
        if (this.isTravelling != isTravelling) {
            this.isTravelling = true
            startTickJob(forceRestart = true)
            ZLog.write("isTravelling changed to $isTravelling, restarting tickJob")
        }
        val connection = genConnectionData(applicationContext, null)
        if (connection.wifiEnabled == false && connection.dataEnabled == true) {
            fetchLocation("ActivityTransition (activity = ${data.activityType}, transition = ${data.transitionType})")
        }
        ZLog.write("[LocationService] Activity Transition: " +
            "activityType: ${data.activityType} " +
            "transitionType: ${data.transitionType}\n\t" +
            "(extraData: ${data.extraData})")
        ZHttp.send(ZApi.KEY_ACTIVITY_TRANSITION, data)
        
    }
    private fun handleGpsData(location: Location): ZLocation {
        val loc = ZLocation(location.latitude.toString(), location.longitude.toString(), ZTime.format(location.time), location.accuracy)
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
            ZLog.write("[LocationService] setting gpsIntervalThreshold: $gpsIntervalThreshold ms gpsPriority: ${ZGps.priorityToString(priority)} (isHome=$isHome)")
            locationClient.setGpsAccuracy(priority)
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
    fun updateNotification() {
    
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
                ZLog.write("[LocationService] Stopping Activity Recognition <TASK>")
                pIntent.cancel()
            }
            task.addOnFailureListener { e: Exception ->
                ZLog.write("MYCOMPONENT ${e.message}")
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
        serviceJob?.cancel("[LocationService] onDestroy()")
        tickJob?.cancel("[LocationService] onDestroy()")
        serviceScope.cancel()
        unregisterReceiver(mainReceiver)
        unregisterReceiver(screenReceiver)
//        unregisterReceiver(activityTransitionReceiver)
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
        
        const val ACTION_PROCESS_ACTIVITY_TRANSITIONS = "com.ztechno.applogclient.ACTION_PROCESS_ACTIVITY_TRANSITIONS"
        const val ACTION_PROCESS_ACTIVITY = "ACTION_PROCESS_ACTIVITY"
        
        val latLngWeesp = ALatLng(52.3114443, 5.0226505)
        val latLngKtown = ALatLng(52.24263440, 5.11889450)
        
        const val ACTIVITY_TRANSITION_TIMEOUT = 1000L * 5L // 5 seconds
        const val TICKER_METADATA_TIMEOUT = 1000L * 60L * 10L // 10 minutes
        
        const val LOCATION_HOME_INTERVAL = 1000L * 60L * 5L // Every 5 minutes
        const val LOCATION_INTERVAL = 1000L * 30L // Every 30 seconds

        // Prod
        const val LOCATION_CLIENT_INTERVAL = 1000L * 60L * 10L // Every 10 minutes
        
        const val TICKER_HOME_INTERVAL = 1000L * 60L * 5L // Every 5 minutes
        const val TICKER_TRAVEL_INTERVAL = 1000L * 15L // Every 15 seconds
        
    }
}