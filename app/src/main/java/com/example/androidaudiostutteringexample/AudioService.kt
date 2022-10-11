package com.example.androidaudiostutteringexample

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.demo.DemoUtil
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.source.dash.DashMediaSource

class AudioService : MediaBrowserServiceCompat() {

  private lateinit var notificationManager: NotificationManager

  private lateinit var player: ExoPlayer

  private val analyticsListener = AudioAnalyticsListener()

  private var serviceForegrounded = false

  override fun onCreate() {
    super.onCreate()
    notificationManager =
      applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!serviceForegrounded) internalStartForeground(createPreparingNotification())

    downloadAndPlayContent()
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onGetRoot(
    clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
    return null
  }

  override fun onLoadChildren(
    parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {

  }

  override fun onDestroy() {
    super.onDestroy()
    serviceForegrounded = false
    try { player.removeAnalyticsListener(analyticsListener) } catch(t: Throwable) {}
    try { player.pause() } catch(t: Throwable) {}
    try { player.release() } catch(t: Throwable) {}
    notificationManager.cancel(R.id.audio_player_notification_id)
  }

  // Private helper functions

  private fun downloadAndPlayContent() {
    // download content
    val dataSourceFactory = DemoUtil.getDataSourceFactory(this)
    val renderersFactory = DemoUtil.buildRenderersFactory(this, false)
    val downloadTracker = DemoUtil.getDownloadTracker(this)
    val initialMediaItem = getInitialMediaItem()
    val deleted = downloadTracker.toggleDownload(null, initialMediaItem, renderersFactory)
    if (deleted) { notificationManager.notify(R.id.audio_player_notification_id, createRestartNotification()) }
    val downloadManager = DemoUtil.getDownloadManager(this)
    downloadManager.addListener(object : DownloadManager.Listener {
      override fun onDownloadChanged(
        downloadManager: DownloadManager, download: Download, finalException: Exception?) {
        if (download.request.id == initialMediaItem.localConfiguration?.uri.toString() && download.state == Download.STATE_COMPLETED) {
          // prepare playback
          val mediaItem = buildMediaItem(download.request, initialMediaItem)
          val mediaSource: DashMediaSource =
            DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

          player = ExoPlayer.Builder(this@AudioService)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
          player.setMediaSource(mediaSource)
          player.playWhenReady = true
          player.playbackParameters = PlaybackParameters(1.75F, 1.0F)
          player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
          player.addAnalyticsListener(analyticsListener)
          player.prepare()
          player.play()
          notificationManager.notify(R.id.audio_player_notification_id, createStartingNotification())
        }
      }
    })
  }

  private fun getInitialMediaItem(): MediaItem {
    return MediaItem.Builder()
      .setUri("https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd")
      .setMimeType("application/dash+xml")
      .setMediaMetadata(MediaMetadata.Builder().setTitle("HD (cenc)").build())
      .setDrmConfiguration(
        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
          .setLicenseUri("https://proxy.uat.widevine.com/proxy?video_id=2015_tears&provider=widevine_test")
          .setForceDefaultLicenseUri(false)
          .setMultiSession(false)
          .setPlayClearContentWithoutKey(false)
          .build()
      ).build()
  }

  private fun buildMediaItem(downloadRequest: DownloadRequest, initialMediaItem: MediaItem): MediaItem {
    val mediaItemBuilder = MediaItem.Builder()
      .setMediaId(downloadRequest.id)
      .setUri(downloadRequest.uri)
      .setMimeType(downloadRequest.mimeType)
      .setMediaMetadata(MediaMetadata.Builder().setTitle(initialMediaItem.mediaMetadata.title).build())
      .setDrmConfiguration(
        MediaItem.DrmConfiguration.Builder(initialMediaItem.localConfiguration?.drmConfiguration?.scheme ?: C.WIDEVINE_UUID)
          .setLicenseUri(initialMediaItem.localConfiguration?.drmConfiguration?.licenseUri)
          .setLicenseRequestHeaders(emptyMap())
          .setKeySetId(downloadRequest.keySetId)
          .build())
      .setStreamKeys(downloadRequest.streamKeys)
    return mediaItemBuilder.build()
  }

  private fun createPreparingNotification(): Notification =
    NotificationCompat.Builder(this, "playback_controls_notification_channel")
      .setSmallIcon(R.drawable.ic_baseline_library_music_24)
      .setContentText(getString(R.string.preparing_playback_message))
      .build()

  private fun createStartingNotification(): Notification =
    NotificationCompat.Builder(this, "playback_controls_notification_channel")
      .setSmallIcon(R.drawable.ic_baseline_library_music_24)
      .setContentText(getString(R.string.starting_playback_message))
      .build()

  private fun createRestartNotification(): Notification =
    NotificationCompat.Builder(this, "playback_controls_notification_channel")
      .setSmallIcon(R.drawable.ic_baseline_library_music_24)
      .setContentText(getString(R.string.restart_playback_message))
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