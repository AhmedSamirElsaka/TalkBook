package com.example.graduationproject.ui.authenticationScreens.phoneAuthentication.phone

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.graduationproject.R
import com.example.graduationproject.databinding.FragmentPhoneBinding
import com.example.graduationproject.ui.base.BaseFragment
import com.example.graduationproject.utilities.collectLast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class PhoneFragment : BaseFragment<FragmentPhoneBinding>() {
    override val layoutIdFragment: Int = R.layout.fragment_phone
    override val viewModel: SendOtpUsingPhoneNumberViewModel by viewModels()


    @Inject
    lateinit var auth: FirebaseAuth


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        collectLast(viewModel.phoneUIEvent){
            it.getContentIfNotHandled()?.let { onEvent(it) }
        }

    }

    private fun onEvent(event: PhoneUiEvent) {
         when(event){
             is PhoneUiEvent.CodeSent ->  {
                 val action = PhoneFragmentDirections.actionPhoneFragmentToOtpFragment(event.verificationId)
                 findNavController().navigate(action)
             }
             is PhoneUiEvent.SendVerifactionCode ->  sendVerifactionCode(event.phoneNumber)
         }
    }


    private fun sendVerifactionCode(phoneNumber :String) {
        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber("+2${phoneNumber}")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(viewModel.callbacks)
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


}
