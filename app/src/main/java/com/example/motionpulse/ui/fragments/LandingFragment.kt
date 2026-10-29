package com.example.motionpulse.ui.fragments

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.motionpulse.R
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentLandingBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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

        // Setup ExoPlayer intro video
        exoPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            binding.playerView.player = this
            val videoUri = android.net.Uri.parse("android.resource://${requireContext().packageName}/${R.raw.intro_video}")
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }

        setupConnectivity()

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                authViewModel.checkAndRefreshTokenOnResume()
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collectLatest { state ->
                delay(2000)
                val destination = when (state) {
                    is AuthState.Authenticated -> R.id.dashboard
                    is AuthState.AwaitingEmailVerification -> R.id.email_verification
                    is AuthState.OnboardingRequired -> R.id.onboarding
                    else -> R.id.login
                }
                if (isResumed) {
                    findNavController().navigate(
                        destination,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.landing, true)
                            .build()
                    )
                }
            }
        }
    }

    private fun setupConnectivity() {
        val connectivityManager = requireContext().getSystemService(ConnectivityManager::class.java)
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                authViewModel.updateConnectivity(false)
            }
            override fun onLost(network: Network) {
                authViewModel.updateConnectivity(true)
            }
        }
        connectivityManager?.registerNetworkCallback(networkRequest, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
    }
}
