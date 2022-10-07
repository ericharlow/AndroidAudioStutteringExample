package com.example.androidaudiostutteringexample

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Debug
import android.support.v4.media.MediaBrowserCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.demo.DemoUtil
import com.google.android.exoplayer2.demo.IntentUtil

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
    if (!serviceForegrounded) internalStartForeground(createPlaceholderNotification())

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
    Debug.waitForDebugger()
    // download content
    val renderersFactory = DemoUtil.buildRenderersFactory(this, false)
    val downloadTracker = DemoUtil.getDownloadTracker(this)
    downloadTracker.toggleDownload(null, getInitialMediaItem(), renderersFactory)
    // TODO: wait for download to complete
    // prepare playback
//    val intent = getIntent()
//    val mediaItems = createMediaItems(intent)
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

  private fun getIntent(): Intent {
    val intent = Intent()
    val mediaItem = getInitialMediaItem()
    IntentUtil.addToIntent(listOf(mediaItem), intent)
    intent.putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, false)
    return intent
  }

//  private fun createMediaItems(intent: Intent): List<MediaItem> {
//    val action = intent.action
//    val actionIsListView = IntentUtil.ACTION_VIEW_LIST == action
//    if (!actionIsListView && IntentUtil.ACTION_VIEW != action) {
//      Log.i("SBSS", "createMediaItems: unexpected intent action")
//      return emptyList()
//    }
//    // Note: important to have download before here
//    val mediaItems: List<MediaItem> =
//      com.google.android.exoplayer2.demo.PlayerActivity.createMediaItems(
//        intent,
//        DemoUtil.getDownloadTracker( /* context= */this)
//      )
//    for (i in mediaItems.indices) {
//      val mediaItem = mediaItems[i]
//      if (!Util.checkCleartextTrafficPermitted(mediaItem)) {
//        Log.i("SBSS", "createMediaItems: clear text network traffic not permitted")
//        return emptyList()
//      }
//      if (Util.maybeRequestReadExternalStoragePermission( /* activity= */this, mediaItem)) {
//        // The player will be reinitialized if the permission is granted.
//        return emptyList()
//      }
//      val drmConfiguration = mediaItem.localConfiguration!!.drmConfiguration
//      if (drmConfiguration != null) {
//        if (Build.VERSION.SDK_INT < 18) {
//          Log.i("SBSS", "createMediaItems: drm unsupported")
//          return emptyList()
//        } else if (!FrameworkMediaDrm.isCryptoSchemeSupported(drmConfiguration.scheme)) {
//          Log.i("SBSS", "createMediaItems: drm scheme unsupported")
//          return emptyList()
//        }
//      }
//    }
//    return mediaItems
//  }

  private fun createPlaceholderNotification(): Notification =
    NotificationCompat.Builder(this, "playback_controls_notification_channel")
      .setSmallIcon(R.drawable.ic_baseline_library_music_24)
      .setContentText(getString(R.string.preparing_playback_message))
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