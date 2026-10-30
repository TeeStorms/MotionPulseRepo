package com.example.motionpulse.ui.fragments

import android.animation.ObjectAnimator
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.motionpulse.R
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentLandingBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LandingFragment : Fragment() {

    private var _binding: FragmentLandingBinding? = null
    private val binding get() = _binding!!

    private val preferenceRepository by lazy { PreferenceRepository(requireContext()) }
    private val authViewModel: AuthViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(preferenceRepository = preferenceRepository) as T
            }
        }
    }

    private var exoPlayer: ExoPlayer? = null
    private var sessionCheckJob: Job? = null
    private var sessionCheckDone = false
    private var sessionValid = false
    private var skipTapped = false
    private var videoEnded = false
    private var hasNavigated = false

    private val INTRO_EXIT_AFTER_MS = 6000L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check animator duration scale
        val animatorScale = android.provider.Settings.Global.getFloat(
            requireContext().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        if (animatorScale == 0f) {
            proceedImmediately()
            return
        }

        // Start session check in parallel
        sessionCheckJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                authViewModel.checkSession()
                sessionValid = authViewModel.authState.value is AuthState.Authenticated
            } catch (e: Exception) {
                android.util.Log.e("LandingFragment", "Session check error: ${e.message}", e)
                sessionValid = false
            } finally {
                sessionCheckDone = true
                checkAndProceed()
            }
        }

        // Show "Checking your session..." if session check takes longer than 2.5s
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2500)
            if (!sessionCheckDone && _binding != null) {
                binding.tvChecking.visibility = View.VISIBLE
            }
        }

        // Show Skip button after ~1s
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)
            if (!hasNavigated && _binding != null) {
                binding.btnSkip.visibility = View.VISIBLE
            }
        }

        binding.btnSkip.setOnClickListener {
            skipTapped = true
            checkAndProceed(force = true)
        }

        // Setup ExoPlayer
        setupExoPlayer()
    }

    private fun setupExoPlayer() {
        val context = context ?: return
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            binding.playerView.player = this
            volume = 0f
            repeatMode = Player.REPEAT_MODE_OFF

            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.intro_video}")
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                private var firstFrameRendered = false

                override fun onRenderedFirstFrame() {
                    if (!firstFrameRendered) {
                        firstFrameRendered = true
                        if (_binding != null) {
                            ObjectAnimator.ofFloat(binding.playerView, View.ALPHA, 0f, 1f).apply {
                                duration = 400
                                start()
                            }
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        videoEnded = true
                        checkAndProceed()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    android.util.Log.e("LandingFragment", "ExoPlayer error: ${error.message}", error)
                    proceedImmediately()
                }
            })
        }

        // 3s timeout for first frame render
        viewLifecycleOwner.lifecycleScope.launch {
            delay(3000)
            if (exoPlayer?.isPlaying != true && !hasNavigated) {
                proceedImmediately()
            }
        }
    }

    private fun checkAndProceed(force: Boolean = false) {
        if (hasNavigated) return

        val player = exoPlayer
        val currentPosition = player?.currentPosition ?: 0L
        val conditionMet = sessionCheckDone && (force || skipTapped || videoEnded || currentPosition >= INTRO_EXIT_AFTER_MS)

        if (conditionMet) {
            navigateToNextDestination()
        }
    }

    private fun navigateToNextDestination() {
        if (hasNavigated) return
        hasNavigated = true

        sessionCheckJob?.cancel()
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null

        val destination = if (sessionValid) {
            R.id.dashboard
        } else {
            R.id.login
        }

        if (isResumed) {
            findNavController().navigate(
                destination,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.landing, true)
                    .build()
            )
        }
    }

    private fun proceedImmediately() {
        sessionCheckDone = true
        sessionValid = authViewModel.authState.value is AuthState.Authenticated
        navigateToNextDestination()
    }

    override fun onStart() {
        super.onStart()
        exoPlayer?.play()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
    }
}
