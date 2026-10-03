package com.psplauncher.feature.settings.ui

import android.content.ClipData
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.psplauncher.core.domain.discord.DeviceLoginState
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.detail.PfpDetailLaunchButton
import com.psplauncher.core.ui.detail.PfpOverlayCard
import com.psplauncher.core.ui.detail.PfpOverlayTitle
import com.psplauncher.core.ui.sound.LocalMenuSounds
import com.psplauncher.core.ui.sound.MenuSound
import com.psplauncher.feature.settings.viewmodel.DiscordSettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DiscordSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscordSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SettingsPageScaffold(
            subtitle = "Discord",
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        ) {
            val scrollState = rememberScrollState()
            LocalSettingsScrollStateRegistrar.current(scrollState)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                SettingsGroup("Account")
                SettingsValueRow(
                    label = "Signed in as",
                    value = when {
                        state.user != null -> state.user!!.label
                        state.signedIn -> "Connecting…"
                        else -> "Not signed in"
                    },
                )
                if (state.signedIn) {
                    SettingsRow(
                        label = "Sign out",
                        sublabel = "Removes the Discord sign-in from this device",
                        onClick = { confirmSignOut = true },
                    )
                } else {
                    SettingsRow(
                        label = "Sign in with QR code",
                        sublabel = "Scan the code with your phone, or enter it at discord.com/activate",
                        onClick = viewModel::startSignIn,
                    )
                }

                SettingsGroup("Presence")
                SettingsToggleRow(
                    label = "Share what I'm playing",
                    sublabel = "Friends see the game you launch from PSPLauncher. Off until you turn it on.",
                    checked = state.shareActivity,
                    onToggle = viewModel::setShareActivity,
                )
                SettingsToggleRow(
                    label = "Hide game titles",
                    sublabel = "Friends see \"a game\" instead of the title",
                    checked = state.genericActivity,
                    onToggle = viewModel::setGenericActivity,
                )
            }
        }

        state.login?.let { login ->
            DiscordSignInOverlay(
                state = login,
                onRetry = viewModel::startSignIn,
                onCancel = viewModel::cancelSignIn,
            )
        }

        if (confirmSignOut) {
            SettingsConfirmOverlay(
                title = "Sign out of Discord?",
                message = "Friends and presence stop until you sign in again.",
                confirmLabel = "Sign out",
                onConfirm = { confirmSignOut = false; viewModel.signOut() },
                onCancel = { confirmSignOut = false },
            )
        }
    }
}

private enum class SignInAction(val label: String) { COPY("Copy code"), RETRY("Try again"), CANCEL("Cancel") }

@Composable
private fun DiscordSignInOverlay(
    state: DeviceLoginState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val sounds = LocalMenuSounds.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val challenge = (state as? DeviceLoginState.AwaitingApproval)?.challenge
    var copied by remember(challenge) { mutableStateOf(false) }

    val actions = when (state) {
        is DeviceLoginState.AwaitingApproval -> listOf(SignInAction.COPY, SignInAction.CANCEL)
        DeviceLoginState.Expired, DeviceLoginState.Denied, is DeviceLoginState.Error ->
            listOf(SignInAction.RETRY, SignInAction.CANCEL)
        else -> listOf(SignInAction.CANCEL)
    }
    var cursor by remember(actions) { mutableIntStateOf(0) }

    val run: (SignInAction) -> Unit = { action ->
        when (action) {
            SignInAction.COPY -> {
                sounds(MenuSound.SELECT)
                challenge?.let { c ->
                    scope.launch {
                        clipboard.setClipEntry(ClipData.newPlainText("Discord code", c.userCode).toClipEntry())
                    }
                    copied = true
                }
            }
            SignInAction.RETRY -> { sounds(MenuSound.SELECT); onRetry() }
            SignInAction.CANCEL -> { sounds(MenuSound.BACK); onCancel() }
        }
    }

    SettingsOverlayInput { action ->
        when (action) {
            GamepadAction.NAVIGATE_UP -> if (cursor > 0) { cursor--; sounds(MenuSound.SCROLL) }
            GamepadAction.NAVIGATE_DOWN -> if (cursor < actions.lastIndex) { cursor++; sounds(MenuSound.SCROLL) }
            GamepadAction.SELECT -> actions.getOrNull(cursor)?.let(run)
            GamepadAction.BACK -> run(SignInAction.CANCEL)
            else -> Unit
        }
    }

    PfpOverlayCard(onScrimTap = { run(SignInAction.CANCEL) }) {
        PfpOverlayTitle("Sign in with Discord")
        Spacer(Modifier.height(12.dp))
        when (state) {
            is DeviceLoginState.AwaitingApproval -> QrPrompt(state, copied)
            DeviceLoginState.Requesting -> OverlayMessage("Asking Discord for a code…")
            is DeviceLoginState.Success -> OverlayMessage("Signed in")
            DeviceLoginState.Expired -> OverlayMessage("The code expired before it was used.")
            DeviceLoginState.Denied -> OverlayMessage("The sign-in was declined.")
            is DeviceLoginState.Error -> OverlayMessage("Could not sign in. ${state.message}")
        }
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEachIndexed { index, action ->
                PfpDetailLaunchButton(
                    label = if (action == SignInAction.COPY && copied) "Copied" else action.label,
                    icon = null,
                    focused = index == cursor,
                    onClick = { cursor = index; run(action) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun QrPrompt(state: DeviceLoginState.AwaitingApproval, copied: Boolean) {
    val challenge = state.challenge
    val qr = remember(challenge.verificationUriComplete) {
        qrBitmap(challenge.verificationUriComplete, 512).asImageBitmap()
    }
    var remaining by remember(challenge) { mutableIntStateOf(challenge.expiresInSeconds) }
    LaunchedEffect(challenge) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(bitmap = qr, contentDescription = "Discord sign-in QR code", modifier = Modifier.size(170.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(challenge.userCode, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            OverlayMessage("Scan with your phone's camera, or go to discord.com/activate and enter the code.")
            OverlayMessage(
                when {
                    copied -> "Code copied"
                    remaining > 0 -> "Expires in ${remaining / 60}:${"%02d".format(remaining % 60)}"
                    else -> "Expired"
                },
            )
        }
    }
}

@Composable
private fun OverlayMessage(text: String) {
    Text(text, color = Color(0xCCFFFFFF), fontSize = 14.sp)
}

private fun qrBitmap(content: String, sizePx: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 2,
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height) { i ->
        if (matrix.get(i % width, i / width)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
