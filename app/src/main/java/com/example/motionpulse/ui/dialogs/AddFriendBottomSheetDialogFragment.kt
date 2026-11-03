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
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.LayoutAddFriendBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.domain.models.UiState
import com.example.motionpulse.ui.adapters.SentRequestsAdapter
import com.example.motionpulse.ui.viewmodels.CommunityViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        // My Friend Code display (placeholder based on UID or preference)
        val myCode = "MP-${currentUserId.take(4).uppercase()}-${currentUserId.takeLast(4).uppercase()}"
        binding.tvMyFriendCode.text = myCode

        binding.btnCopyCode.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("Friend Code", myCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareCode.setOnClickListener {
            val shareText = "Join me on Motion.Pulse! Add me with my friend code: $myCode"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Friend Code"))
        }

        // Code formatting watcher: MP-XXXX-XXXX
        binding.etCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) return
                isFormatting = true

                val clean = s?.toString()?.replace("-", "")?.uppercase() ?: ""
                val formatted = StringBuilder()
                for (i in clean.indices) {
                    if (i == 2 || i == 6) formatted.append("-")
                    formatted.append(clean[i])
                }

                binding.etCode.setText(formatted.toString())
                binding.etCode.setSelection(formatted.length)
                isFormatting = false
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(viewModel)
                true
            } else false
        }

        binding.btnPasteCode.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pasted = clipData.getItemAt(0).text?.toString() ?: ""
                binding.etCode.setText(pasted)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.searchResult.collectLatest { result ->
                        if (result != null) {
                            binding.cardSearchResult.visibility = View.VISIBLE
                            binding.searchAvatar.setDisplayName(result.displayName)
                            binding.tvSearchName.text = result.displayName
                            binding.btnSendRequest.setOnClickListener {
                                viewModel.sendFriendRequest(result.uid)
                                Toast.makeText(requireContext(), "Request sent", Toast.LENGTH_SHORT).show()
                                binding.cardSearchResult.visibility = View.GONE
                                binding.etCode.setText("")
                            }
                        } else {
                            binding.cardSearchResult.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.searchError.collectLatest { error ->
                        if (!error.isNullOrBlank()) {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
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
        val code = binding.etCode.text?.toString()?.trim() ?: ""
        if (code.isNotEmpty()) {
            viewModel.searchByCode(code)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
