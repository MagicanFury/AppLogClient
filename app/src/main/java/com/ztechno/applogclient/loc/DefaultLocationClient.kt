package com.ztechno.applogclient.loc

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.ztechno.applogclient.utils.ALatLng
import com.ztechno.applogclient.utils.ZGps
import com.ztechno.applogclient.utils.hasLocationPermission
import com.ztechno.applogclient.utils.ZLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.math.min

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient,
    private var gpsPriority: Int
): LocationClient {
    
    private var distFromHome: Double = Double.MAX_VALUE
    
    override fun getClosestUserLocDistance(): Double {
        return distFromHome
    }
    
    override fun isCloseToUserLocations(currLatLng: ALatLng, userLocations: List<ALatLng>, thresholdDist: Double): Boolean {
        var minDist: Double = Double.MAX_VALUE
        var isCloseToUserLoc = false
        userLocations.map {
            val dist = ZGps.distancePrecise(currLatLng, it)
            minDist = min(minDist, dist)
            if (dist <= thresholdDist) {
                isCloseToUserLoc = true
            }
        }
        return isCloseToUserLoc
    }

    override fun getProvider(): FusedLocationProviderClient {
        return client
    }
    
    override fun setGpsAccuracy(priority: Int) {
        gpsPriority = priority
    }
    
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long, onClose: () -> Unit): Flow<Location> {
        ZLog.write("[DefaultLocationClient] getLocationUpdates(interval=${"%.2f".format((interval / 1000f))}s)")
        return callbackFlow {
            if(!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing location permission")
            }
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!isGpsEnabled && !isNetworkEnabled) {
//                throw LocationClient.LocationException("GPS is disabled")
                ZLog.error("GPS is disabled")
            }

            val request = LocationRequest.create()
                .setPriority(gpsPriority)
                .setInterval(interval)
                .setFastestInterval(interval)

            val locationCallback = object : LocationCallback() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    if (gpsPriority != request.priority) {
                        request.priority = gpsPriority
                    }
                    result.locations.lastOrNull()?.let { location ->
                        launch { send(location) }
                    }
                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
            awaitClose {
                ZLog.write("Calling client.removeLocationUpdates(locationCallback)")
                client.removeLocationUpdates(locationCallback).addOnCompleteListener {
                    onClose.invoke()
                }
            }
        }
    }
}