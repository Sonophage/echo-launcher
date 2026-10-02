package com.psplauncher.core.ui.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AndroidNotice(

    val key: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,

    val canOpen: Boolean = false,

    val canDismiss: Boolean = false,

    val packageName: String = "",
)

data class NoticeExtras(
    val title: String? = null,
    val bigTitle: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val message: String? = null,
    val subText: String? = null,
    val infoText: String? = null,
)

fun noticeOf(
    key: String,
    appLabel: String,
    postedAt: Long,
    extras: NoticeExtras,
    isGroupSummary: Boolean = false,
    canOpen: Boolean = false,
    canDismiss: Boolean = false,
    packageName: String = "",
): AndroidNotice? {
    if (isGroupSummary) return null
    return AndroidNotice(
        key = key,
        appLabel = appLabel,
        title = firstFilled(extras.title, extras.bigTitle) ?: appLabel,
        text = firstFilled(extras.text, extras.bigText, extras.message, extras.subText, extras.infoText),
        postedAt = postedAt,
        canOpen = canOpen,
        canDismiss = canDismiss,
        packageName = packageName,
    )
}

private fun firstFilled(vararg candidates: String?): String? =
    candidates.firstOrNull { !it.isNullOrBlank() }?.trim()

object AndroidNotifications {
    private val _active = MutableStateFlow<List<AndroidNotice>>(emptyList())

    val active: StateFlow<List<AndroidNotice>> = _active

    interface NoticeActions {
        fun open(key: String): Boolean

        fun dismiss(key: String)
    }

    private var actions: NoticeActions? = null

    fun attach(actions: NoticeActions) {
        this.actions = actions
    }

    fun publish(notices: List<AndroidNotice>) {
        _active.value = notices.sortedByDescending { it.postedAt }
    }

    fun disconnected() {
        _active.value = emptyList()
        actions = null
    }

    fun open(key: String): Boolean = actions?.open(key) ?: false

    fun dismiss(key: String) {
        actions?.dismiss(key)
    }

    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, ENABLED_LISTENERS).orEmpty()
        if (flat.isBlank()) return false
        return flat.split(':').any { entry ->
            ComponentName.unflattenFromString(entry)?.packageName == context.packageName
        }
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    private const val ENABLED_LISTENERS = "enabled_notification_listeners"
}
