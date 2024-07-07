package com.example.graduationproject.ui.authenticationScreens.phoneAuthentication.otp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.example.graduationproject.R
import com.example.graduationproject.databinding.FragmentOtpBinding
import com.example.graduationproject.ui.MainActivity
import com.example.graduationproject.ui.base.BaseFragment
import com.example.graduationproject.utilities.collectLast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OtpFragment : BaseFragment<FragmentOtpBinding>(){

    override val layoutIdFragment: Int = R.layout.fragment_otp
    override val viewModel: OtpViewModel by viewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
         binding.viewmodel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        collectLast(viewModel.otpUIEvent){
            it.getContentIfNotHandled()?.let { onEvent(it) }
        }

    }

    private fun onEvent(event: OtpUiEvent) {
        when(event){
            OtpUiEvent.Verification -> {
                ( activity as MainActivity).binding.mainGroup.visibility = View.VISIBLE
                ( activity as MainActivity).binding.fragmentContainerView.visibility = View.GONE
            }
        }
    }

}