package com.example.motionpulse.ui.screens

import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.motionpulse.R
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun LandingScreen(
    viewModel: AuthViewModel,
    onTimeout: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isSessionChecked by remember { mutableStateOf(false) }
    var isVideoEnded by remember { mutableStateOf(false) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }
    var showSkipButton by remember { mutableStateOf(false) }

    // Check system animator duration scale (if 0f, skip video immediately)
    val animScale = remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (_: Exception) {
            1f
        }
    }

    LaunchedEffect(animScale) {
        if (animScale == 0f) {
            isVideoEnded = true
        }
    }

    // Run AuthViewModel.checkSession() in parallel as soon as LandingScreen appears
    LaunchedEffect(Unit) {
        viewModel.checkSession()
        isSessionChecked = true
    }

    // Show skip button after 1 second
    LaunchedEffect(Unit) {
        delay(1000)
        showSkipButton = true
    }

    // 3-second fallback timeout for first frame rendering
    LaunchedEffect(Unit) {
        delay(3000)
        if (!isFirstFrameRendered) {
            Log.w("LandingScreen", "First frame render timeout (3s) reached. Proceeding.")
            isVideoEnded = true
        }
    }

    // Navigate only when BOTH video has ended/skipped AND session check has completed
    LaunchedEffect(isVideoEnded, isSessionChecked) {
        if (isVideoEnded && isSessionChecked) {
            onTimeout()
        }
    }

    // ExoPlayer Setup & Lifecycle Management
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.intro_video}")
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            volume = 0f
            setAudioAttributes(AudioAttributes.DEFAULT, false) // Do not request audio focus
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isVideoEnded = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("LandingScreen", "ExoPlayer error: ${error.message}", error)
                isVideoEnded = true
            }
        }
        exoPlayer.addListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                Lifecycle.Event.ON_START -> if (!isVideoEnded) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val videoAlpha by animateFloatAsState(
        targetValue = if (isFirstFrameRendered) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "videoFadeIn"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .semantics { contentDescription = "Motion.Pulse intro animation" },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(videoAlpha)
        )

        // Show "Checking your session..." caption if video finished but session check is still running
        if (isVideoEnded && !isSessionChecked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "Checking your session...",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // "Skip" button appearing after 1 second
        if (showSkipButton && !isVideoEnded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .clickable {
                            isVideoEnded = true
                            exoPlayer.stop()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics { contentDescription = "Skip intro video button" }
                ) {
                    Text(
                        text = "Skip",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
