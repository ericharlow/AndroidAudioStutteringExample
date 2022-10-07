package com.example.androidaudiostutteringexample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.demo.DemoDownloadService
import com.google.android.exoplayer2.offline.DownloadService

class AudioPlayerActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_audio_player)
    startDownloadService()
    ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, AudioService::class.java))
  }

  /** Start the download service if it should be running but it's not currently.  */
  private fun startDownloadService() {
    // Starting the service in the foreground causes notification flicker if there is no scheduled
    // action. Starting it in the background throws an exception if the app is in the background too
    // (e.g. if device screen is locked).
    try {
      DownloadService.start(this, DemoDownloadService::class.java)
    } catch (e: IllegalStateException) {
      DownloadService.startForeground(this, DemoDownloadService::class.java)
    }
  }
}