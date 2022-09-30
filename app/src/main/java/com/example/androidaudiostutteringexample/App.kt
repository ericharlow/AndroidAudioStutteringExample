package com.example.androidaudiostutteringexample

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App: Application() {
  override fun onCreate() {
    super.onCreate()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

      val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

      // Create playback controls notification channel
      val playbackControlsNotificationChannel = NotificationChannel(
        "playback_controls_notification_channel",
        getString(R.string.playback_controls_notification_channel_name),
        NotificationManager.IMPORTANCE_LOW
      )
      playbackControlsNotificationChannel.description =
        getString(R.string.playback_controls_notification_channel_description)
      notificationManager.createNotificationChannel(playbackControlsNotificationChannel)
    }
  }
}