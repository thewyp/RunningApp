package com.thewyp.runningapp.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.thewyp.runningapp.R
import com.thewyp.runningapp.databinding.FragmentSetupBinding

class SetupFragment : Fragment(R.layout.fragment_setup) {

    private var _binding: FragmentSetupBinding? = null

    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSetupBinding.bind(view)

        binding.tvContinue.setOnClickListener {
            findNavController().navigate(R.id.action_setupFragment_to_runFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}