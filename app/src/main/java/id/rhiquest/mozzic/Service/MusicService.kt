package id.rhiquest.mozzic.Service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import id.rhiquest.mozzic.MainActivity
import id.rhiquest.mozzic.R

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "moozic_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "id.rhiquest.mozzic.ACTION_PLAY"
        const val ACTION_PAUSE = "id.rhiquest.mozzic.ACTION_PAUSE"
        const val ACTION_STOP = "id.rhiquest.mozzic.ACTION_STOP"
        const val ACTION_NEXT = "id.rhiquest.mozzic.ACTION_NEXT"
        const val ACTION_PREVIOUS = "id.rhiquest.mozzic.ACTION_PREVIOUS"
        const val ACTION_SEEK_TO = "id.rhiquest.mozzic.ACTION_SEEK_TO"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SINGER = "extra_singer"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"
        const val EXTRA_CURRENT_SECOND = "extra_current_second"
        const val EXTRA_TOTAL_DURATION = "extra_total_duration"
        const val EXTRA_SEEK_POSITION = "extra_seek_position"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var cachedAlbumArt: Bitmap? = null
    private var cachedThumbnailUrl: String? = null
    private var mediaSession: MediaSessionCompat? = null

    // Cache last known values
    private var lastTitle = "Moozic"
    private var lastSinger = ""
    private var lastIsPlaying = true
    private var lastCurrentSecond = 0f
    private var lastTotalDuration = 0f

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        initMediaSession()
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MoozicSession").apply {
            // Handle media button actions and seek
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    sendBroadcast(Intent(ACTION_PLAY))
                }

                override fun onPause() {
                    sendBroadcast(Intent(ACTION_PAUSE))
                }

                override fun onStop() {
                    sendBroadcast(Intent(ACTION_STOP))
                }

                override fun onSkipToNext() {
                    sendBroadcast(Intent(ACTION_NEXT))
                }

                override fun onSkipToPrevious() {
                    sendBroadcast(Intent(ACTION_PREVIOUS))
                }

                override fun onSeekTo(pos: Long) {
                    // pos is in milliseconds, convert to seconds for broadcast
                    val seekSeconds = pos / 1000f
                    sendBroadcast(Intent(ACTION_SEEK_TO).apply {
                        putExtra(EXTRA_SEEK_POSITION, seekSeconds)
                    })
                }
            })

            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                sendBroadcast(Intent(ACTION_PLAY))
                return START_STICKY
            }
            ACTION_PAUSE -> {
                sendBroadcast(Intent(ACTION_PAUSE))
                return START_STICKY
            }
            ACTION_NEXT -> {
                sendBroadcast(Intent(ACTION_NEXT))
                return START_STICKY
            }
            ACTION_PREVIOUS -> {
                sendBroadcast(Intent(ACTION_PREVIOUS))
                return START_STICKY
            }
            ACTION_STOP -> {
                sendBroadcast(Intent(ACTION_STOP))
                cachedAlbumArt = null
                cachedThumbnailUrl = null
                mediaSession?.isActive = false
                mediaSession?.release()
                mediaSession = null
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        lastTitle = intent?.getStringExtra(EXTRA_TITLE) ?: lastTitle
        lastSinger = intent?.getStringExtra(EXTRA_SINGER) ?: lastSinger
        lastIsPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, lastIsPlaying) ?: lastIsPlaying
        lastCurrentSecond = intent?.getFloatExtra(EXTRA_CURRENT_SECOND, lastCurrentSecond) ?: lastCurrentSecond
        lastTotalDuration = intent?.getFloatExtra(EXTRA_TOTAL_DURATION, lastTotalDuration) ?: lastTotalDuration
        val thumbnailUrl = intent?.getStringExtra(EXTRA_THUMBNAIL_URL)

        // Update MediaSession state (this drives the seekable progress bar)
        updateMediaSessionState()
        updateMediaSessionMetadata()

        // If thumbnail URL changed, load new bitmap
        if (thumbnailUrl != null && thumbnailUrl != cachedThumbnailUrl) {
            cachedThumbnailUrl = thumbnailUrl
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)

            loadAlbumArt(thumbnailUrl) { bitmap ->
                cachedAlbumArt = bitmap
                updateMediaSessionMetadata() // Update metadata with album art
                val updatedNotification = buildNotification()
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, updatedNotification)
            }
        } else {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    private fun updateMediaSessionState() {
        val state = if (lastIsPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackSpeed = if (lastIsPlaying) 1f else 0f
        val positionMs = (lastCurrentSecond * 1000).toLong()

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, positionMs, playbackSpeed, SystemClock.elapsedRealtime())
            .build()

        mediaSession?.setPlaybackState(playbackState)
    }

    private fun updateMediaSessionMetadata() {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, lastTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, lastSinger)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (lastTotalDuration * 1000).toLong())

        if (cachedAlbumArt != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cachedAlbumArt)
        }

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Moozic::MusicPlaybackLock"
        ).apply {
            acquire(60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi saat musik sedang diputar"
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun loadAlbumArt(url: String, onLoaded: (Bitmap?) -> Unit) {
        try {
            Glide.with(this)
                .asBitmap()
                .load(url)
                .override(512, 512)
                .centerCrop()
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        onLoaded(resource)
                    }

                    override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                        onLoaded(null)
                    }
                })
        } catch (e: Exception) {
            onLoaded(null)
        }
    }

    private fun buildNotification(): Notification {
        val isPlaying = lastIsPlaying

        // Intent to open app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntents for buttons
        val previousPendingIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPausePendingIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextPendingIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MusicService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPendingIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play

        // MediaStyle notification with MediaSession — system renders seekable progress bar
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(lastTitle)
            .setContentText(lastSinger)
            .setSubText("Moozic")
            .setSmallIcon(R.drawable.ic_notif_music)
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(R.drawable.ic_notif_previous, "Previous", previousPendingIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePendingIntent)
            .addAction(R.drawable.ic_notif_next, "Next", nextPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        // Set album art as large icon
        if (cachedAlbumArt != null) {
            builder.setLargeIcon(cachedAlbumArt)
        }

        return builder.build()
    }
}
