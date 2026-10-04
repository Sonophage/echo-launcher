package com.echo.launcher.notification

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.echo.core.ui.notification.AndroidNotice
import com.echo.core.ui.notification.ExternalPlayback
import com.echo.core.ui.notification.isMediaPlayback
import com.echo.core.ui.notification.NoticeExtras
import com.echo.core.ui.notification.noticeOf
import com.echo.core.ui.notification.AndroidNotifications
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.BundleCompat
import timber.log.Timber

class EchoNotificationListener : NotificationListenerService(), AndroidNotifications.NoticeActions {
    private var intents: Map<String, PendingIntent> = emptyMap()
    private var sessions: Map<String, MediaSessionNotice> = emptyMap()
    private val main = Handler(Looper.getMainLooper())
    private val tick = Runnable { publishPlayback() }

    private inner class MediaSessionNotice(var sbn: StatusBarNotification, val controller: MediaController?) {
        var meta: MediaMetadata? = controller?.metadata
        var art: Bitmap? = artOf(meta)

        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) = publishPlayback()

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                meta = metadata
                art = artOf(metadata)
                publishPlayback()
            }

            override fun onSessionDestroyed() {
                sessions.entries.firstOrNull { it.value === this@MediaSessionNotice }?.let { dropSessions(setOf(it.key)) }
                publishPlayback()
            }
        }

        private fun artOf(metadata: MediaMetadata?): Bitmap? =
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: runCatching { sbn.notification.getLargeIcon()?.loadDrawable(this@EchoNotificationListener)?.toBitmap() }.getOrNull()
    }

    override fun onListenerConnected() {
        Timber.i("Notification listener connected")
        AndroidNotifications.attach(this)
        republish()
    }

    override fun onListenerDisconnected() {
        Timber.i("Notification listener disconnected")
        forgetAll()
    }

    private fun forgetAll() {
        intents = emptyMap()
        dropSessions(sessions.keys)
        AndroidNotifications.disconnected()
    }

    override fun playPause() {
        val (session, state) = nowPlaying() ?: return
        val controls = session.controller ?: return
        if (state.isPlaying()) {
            controls.transportControls.pause()
        } else {
            controls.transportControls.play()
        }
    }

    override fun skipNext() {
        nowPlaying()?.first?.controller?.transportControls?.skipToNext()
    }

    override fun open(key: String): Boolean {
        val intent = intents[key] ?: return false

        return runCatching { intent.send(this, 0, null, null, null, null, balOptions()) }
            .onFailure { Timber.i(it, "Notification content intent could not be sent") }
            .isSuccess
    }

    private fun balOptions(): Bundle? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                )
                .toBundle()
        } else {
            null
        }

    override fun dismiss(key: String) {
        runCatching { cancelNotification(key) }
            .onFailure { Timber.w(it, "Could not dismiss notification") }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = republish()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = republish()

    private fun republish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: run {
            forgetAll()
            return
        }
        trackSessions(active.orEmpty().filter { it.isMediaPlayback() })
        val drawable = active.orEmpty().mapNotNull { sbn -> sbn.toNotice()?.let { sbn to it } }
        intents = drawable.mapNotNull { (sbn, _) ->
            sbn.notification?.contentIntent?.let { sbn.key to it }
        }.toMap()
        val notices = drawable.map { it.second }
        Timber.i("Notification listener: ${active.orEmpty().size} active, ${notices.size} drawable, ${intents.size} openable")
        AndroidNotifications.publish(notices)
    }

    private fun StatusBarNotification.toNotice(): AndroidNotice? {
        val n = notification ?: return null
        val extras = n.extras ?: return null

        return noticeOf(
            key = key,
            appLabel = appLabelFor(packageName),
            postedAt = postTime,
            extras = NoticeExtras(
                title = extras.string(Notification.EXTRA_TITLE),
                bigTitle = extras.string(Notification.EXTRA_TITLE_BIG),
                text = extras.string(Notification.EXTRA_TEXT),
                bigText = extras.string(Notification.EXTRA_BIG_TEXT),
                message = extras.lastMessage(),
                subText = extras.string(Notification.EXTRA_SUB_TEXT),
                infoText = extras.string(Notification.EXTRA_INFO_TEXT),
            ),
            isGroupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            canOpen = n.contentIntent != null,
            canDismiss = isClearable,
            packageName = packageName,
            mediaPlayback = isMediaPlayback(),
        )
    }

    private fun StatusBarNotification.isMediaPlayback(): Boolean {
        val n = notification ?: return false
        return isMediaPlayback(n.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true, n.category)
    }

    private fun trackSessions(media: List<StatusBarNotification>) {
        val live = media.associateBy { it.key }
        dropSessions(sessions.keys - live.keys)
        sessions = live.mapValues { (key, sbn) ->
            sessions[key]?.takeIf { it.controller != null }?.also { it.sbn = sbn }
                ?: MediaSessionNotice(sbn, controllerFor(sbn)).also { it.controller?.registerCallback(it.callback, main) }
        }
        publishPlayback()
    }

    private fun dropSessions(keys: Set<String>) {
        keys.forEach { key -> sessions[key]?.let { it.controller?.unregisterCallback(it.callback) } }
        sessions = sessions - keys
        if (sessions.isEmpty()) main.removeCallbacks(tick)
    }

    private fun controllerFor(sbn: StatusBarNotification): MediaController? = runCatching {
        val token = sbn.notification.extras?.let { BundleCompat.getParcelable(it, Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java) }
        token?.let { MediaController(this, it) }
            ?: getSystemService(MediaSessionManager::class.java)
                .getActiveSessions(ComponentName(this, EchoNotificationListener::class.java))
                .firstOrNull { it.packageName == sbn.packageName }
    }.onFailure { Timber.i(it, "No media session for ${sbn.packageName}") }.getOrNull()

    private fun PlaybackState?.isPlaying(): Boolean = this?.state == PlaybackState.STATE_PLAYING

    private fun nowPlaying(): Pair<MediaSessionNotice, PlaybackState?>? =
        sessions.values.map { it to it.controller?.playbackState }
            .maxWithOrNull(compareBy<Pair<MediaSessionNotice, PlaybackState?>> { it.second.isPlaying() }.thenBy { it.first.sbn.postTime })

    private fun publishPlayback() {
        main.removeCallbacks(tick)
        val now = nowPlaying()
        if (now == null) {
            AndroidNotifications.publishPlayback(null)
            return
        }
        val (session, state) = now
        AndroidNotifications.publishPlayback(session.toPlayback(state), session.positionMs(state))
        if (state.isPlaying()) main.postDelayed(tick, 1_000)
    }

    private fun MediaSessionNotice.durationMs(): Long = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0) ?: 0

    private fun MediaSessionNotice.positionMs(state: PlaybackState?): Long {
        val position = state?.let {
            val elapsed = if (it.state == PlaybackState.STATE_PLAYING) SystemClock.elapsedRealtime() - it.lastPositionUpdateTime else 0
            it.position + (elapsed * it.playbackSpeed).toLong()
        } ?: 0
        val duration = durationMs()
        return if (duration > 0) position.coerceIn(0, duration) else position.coerceAtLeast(0)
    }

    private fun MediaSessionNotice.toPlayback(state: PlaybackState?): ExternalPlayback {
        val extras = sbn.notification.extras
        return ExternalPlayback(
            packageName = sbn.packageName,
            appLabel = appLabelFor(sbn.packageName),
            title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
                ?: extras?.string(Notification.EXTRA_TITLE)?.takeIf { it.isNotBlank() }
                ?: appLabelFor(sbn.packageName),
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
                ?: extras?.string(Notification.EXTRA_TEXT)?.takeIf { it.isNotBlank() },
            art = art,
            playing = state.isPlaying(),
            durationMs = durationMs(),
        )
    }

    private fun Bundle.string(key: String): String? = getCharSequence(key)?.toString()

    private fun Bundle.lastMessage(): String? {
        val lines = getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        return lines?.lastOrNull { !it.toString().isBlank() }?.toString()
    }

    private val appLabels = HashMap<String, String>()

    private fun appLabelFor(pkg: String): String = appLabels.getOrPut(pkg) {
        runCatching {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(pkg)
    }
}
