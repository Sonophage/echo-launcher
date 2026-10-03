package com.psplauncher.discord

import android.app.Activity
import com.discord.socialsdk.DiscordSocialSdkInit
import com.psplauncher.core.domain.discord.DiscordConfig
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

object DiscordNativeBridge {
    private var libraryLoaded = false
    private var engineActivity: WeakReference<Activity>? = null
    private val clientStarted = AtomicBoolean(false)

    @Synchronized
    fun attachActivity(activity: Activity) {
        engineActivity = WeakReference(activity)
        if (libraryLoaded) DiscordSocialSdkInit.setEngineActivity(activity)
    }

    @Synchronized
    private fun ensureLibraryLoaded(): Boolean {
        if (libraryLoaded) return true
        val activity = engineActivity?.get() ?: return false
        System.loadLibrary("discord_bridge")
        DiscordSocialSdkInit.setEngineActivity(activity)
        libraryLoaded = true
        return true
    }

    fun updateToken(accessToken: String): Boolean {
        if (!ensureLibraryLoaded()) return false
        if (clientStarted.compareAndSet(false, true)) {
            nativeInit(DiscordConfig.APPLICATION_ID.toLong())
        }
        return nativeUpdateToken(accessToken)
    }

    fun disconnect() {
        if (clientStarted.get()) nativeDisconnect()
    }

    fun status(): Int = if (clientStarted.get()) nativeGetStatus() else 0

    fun currentUserJson(): String = if (clientStarted.get()) nativeGetCurrentUserJson() else ""

    fun friendsJson(): String = if (clientStarted.get()) nativeGetFriendsJson() else "[]"

    fun setActivity(name: String, details: String) {
        if (clientStarted.get()) nativeSetActivity(name, details)
    }

    fun clearActivity() {
        if (clientStarted.get()) nativeClearActivity()
    }

    fun shutdown() {
        if (clientStarted.compareAndSet(true, false)) nativeShutdown()
    }

    private external fun nativeInit(applicationId: Long)
    private external fun nativeUpdateToken(token: String): Boolean
    private external fun nativeDisconnect()
    private external fun nativeGetStatus(): Int
    private external fun nativeGetCurrentUserJson(): String
    private external fun nativeGetFriendsJson(): String
    private external fun nativeSetActivity(name: String, details: String)
    private external fun nativeClearActivity()
    private external fun nativeShutdown()
}
