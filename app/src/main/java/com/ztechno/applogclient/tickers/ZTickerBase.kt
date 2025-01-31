package com.ztechno.applogclient.tickers

import android.os.Build
import androidx.annotation.RequiresApi
import com.ztechno.applogclient.utils.launchPeriodicAsync
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job

@RequiresApi(Build.VERSION_CODES.O)
open class ZTickerBase(private val scope: CoroutineScope, private val interval: Long, callback: ((prevTime: Long) -> Boolean)? = null) {
  
  private var func: ((prevTime: Long) -> Boolean)
  private var tickJob: Job? = null
  private var prevTime: Long = 0
  
  init {
    prevTime = ZTime.msSince1970() - (interval + 1L)
    func = callback ?: { tick(it) }
  }
  
  fun start(forceRestart: Boolean) {
    if (forceRestart) {
      tickJob?.cancel()
    } else if (tickJob?.isActive == true) {
      ZLog.warn("Can't start tickJob(${javaClass.simpleName}) because it's already running")
      return
    }
    tickJob = scope.launchPeriodicAsync(interval) {
      val now = ZTime.msSince1970()
      if (now - prevTime > interval) {
        val executed = func.invoke(prevTime)
        if (executed) {
          prevTime = now
        }
      }
    }.job
  }
  
  open fun tick(prevTime: Long): Boolean {
    throw Error("Not Implemented Error: ZTickerBase.tick!")
  }
  
  fun cancel(reason: String?) {
    tickJob?.cancel(reason ?: "[ZTickerBase] cancel()")
  }
  
}