package com.example.androidaudiostutteringexample

import android.util.Log
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import java.io.IOException
import java.lang.Exception

class AudioAnalyticsListener : AnalyticsListener {

  override fun onDrmSessionManagerError(eventTime: AnalyticsListener.EventTime, error: Exception) {
    Log.e("SBSS", "onDrmSessionManagerError: ", error)
  }

  override fun onDrmKeysRemoved(eventTime: AnalyticsListener.EventTime) {
    Log.i("SBSS", "onDrmKeysRemoved: ")
  }

  override fun onDrmKeysRestored(eventTime: AnalyticsListener.EventTime) {
    Log.i("SBSS", "onDrmKeysRestored: ")
  }

  override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime) {
    Log.i("SBSS", "onDrmKeysLoaded: ")
  }

  override fun onLoadStarted(
    eventTime: AnalyticsListener.EventTime,
    loadEventInfo: LoadEventInfo,
    mediaLoadData: MediaLoadData
  ) {
    Log.i("SBSS", "onLoadStarted: ")
  }
  override fun onLoadError(
    eventTime: AnalyticsListener.EventTime,
    loadEventInfo: LoadEventInfo,
    mediaLoadData: MediaLoadData,
    error: IOException,
    wasCanceled: Boolean) {
    Log.e("SBSS", "onLoadError: ", error)
  }

  override fun onLoadCompleted(
    eventTime: AnalyticsListener.EventTime,
    loadEventInfo: LoadEventInfo,
    mediaLoadData: MediaLoadData
  ) {
    Log.i("SBSS", "onLoadCompleted: ")
  }

  override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
    Log.e("SBSS", "onPlayerError: ", error)
  }

  override fun onAudioDecoderInitialized(
    eventTime: AnalyticsListener.EventTime,
    decoderName: String,
    initializedTimestampMs: Long,
    initializationDurationMs: Long
  ) {
    Log.i("SBSS", "onAudioDecoderInitialized: ")
  }

  override fun onAudioCodecError(
    eventTime: AnalyticsListener.EventTime,
    audioCodecError: Exception
  ) {
    Log.e("SBSS", "onAudioCodecError: ", audioCodecError)
  }

  override fun onAudioUnderrun(
    eventTime: AnalyticsListener.EventTime,
    bufferSize: Int,
    bufferSizeMs: Long,
    elapsedSinceLastFeedMs: Long) {
    Log.i("SBSS", "onAudioUnderrun: time: ${eventTime.eventPlaybackPositionMs} buffer: $bufferSize bufferMs: $bufferSizeMs elapsed: $elapsedSinceLastFeedMs")
  }
}