package com.rtvvoice.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.rtvvoice.app.R
import com.rtvvoice.app.tts.ArticleReader
import com.rtvvoice.app.tts.ReaderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the voice reader playing after the article
 * screen is gone, and exposes playback as a media notification (lock screen
 * and notification controls).
 */
class ReaderService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()

        val session = MediaSessionCompat(this, "RTVoiceReader")
        session.isActive = true
        session.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { ReaderManager.resume() }
            override fun onPause() { ReaderManager.pause() }
            override fun onSkipToNext() { ReaderManager.nextParagraph() }
            override fun onSkipToPrevious() { ReaderManager.previousParagraph() }
            override fun onStop() { ReaderManager.stop() }
        })
        mediaSession = session

        scope.launch {
            ReaderManager.status.collect { updateForeground(it) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> ReaderManager.toggle()
            ACTION_NEXT -> ReaderManager.nextParagraph()
            ACTION_PREV -> ReaderManager.previousParagraph()
            ACTION_STOP -> ReaderManager.stop()
        }
        // startForeground must happen promptly even if the reader already went idle.
        updateForeground(ReaderManager.status.value)
        return START_NOT_STICKY
    }

    private fun updateForeground(status: ReaderManager.Status) {
        val session = mediaSession ?: return
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
                )
                .setState(
                    when (status.state) {
                        ArticleReader.State.PLAYING -> PlaybackStateCompat.STATE_PLAYING
                        ArticleReader.State.PAUSED -> PlaybackStateCompat.STATE_PAUSED
                        ArticleReader.State.IDLE -> PlaybackStateCompat.STATE_STOPPED
                    },
                    0L, 0f
                )
                .build()
        )
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, status.title)
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    getString(R.string.app_name),
                )
                .build()
        )

        if (!status.isActive) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val notification = buildNotification(status, session)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(status: ReaderManager.Status, session: MediaSessionCompat): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(Intent.ACTION_MAIN).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val playing = status.state == ArticleReader.State.PLAYING

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reader_mic)
            .setContentTitle(status.title)
            .setContentText(
                getString(
                    R.string.reader_notification_text,
                    status.paragraphIndex + 1, status.paragraphCount,
                )
            )
            .setContentIntent(contentIntent)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_prev, getString(R.string.reader_prev), actionIntent(ACTION_PREV, 1))
            .addAction(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                getString(if (playing) R.string.reader_pause else R.string.reader_play),
                actionIntent(ACTION_TOGGLE, 2),
            )
            .addAction(R.drawable.ic_next, getString(R.string.reader_next), actionIntent(ACTION_NEXT, 3))
            .addAction(R.drawable.ic_stop, getString(R.string.reader_stop), actionIntent(ACTION_STOP, 4))
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, ReaderService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.reader_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.reader_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val CHANNEL_ID = "voice_reader"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_TOGGLE = "com.rtvvoice.app.reader.TOGGLE"
        private const val ACTION_NEXT = "com.rtvvoice.app.reader.NEXT"
        private const val ACTION_PREV = "com.rtvvoice.app.reader.PREV"
        private const val ACTION_STOP = "com.rtvvoice.app.reader.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, ReaderService::class.java),
            )
        }
    }
}
