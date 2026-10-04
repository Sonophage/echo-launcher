package com.echo.discord

import android.app.Activity
import com.discord.socialsdk.DiscordSocialSdkInit
import com.echo.core.domain.discord.DiscordConfig
import java.lang.ref.WeakReference

object DiscordNativeBridge {
    private var libraryLoaded = false
    private var engineActivity: WeakReference<Activity>? = null
    @Volatile private var clientReady = false

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

    @Synchronized
    private fun startClient(): Boolean {
        if (clientReady) return true
        if (!ensureLibraryLoaded()) return false
        nativeInit(DiscordConfig.APPLICATION_ID.toLong())
        clientReady = true
        return true
    }

    fun updateToken(accessToken: String): Boolean {
        if (!startClient()) return false
        return nativeUpdateToken(accessToken)
    }

    fun disconnect() {
        if (clientReady) nativeDisconnect()
    }

    fun currentUserJson(): String = if (clientReady) nativeGetCurrentUserJson() else ""

    fun friendsJson(): String = if (clientReady) nativeGetFriendsJson() else "[]"

    fun setActivity(name: String, details: String) {
        if (clientReady) nativeSetActivity(name, details)
    }

    fun clearActivity() {
        if (clientReady) nativeClearActivity()
    }

    private external fun nativeInit(applicationId: Long)
    private external fun nativeUpdateToken(token: String): Boolean
    private external fun nativeDisconnect()
    private external fun nativeGetCurrentUserJson(): String
    private external fun nativeGetFriendsJson(): String
    private external fun nativeSetActivity(name: String, details: String)
    private external fun nativeClearActivity()
}
