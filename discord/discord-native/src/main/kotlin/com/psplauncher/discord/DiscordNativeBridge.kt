package com.psplauncher.discord

import android.app.Activity
import com.discord.socialsdk.DiscordSocialSdkInit
import com.psplauncher.core.domain.discord.DiscordConfig
import java.util.concurrent.atomic.AtomicBoolean

object DiscordNativeBridge {
    private val libraryLoaded = AtomicBoolean(false)
    private val clientStarted = AtomicBoolean(false)

    private fun ensureLibraryLoaded() {
        if (libraryLoaded.compareAndSet(false, true)) System.loadLibrary("discord_bridge")
    }

    fun attachActivity(activity: Activity) {
        ensureLibraryLoaded()
        DiscordSocialSdkInit.setEngineActivity(activity)
    }

    fun ensureInitialized() {
        ensureLibraryLoaded()
        if (clientStarted.compareAndSet(false, true)) {
            nativeInit(DiscordConfig.APPLICATION_ID.toLong())
        }
    }

    fun updateToken(accessToken: String): Boolean {
        ensureInitialized()
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
