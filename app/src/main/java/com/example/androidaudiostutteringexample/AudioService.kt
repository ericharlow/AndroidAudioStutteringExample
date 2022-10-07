package com.example.androidaudiostutteringexample

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Debug
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.drm.DrmSession.DrmSessionException
import com.google.android.exoplayer2.drm.DrmSessionEventListener
import com.google.android.exoplayer2.drm.OfflineLicenseHelper
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.offline.DashDownloader
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*


class AudioService : MediaBrowserServiceCompat() {

  private lateinit var notificationManager: NotificationManager

  private lateinit var player: ExoPlayer

  private lateinit var cacheDataSourceFactory: DataSource.Factory

  private lateinit var renderersFactory: RenderersFactory

  private val analyticsListener = AudioAnalyticsListener()

  private var serviceForegrounded = false

  override fun onCreate() {
    super.onCreate()
    notificationManager =
      applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    renderersFactory = DefaultRenderersFactory(this).forceEnableMediaCodecAsynchronousQueueing()

    cacheDataSourceFactory = CacheDataSource.Factory()
      .setCache(application.asApp().downloadCache)
      .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory()) // DefaultDataSource.Factory(this)
      .setCacheWriteDataSinkFactory(null) // Disable writing.

    player = ExoPlayer.Builder(this, renderersFactory)
      .setWakeMode(C.WAKE_MODE_LOCAL)
      .setMediaSourceFactory(
        DefaultMediaSourceFactory(this).setDataSourceFactory(cacheDataSourceFactory))
      .setRenderersFactory(renderersFactory)
      .build()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!serviceForegrounded) internalStartForeground(createPlaceholderNotification())

