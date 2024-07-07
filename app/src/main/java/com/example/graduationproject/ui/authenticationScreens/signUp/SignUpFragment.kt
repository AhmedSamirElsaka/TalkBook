package com.example.graduationproject.ui.authenticationScreens.signUp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.graduationproject.R
import com.example.graduationproject.databinding.FragmentSignUpBinding
import com.example.graduationproject.ui.MainActivity

import com.example.graduationproject.ui.base.BaseFragment
import com.example.graduationproject.utilities.GOOGLE_ACCOUNT_REQUEST
import com.example.graduationproject.utilities.collectLast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignUpFragment : BaseFragment<FragmentSignUpBinding>() {

    override val layoutIdFragment: Int = R.layout.fragment_sign_up
    override val viewModel: SignUpViewModel by viewModels()

    @Inject
    lateinit var signInClient : GoogleSignInClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewmodel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        collectLast(viewModel.signUpUIEvent){
            it.getContentIfNotHandled()?.let { onEvent(it) }
        }

    }

    private fun onEvent(event: SignUpUiEvent) {
       when(event){
           SignUpUiEvent.LogInEvent ->  findNavController().navigate(R.id.action_signUpFragment_to_logInFragment)
           SignUpUiEvent.SignUpEvent -> {
               ( activity as MainActivity).binding.mainGroup.visibility = View.VISIBLE
               ( activity as MainActivity).binding.fragmentContainerView.visibility = View.GONE
           }
           SignUpUiEvent.SignUpWithGoogleEvent ->  signInClient.signInIntent.also { startActivityForResult(it, GOOGLE_ACCOUNT_REQUEST) }
           SignUpUiEvent.SignUpWithPhoneEvent -> findNavController().navigate(R.id.action_signUpFragment_to_phoneFragment)
       }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == GOOGLE_ACCOUNT_REQUEST) {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
                account?.let {
                    viewModel.signUpWithGoogleAccount(it , activity as MainActivity)
                }
            }
        }
    }

}