package com.example.bleTransfer

import android.app.Application
import app.eccweizhi.onscreenlog.OnScreenLog
import app.eccweizhi.onscreenlog.timber.OnScreenLoggingTree
import timber.log.Timber

class BleApplication: Application() {

  lateinit var onScreenLog:OnScreenLog
  private set

  override fun onCreate() {
    super.onCreate()
    onScreenLog = OnScreenLog.builder()
      .context(this)
      .notificationId(1001)
      .build()
    INSTANCE = this
    Timber.plant(OnScreenLoggingTree(onScreenLog))
  }

  companion object{
    lateinit var INSTANCE : BleApplication
  }
}