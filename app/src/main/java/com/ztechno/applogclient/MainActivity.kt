package com.ztechno.applogclient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.ztechno.applogclient.services.LocationService
import com.ztechno.applogclient.ui.theme.AppLogClientTheme
import com.ztechno.applogclient.utils.ZDevice
import com.ztechno.applogclient.utils.ZLog

class MainActivity : ComponentActivity() {
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            0
        )
        val btnSize = Modifier.size(width = 360.dp, height = 40.dp)
        setContent {
            AppLogClientTheme {
                val ctx = LocalContext.current
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(modifier = btnSize, onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_START
                            startService(this)
                        }
                    }) {
                        Text(text = "Start")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(modifier = btnSize, onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_STOP
                            startService(this)
                        }
                    }) {
                        Text(text = "Stop")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(modifier = btnSize,onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_MANUAL
                            startService(this)
                        }
                    }) {
                        Text(text = "Ping")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(modifier = btnSize, onClick = {
                        ZLog.write("Battery Perc: ${ZDevice.calcBatteryPercentage(applicationContext)}")
                    }) {
                        Text(text = "Battery Check")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(modifier = btnSize, onClick = {
                        val p = packageManager
                        p.setComponentEnabledSetting(
                            componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }) {
                        Text("Hide App")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(modifier = btnSize, onClick = {
                        val intent = Intent(ctx, SetupActivity::class.java)
                        ctx.startActivity(intent)
                    }) {
                        Text("Setup Device Id")
                    }
                }
            }
        }
    }
}