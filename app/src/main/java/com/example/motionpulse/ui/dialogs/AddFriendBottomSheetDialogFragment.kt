package com.example.motionpulse.ui.dialogs

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.motionpulse.BuildConfig
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.FriendCodeDataSource
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.LayoutAddFriendBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.domain.models.FriendSearchUiState
import com.example.motionpulse.domain.models.UiState
import com.example.motionpulse.ui.adapters.SentRequestsAdapter
import com.example.motionpulse.ui.viewmodels.CommunityViewModel
import com.example.motionpulse.util.ConnectivityMonitor
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AddFriendBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutAddFriendBinding? = null
    private val binding get() = _binding!!

    private val preferenceRepository by lazy { PreferenceRepository(requireContext()) }
    private val authViewModel: AuthViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(preferenceRepository = preferenceRepository) as T
            }
        }
    }

    private val db by lazy {
        Room.databaseBuilder(
            requireContext().applicationContext,
            AppDatabase::class.java, "motion_pulse_db"
        ).fallbackToDestructiveMigration().build()
    }

    private lateinit var sentAdapter: SentRequestsAdapter
    private var isFormatting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutAddFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.authState.collectLatest { state ->
                        val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            val communityViewModel = ViewModelProvider(
                                requireActivity(),
                                CommunityViewModel.Factory(db, uid, requireContext())
                            )[CommunityViewModel::class.java]

                            setupAddFriendContent(communityViewModel, uid)
                        }
                    }
                }
            }
        }
    }

    private fun setupAddFriendContent(viewModel: CommunityViewModel, currentUserId: String) {
        sentAdapter = SentRequestsAdapter(
            onCancel = { request -> viewModel.cancelSentRequest(request.id) }
        )

        binding.rvSentRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSentRequests.adapter = sentAdapter

        // My Friend Code display (Setting up your code... while loading)
        binding.tvMyFriendCode.text = "Setting up your code..."
        binding.btnCopyCode.isEnabled = false
        binding.btnShareCode.isEnabled = false

        if (BuildConfig.DEBUG) {
            binding.tvMyFriendCode.setOnLongClickListener {
                runFriendCodeProbe(currentUserId)
                true
            }
        }

        // Ensure friend code exists on open if missing
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                FriendCodeDataSource(requireContext()).ensureMyFriendCode(currentUserId)
            } catch (e: Exception) {
                android.util.Log.e("AddFriendDialog", "Error ensuring friend code on open: ${e.message}", e)
            }
        }

        binding.btnCopyCode.setOnClickListener {
            val codeToCopy = binding.tvMyFriendCode.text?.toString() ?: ""
            if (codeToCopy.isNotBlank() && codeToCopy != "Setting up your code...") {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = android.content.ClipData.newPlainText("Friend Code", codeToCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShareCode.setOnClickListener {
            val codeToShare = binding.tvMyFriendCode.text?.toString() ?: ""
            if (codeToShare.isNotBlank() && codeToShare != "Setting up your code...") {
                val shareText = "Join me on Motion.Pulse! Add me with my friend code: $codeToShare"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Friend Code"))
            }
        }

        // Search icon tap on input layout
        binding.tilCode.setEndIconOnClickListener {
            performSearch(viewModel)
        }

        // System paste / typing watcher: editing text clears old error/result
        binding.etCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                viewModel.resetSearchState()
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                val text = s?.toString() ?: ""
                if (text.length > 12 && (text.contains("MP-") || text.contains("code") || text.contains("Motion.Pulse"))) {
                    isFormatting = true
                    val extracted = com.example.motionpulse.util.FriendCodeUtil.extractCodeFromText(text)
                    binding.etCode.setText(extracted)
                    binding.etCode.setSelection(extracted.length)
                    isFormatting = false
                    performSearch(viewModel)
                }
            }
        })

        binding.etCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performSearch(viewModel)
                true
            } else false
        }

        binding.btnPasteCode.setOnClickListener {
            viewModel.resetSearchState()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pasted = clipData.getItemAt(0).text?.toString() ?: ""
                val extracted = com.example.motionpulse.util.FriendCodeUtil.extractCodeFromText(pasted)
                binding.etCode.setText(extracted)
                binding.etCode.setSelection(extracted.length)
                performSearch(viewModel)
            }
        }

        // Collect ConnectivityMonitor to auto-repeat search on reconnect
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var wasOffline = false
                ConnectivityMonitor(requireContext()).isConnectedFlow.collectLatest { isConnected ->
                    if (!isConnected) {
                        wasOffline = true
                    } else if (wasOffline) {
                        wasOffline = false
                        performSearch(viewModel)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userProfile.collectLatest { profile ->
                        if (profile != null && profile.friendCode.isNotBlank()) {
                            binding.tvMyFriendCode.text = profile.friendCode
                            binding.btnCopyCode.isEnabled = true
                            binding.btnShareCode.isEnabled = true
                        }
                    }
                }

                launch {
                    viewModel.searchState.collectLatest { state ->
                        when (state) {
                            is FriendSearchUiState.Idle,
                            is FriendSearchUiState.Typing -> {
                                binding.cardSearchResult.visibility = View.GONE
                                binding.tilCode.error = null
                                binding.tilCode.helperText = null
                            }
                            is FriendSearchUiState.Loading -> {
                                binding.cardSearchResult.visibility = View.GONE
                                binding.tilCode.error = null
                                binding.tilCode.helperText = "Searching..."
                            }
                            is FriendSearchUiState.Found -> {
                                binding.cardSearchResult.visibility = View.VISIBLE
                                binding.searchAvatar.setDisplayName(state.profile.displayName)
                                binding.tvSearchName.text = "${state.profile.displayName} (Lvl ${state.profile.currentLevel})"
                                binding.btnSendRequest.setOnClickListener {
                                    viewModel.sendFriendRequest(state.profile.uid, state.profile.displayName)
                                    Toast.makeText(requireContext(), "Request sent to ${state.profile.displayName}", Toast.LENGTH_SHORT).show()
                                    binding.cardSearchResult.visibility = View.GONE
                                    binding.etCode.setText("")
                                    viewModel.resetSearchState()
                                }
                                binding.tilCode.error = null
                                binding.tilCode.helperText = null
                            }
                            is FriendSearchUiState.Error -> {
                                binding.cardSearchResult.visibility = View.GONE
                                binding.tilCode.error = state.message
                                if (BuildConfig.DEBUG && !state.debugCode.isNullOrBlank()) {
                                    binding.tilCode.helperText = state.debugCode
                                } else {
                                    binding.tilCode.helperText = null
                                }
                            }
                            is FriendSearchUiState.RateLimited -> {
                                binding.cardSearchResult.visibility = View.GONE
                                val msg = "Too many searches. Try again in ${state.seconds} seconds."
                                binding.tilCode.error = msg
                                binding.tilCode.helperText = null
                            }
                        }
                    }
                }

                launch {
                    viewModel.sentRequestsState.collectLatest { state ->
                        if (state is UiState.Success) {
                            sentAdapter.submitList(state.data)
                        } else {
                            sentAdapter.submitList(emptyList())
                        }
                    }
                }
            }
        }
    }

    private fun performSearch(viewModel: CommunityViewModel) {
        val rawInput = binding.etCode.text?.toString()?.trim() ?: ""
        if (rawInput.isEmpty()) return
        val extracted = com.example.motionpulse.util.FriendCodeUtil.extractCodeFromText(rawInput)
        when (val res = com.example.motionpulse.util.FriendCodeUtil.normalize(extracted)) {
            is com.example.motionpulse.util.FriendCodeResult.Valid -> {
                binding.tilCode.error = null
                viewModel.searchByCode(res.canonicalCode, requireContext())
            }
            is com.example.motionpulse.util.FriendCodeResult.InvalidFormat -> {
                binding.tilCode.error = res.message
            }
        }
    }

    private fun runFriendCodeProbe(uid: String) {
        if (!BuildConfig.DEBUG) return
        viewLifecycleOwner.lifecycleScope.launch {
            val sb = StringBuilder()
            val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId ?: "unknown"

            // Step 1: Network & Transport
            val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
            val isValidated = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            val isWifi = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val isVpn = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
            val transportStr = when {
                isVpn -> "VPN"
                isWifi -> "Wi-Fi"
                isCellular -> "Cellular"
                else -> "None/Other"
            }
            val privateDnsMode = try {
                android.provider.Settings.Global.getString(requireContext().contentResolver, "private_dns_mode") ?: "off"
            } catch (e: Exception) { "unknown" }

            sb.append("1. Network: PASS (Validated: $isValidated, Transport: $transportStr, Private DNS: $privateDnsMode)\n\n")

            // Step 2: DNS
            try {
                withTimeout(5000) {
                    withContext(Dispatchers.IO) {
                        java.net.InetAddress.getByName("firestore.googleapis.com")
                    }
                }
                sb.append("2. DNS firestore.googleapis.com: PASS\n\n")
            } catch (e: Exception) {
                sb.append("2. DNS firestore.googleapis.com: FAIL (${e.javaClass.simpleName} - ${e.message})\n\n")
            }

            // Step 3: HTTPS HEAD Request
            try {
                withTimeout(5000) {
                    withContext(Dispatchers.IO) {
                        val url = java.net.URL("https://firestore.googleapis.com")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "HEAD"
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        conn.responseCode
                        conn.disconnect()
                    }
                }
                sb.append("3. HTTPS HEAD firestore.googleapis.com: PASS\n\n")
            } catch (e: Exception) {
                sb.append("3. HTTPS HEAD firestore.googleapis.com: FAIL (${e.javaClass.simpleName} - ${e.message})\n\n")
            }

            // Step 4: Firestore Server Read friendCodes/{myCode}
            val cachedCode = binding.tvMyFriendCode.text?.toString() ?: ""
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            try {
                val fcDoc = firestore.collection("friendCodes").document(cachedCode).get(com.google.firebase.firestore.Source.SERVER).await()
                if (fcDoc.exists()) {
                    val owner = fcDoc.getString("uid")
                    val matchesOwner = owner == uid
                    sb.append("4. friendCodes/$cachedCode: PASS (EXISTS, uidMatches=$matchesOwner)\n\n")
                } else {
                    sb.append("4. friendCodes/$cachedCode: FAIL (MISSING)\n\n")
                }
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                sb.append("4. friendCodes/$cachedCode: FAIL (${e.code} - ${e.message})\n\n")
            } catch (e: Exception) {
                sb.append("4. friendCodes/$cachedCode: FAIL (${e.message})\n\n")
            }

            // Step 5: Firestore Server Read publicProfiles/{myUid}
            try {
                val pubDoc = firestore.collection("publicProfiles").document(uid).get(com.google.firebase.firestore.Source.SERVER).await()
                if (pubDoc.exists()) {
                    val hasField = pubDoc.contains("friendCode")
                    sb.append("5. publicProfiles/$uid: PASS (EXISTS, hasFriendCodeField=$hasField)\n\n")
                } else {
                    sb.append("5. publicProfiles/$uid: FAIL (MISSING)\n\n")
                }
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                sb.append("5. publicProfiles/$uid: FAIL (${e.code} - ${e.message})\n\n")
            } catch (e: Exception) {
                sb.append("5. publicProfiles/$uid: FAIL (${e.message})\n\n")
            }

            // Step 6: Auth Info
            sb.append("6. Auth Info: UID=$uid, EmailVerified=${authUser?.isEmailVerified == true}, ProjectID=$projectId")

            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                .setTitle("Friend Code Probe (DEBUG)")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
