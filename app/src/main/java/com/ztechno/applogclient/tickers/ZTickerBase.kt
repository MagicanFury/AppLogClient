package com.ztechno.applogclient.tickers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ztechno.applogclient.utils.launchPeriodicAsync
import com.ztechno.applogclient.utils.ZLog
import com.ztechno.applogclient.utils.ZTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlin.coroutines.cancellation.CancellationException

open class ZTickerBase(private val scope: CoroutineScope, protected var interval: Long, callback: ((prevTime: Long) -> Boolean)? = null) {
  
  private var func: ((prevTime: Long) -> Boolean)
  private var tickJob: Job? = null
  protected var prevTime: Long = 0
  
  var isActive by mutableStateOf(tickJob?.isActive ?: false)
    private set
  
//  val isActive get() = tickJob?.isActive ?: false
  val isCancelled get() = tickJob?.isCancelled ?: false
  
  val currInterval: Long get() = interval
  
  val resetPrevTimeOnCompletion = true
  
  var restartReason: String? = null
  
  init {
    prevTime = ZTime.msSince1970() - (interval + 1000L)
    func = callback ?: { tick(it) }
  }
  
  open fun start(forceRestart: Boolean) {
    if (forceRestart) {
      if (resetPrevTimeOnCompletion) {
        prevTime = 0
      }
      tickJob?.cancel("ZTickerBase cancelled by start(forceRestart=true) method")
    } else if (tickJob?.isActive == true) {
//      ZLog.warn("Can't start tickJob(${javaClass.simpleName}) because it's already running")
      return
    }
    try {
      val deferred = scope.launchPeriodicAsync(interval) {
        val now = ZTime.msSince1970()
        if (now - prevTime > interval) {
          val executed = func.invoke(prevTime)
          if (executed) {
            prevTime = now
          }
        }
      }
      tickJob = deferred.job
      onStarted()
    } catch (ce: CancellationException) {
      ZLog.warn("tickJob Cancelled")
    } catch (t: Throwable) {
      ZLog.error(t)
    }
    
    tickJob?.invokeOnCompletion {
      onCompletion(it)
    }
  }
  
  open fun onStarted() {
    ZLog.warn("[${this.javaClass.simpleName}] onStarted called!")
    isActive = tickJob?.isActive ?: false
  }
  
  open fun onCompletion(err: Throwable?) {
    ZLog.info("[${this.javaClass.simpleName}]", "onCompletion called! reason: $restartReason")
    restartReason = null
    isActive = tickJob?.isActive ?: false
    if (resetPrevTimeOnCompletion) {
      prevTime = 0
    }
    if (err != null) {
      if (err is CancellationException) {
        // Ignore err...
      } else {
        ZLog.error(err)
      }
    }
  }
  
  open fun tick(prevTime: Long): Boolean {
    throw Error("Not Implemented Error: ZTickerBase.tick!")
  }
  
  open fun cancel(reason: String?) {
    this.restartReason = reason
    isActive = tickJob?.isActive ?: false
    tickJob?.cancel(reason ?: "[${this.javaClass.simpleName}] cancel()")
  }
  
  fun restartWithInterval(interval: Long, reason: String?) {
//    if (tickJob?.isActive == true) {
//      tickJob?.cancel(reason ?: "[ZTickerBase] restartWithInterval($interval)")
//    }
    this.restartReason = reason
    this.interval = interval
    start(forceRestart = true)
  }
  
}