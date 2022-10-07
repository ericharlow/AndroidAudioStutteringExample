package com.example.androidaudiostutteringexample

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

class App: Application() {

  val databaseProvider: StandaloneDatabaseProvider by lazy {
    StandaloneDatabaseProvider(this)
  }

  val downloadCache: SimpleCache by lazy {
    val cacheDir = File(this.cacheDir, "exoplayer")
    if (!cacheDir.exists()) {
      try {
        val result = cacheDir.mkdirs()
        if (!result) throw Exception("cache dir not created")
        else Log.i("SBSS", "cache dir: ${cacheDir.path}")
      } catch(t: Throwable) {
        Log.e("SBSS", "simple cache: ", t)
      }
    }
    SimpleCache(
      cacheDir,
      NoOpCacheEvictor(),
      databaseProvider)
  }

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