//    downloadAndPlayContentSimpleApproach()
    downloadAndPlayContentMoreComplexApproach()
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

  private fun downloadAndPlayContentSimpleApproach() {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val app = application.asApp()
        val cache = app.downloadCache
        val cacheDataSourceFactory = CacheDataSource.Factory()
          .setCache(cache)
          .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
        // Create a downloader for the first representation of the first adaptation set of the first
        // period.
        val initialMediaItem = MediaItem.Builder()
          .setUri("https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd")
          .setStreamKeys(Collections.singletonList(StreamKey(0, 0, 0)))
          .build()
        val dashDownloader = DashDownloader(
          initialMediaItem,
          cacheDataSourceFactory)
        // Perform the download.
        dashDownloader.download { contentLength, bytesDownloaded, percentDownloaded ->
          if (percentDownloaded == 100.0F)
          Log.i("SBSS", "onStartCommand: download percent: $percentDownloaded")
        }

        val mediaSource: DashMediaSource =
          DashMediaSource.Factory(cacheDataSourceFactory).createMediaSource(initialMediaItem)

        CoroutineScope(Dispatchers.Main).launch {
          try {
            player.setMediaSource(mediaSource)

            player.playWhenReady = true
            player.playbackParameters = PlaybackParameters(1.75F, 1.0F)
            player.addAnalyticsListener(analyticsListener)
            player.prepare()
          } catch(t: Throwable) {
            Log.e("SBSS", "downloadAndPlayContent: ", t)
          }
        }
      } catch(t: Throwable) {
        Log.e("SBSS", "downloadAndPlayContent: ", t)
      }
    }
  }

  private fun downloadAndPlayContentMoreComplexApproach() {
//        Debug.waitForDebugger()
    val app = application.asApp()

    val downloadManager = DownloadManager(
      this,
      app.databaseProvider,
      app.downloadCache,
      cacheDataSourceFactory,
      Runnable::run)

    val initialMediaItem = buildInitialMediaItem()
    val dataSourceFactory = DefaultHttpDataSource.Factory()
    val offlineLicenseHelper = OfflineLicenseHelper.newWidevineInstance(
      initialMediaItem.localConfiguration?.drmConfiguration?.licenseUri.toString(),
      dataSourceFactory,
      DrmSessionEventListener.EventDispatcher())
    val downloadHelper: DownloadHelper = DownloadHelper.forMediaItem(
      this,
      initialMediaItem,
      DefaultRenderersFactory(this),
      dataSourceFactory)
    downloadHelper.prepare(object : DownloadHelper.Callback {
      override fun onPrepared(helper: DownloadHelper) {
        val format = getFirstFormatWithDrmInitData(helper)
        val keySetId = try {
          offlineLicenseHelper.downloadLicense(format!!)
        } catch (e: DrmSessionException) {
          Log.e("SBSS", "DownloadHelper.onPrepared: ", e)
          ByteArray(0)
        } finally {
          offlineLicenseHelper.release()
        }
        val data = "HD (cenc)".toByteArray(Charsets.UTF_8)
        val downloadRequest = helper.getDownloadRequest(data).copyWithKeySetId(keySetId)
        downloadManager.addDownload(downloadRequest)
        downloadManager.resumeDownloads()
        downloadManager.addListener(object : DownloadManager.Listener {
          override fun onDownloadChanged(
            downloadManager: DownloadManager, download: Download, finalException: Exception?) {
            Log.i("SBSS", "onDownloadChanged: ")
            if (download.request.id == downloadRequest.id && download.state == Download.STATE_COMPLETED) {
              val mediaItem = buildMediaItem(download.request)
              val progressiveMediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(mediaItem)
              player.setMediaSource(progressiveMediaSource)

              player.playWhenReady = true
              player.playbackParameters = PlaybackParameters(1.75F, 1.0F)
              player.addAnalyticsListener(analyticsListener)
              player.prepare()

//              try { downloadHelper.release() } catch(t: Throwable) {}
            }
          }
        })
      }

      override fun onPrepareError(helper: DownloadHelper, e: IOException) {
        Log.e("SBSS", "onPrepareError: ", e)
        helper.release()
      }
    })
  }

  private fun buildMediaItem(downloadRequest: DownloadRequest): MediaItem {
    val mediaItemBuilder = MediaItem.Builder()
      .setMediaId(downloadRequest.id)
      .setUri(downloadRequest.uri)
      .setMimeType(downloadRequest.mimeType)
      .setMediaMetadata(MediaMetadata.Builder().setTitle("HD (cenc)").build())
      .setDrmConfiguration(
        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
          .setLicenseUri("https://proxy.uat.widevine.com/proxy?video_id=2015_tears&provider=widevine_test")
          .setLicenseRequestHeaders(emptyMap())
          .setKeySetId(downloadRequest.keySetId)
          .build())
      .setStreamKeys(downloadRequest.streamKeys)
    return mediaItemBuilder.build()
  }

  private fun buildInitialMediaItem(): MediaItem {
    val contentUri = Uri.parse("https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd")
    val licenseUri = Uri.parse("https://proxy.uat.widevine.com/proxy?video_id=2015_tears&provider=widevine_test")
    val mediaItemBuilder = MediaItem.Builder()
      .setUri(contentUri)
      .setMimeType("application/dash+xml")
      .setMediaMetadata(MediaMetadata.Builder().setTitle("HD (cenc)").build())
      .setDrmConfiguration(
        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
          .setLicenseUri(licenseUri)
          .setLicenseRequestHeaders(emptyMap())
//          .setKeySetId(keySetId)
          .build()
      )
//      .setStreamKeys(streamKeys)
    return mediaItemBuilder.build()
  }

  // Internal methods.
  private fun getFirstFormatWithDrmInitData(helper: DownloadHelper): Format? {
    for (periodIndex in 0 until helper.periodCount) {
      val mappedTrackInfo = helper.getMappedTrackInfo(periodIndex)
      for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
        val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
        for (trackGroupIndex in 0 until trackGroups.length) {
          val trackGroup = trackGroups[trackGroupIndex]
          for (formatIndex in 0 until trackGroup.length) {
            val format = trackGroup.getFormat(formatIndex)
            if (format.drmInitData != null) {
              return format
            }
          }
        }
      }
    }
    return null
  }

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

private fun Application.asApp(): App = this as App