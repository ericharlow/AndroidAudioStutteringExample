package com.example.androidaudiostutteringexample

import android.util.Log
import com.google.android.exoplayer2.analytics.AnalyticsListener

class AudioAnalyticsListener : AnalyticsListener {

  override fun onAudioUnderrun(
    eventTime: AnalyticsListener.EventTime,
    bufferSize: Int,
    bufferSizeMs: Long,
    elapsedSinceLastFeedMs: Long) {
    Log.i("exoplayer ", "onAudioUnderrun: time: ${eventTime.eventPlaybackPositionMs} buffer: $bufferSize bufferMs: $bufferSizeMs elapsed: $elapsedSinceLastFeedMs")
  }
}