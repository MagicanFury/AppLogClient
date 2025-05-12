package com.ztechno.applogclient

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.ui.theme.AppLogClientTheme
import com.ztechno.applogclient.utils.ZDevice
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.hasActivityRecognitionPermission
import com.ztechno.applogclient.utils.hasLocationPermission

class MainActivity(var viewModel: MainViewModel = MainViewModel()) : ComponentActivity() {
    
    private lateinit var locationService: LocationService
    private var bound: Boolean = false
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService()
            bound = true
            viewModel.loadValue(locationService)
            
            ZLog.write("LocationService BOUND!")
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            ZLog.warn("LocationService UNBOUND!!")
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        if (!bound) {
            Intent(this, LocationService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
        
        if (!this.hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }
        if (!this.hasActivityRecognitionPermission()) {
            ZLog.error("[MainActivity] No Activity Recognition Permission!")
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 0)
        }
        if (!Settings.canDrawOverlays(applicationContext)) {
            ZLog.error("[MainActivity] No Overlay Permission!")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val cardShape = RoundedCornerShape(size = 8.dp)
        val btnShape = RoundedCornerShape(size = 32.dp)
        setContent {
            AppLogClientTheme {
//                val ctx = LocalContext.current
                
                val btnSize = Modifier
                    .size(width = 360.dp, height = 48.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colors.primary,
                        shape = btnShape,
                    )
                    .clip(btnShape)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .width(360.dp)
                            .clip(cardShape),
                        elevation = 2.dp
                    ) {
                        val rowMod = Modifier.fillMaxWidth()
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(rowMod, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Service Enabled")
                                Text("${viewModel.serviceEnabled}")
                            }
                            Row(rowMod, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("State")
                                Text(viewModel.currentState)
                            }
                            Row(rowMod, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Hom")
                                Text("${viewModel.hom}")
                            }
                            Row(rowMod, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Mov")
                                Text("${viewModel.mov}")
                            }
                            Row(rowMod, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Int")
                                Text("${viewModel.int}")
                            }
//                        Text(viewModel.mutableValue, modifier = Modifier.padding(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(modifier = btnSize, onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_START
                            startForegroundService(this)
                        }
                    }) {
                        Text(text = "Start")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(modifier = btnSize, onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_STOP
                            startForegroundService(this)
                        }
                    }) {
                        Text(text = "Stop")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(modifier = btnSize, onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_RESTART
                            startForegroundService(this)
                        }
                    }) {
                        Text(text = "Restart")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(modifier = btnSize, onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_MANUAL
                            startForegroundService(this)
                        }
                    }) {
                        Text(text = "Ping")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
//                    Button(modifier = btnSize,onClick = {
//                        Intent(applicationContext, LocationService::class.java).apply {
//                            action = LocationService.ACTION_SET_HOME_WEESP
//                            startForegroundService(this)
//                        }
//                    }) {
//                        Text(text = "Set Home to WP")
//                    }
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Button(modifier = btnSize, onClick = {
//                        Intent(applicationContext, LocationService::class.java).apply {
//                            action = LocationService.ACTION_SET_HOME_KTOWN
//                            startForegroundService(this)
//                        }
//                    }) {
//                        Text(text = "Set Home to KT")
//                    }
//                    Spacer(modifier = Modifier.height(8.dp))
                    Button(modifier = btnSize, onClick = {
                        ZLog.write("Battery Perc: ${ZDevice.calcBatteryPercentage(applicationContext)}")
                    }) {
                        Text(text = "Battery Check")
                    }
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Button(modifier = btnSize, onClick = {
//                        val p = packageManager
//                        p.setComponentEnabledSetting(
//                            componentName,
//                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//                            PackageManager.DONT_KILL_APP
//                        )
//                    }) {
//                        Text("Hide App")
//                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(modifier = btnSize, onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_SETUP_DEVICE
                            startForegroundService(this)
                        }
                    }) {
                        Text("Setup Device Id")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        modifier = btnSize,
//                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                        onClick = {
                            try {
                                ZLog.clearLogHistory()
                                locationService.clearPacketHistory()
                            } catch (e: Throwable){
                                ZLog.error(e)
                            }
                        }) {
                        Text("Clear All Logs")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(modifier = btnSize, onClick = {
                        val intent2 = Intent(applicationContext, HistoryActivity::class.java)
                        startActivity(intent2)
                    }) {
                        Text("Show History")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(modifier = btnSize, onClick = {
                        startActivity(Intent(applicationContext, LogActivity::class.java))
                    }) {
                        Text("Show Log")
                    }
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
//        viewModel.
        unbindService(connection)
        bound = false
    }
}