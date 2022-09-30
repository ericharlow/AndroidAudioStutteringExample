package com.example.androidaudiostutteringexample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat

class AudioPlayerActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_audio_player)
    ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, AudioService::class.java))
  }
}