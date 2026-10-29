package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.motionpulse.R
import com.example.motionpulse.databinding.FragmentBadgesGalleryBinding

class BadgesGalleryFragment : Fragment() {

    private var _binding: FragmentBadgesGalleryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBadgesGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dropletHeader.setTitle("Badge Gallery")
        binding.dropletHeader.setCaption("MOTION.PULSE")

        val backIcon = ImageView(requireContext()).apply {
            setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back))
            setOnClickListener { findNavController().popBackStack() }
        }
        binding.dropletHeader.setNavigationIconView(backIcon)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
