package com.example.graduationproject.ui.authenticationScreens.resetPassword

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.graduationproject.R
import com.example.graduationproject.databinding.FragmentForgetPasswordBinding
import com.example.graduationproject.ui.base.BaseFragment
import com.example.graduationproject.utilities.collectLast
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ForgetPasswordFragment : BaseFragment<FragmentForgetPasswordBinding>() {
    override val layoutIdFragment: Int = R.layout.fragment_forget_password
    override val viewModel: ForgetPasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        collectLast(viewModel.forgetPasswordUIEvent){
            it.getContentIfNotHandled()?.let { onEvent(it) }
        }

    }

    private fun onEvent(event: ForgetPasswordUiEvent) {
        when (event) {
            ForgetPasswordUiEvent.SendResetPassword -> {
                Toast.makeText(requireContext(), "Check Your Email , Please", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_forgetPasswordFragment_to_logInFragment)
            }
        }
    }

}