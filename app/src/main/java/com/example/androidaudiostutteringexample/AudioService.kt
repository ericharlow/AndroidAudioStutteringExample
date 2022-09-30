package com.example.androidaudiostutteringexample

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

class AudioService : MediaBrowserServiceCompat() {

  private lateinit var notificationManager: NotificationManager

  private lateinit var player: ExoPlayer

  private val analyticsListener = AudioAnalyticsListener()

  private var serviceForegrounded = false

  override fun onCreate() {
    super.onCreate()
    notificationManager =
      applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    player = ExoPlayer.Builder(this)
      .setWakeMode(C.WAKE_MODE_LOCAL)
      .build()
    // TODO: impl notification
//    notificationManager.notify(R.id.audio_player_notification_id, notification)
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceForegrounded = false
    try { player.removeAnalyticsListener(analyticsListener) } catch(t: Throwable) {}
    try { player.pause() } catch(t: Throwable) {}
    try { player.release() } catch(t: Throwable) {}
    notificationManager.cancel(R.id.audio_player_notification_id)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!serviceForegrounded) internalStartForeground(createPlaceholderNotification())

//    val mediaItem = MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/audio-141.mp4")

//    val mediaItem = MediaItem.fromUri("file:///android_asset/audio-141.mp4")
//    player.setMediaItem(mediaItem)

    val mediaSource: MediaSource = DashMediaSource.Factory(DefaultHttpDataSource.Factory())
      .createMediaSource(MediaItem.fromUri("https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd"))
    player.setMediaSource(mediaSource)

    player.playbackParameters = PlaybackParameters(1.75F, 1.0F)
    player.addAnalyticsListener(analyticsListener)
    player.prepare()
    player.play()
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onGetRoot(
    clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
    return null
  }

  override fun onLoadChildren(
    parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {

  }

  // Private helper functions

  private fun createPlaceholderNotification(): Notification =
    NotificationCompat.Builder(this, "playback_controls_notification_channel")
      .setSmallIcon(R.drawable.ic_baseline_library_music_24)
      .setContentText(getString(R.string.starting_playback_message))
      .build()

  private fun internalStartForeground(notification: Notification) {
    try {
      startForeground(R.id.audio_player_notification_id, notification)
      serviceForegrounded = true
    } catch (t: Throwable) {
      terminate()
    }
  }

  private fun terminate() {
    try { player.pause() } catch(t: Throwable) {}
    serviceForegrounded = false
    stopForeground(false)
    stopSelf()
  }

}