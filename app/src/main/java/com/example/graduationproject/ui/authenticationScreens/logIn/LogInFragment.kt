package com.example.graduationproject.ui.authenticationScreens.logIn

import android.app.Activity.RESULT_CANCELED
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.graduationproject.R
import com.example.graduationproject.databinding.FragmentLogInBinding
import com.example.graduationproject.ui.MainActivity
import com.example.graduationproject.ui.base.BaseFragment
import com.example.graduationproject.utilities.GOOGLE_ACCOUNT_REQUEST
import com.example.graduationproject.utilities.collectLast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class LogInFragment : BaseFragment<FragmentLogInBinding>() {

    override val layoutIdFragment: Int = R.layout.fragment_log_in
    override val viewModel: LogInViewModel by viewModels()

    @Inject
    lateinit var signInClient: GoogleSignInClient

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        collectLast(viewModel.loginUIEvent) {
            it.getContentIfNotHandled()?.let { onEvent(it) }
        }
    }

    private fun onEvent(event: LogInUiEvent) {
        when (event) {
            LogInUiEvent.ForgetPasswordEvent -> findNavController().navigate(R.id.action_logInFragment_to_forgetPasswordFragment)
            LogInUiEvent.LogInEvent -> {
                (activity as MainActivity).binding.mainGroup.visibility = View.VISIBLE
                (activity as MainActivity).binding.fragmentContainerView.visibility = View.GONE
            }

            LogInUiEvent.LogInWithGoogleEvent -> signInClient.signInIntent.also {
                startActivityForResult(
                    it,
                    GOOGLE_ACCOUNT_REQUEST
                )
            }

            LogInUiEvent.SignUpEvent -> {
                findNavController().navigate(R.id.action_logInFragment_to_signUpFragment)
            }

            LogInUiEvent.LogInWithPhoneEvent -> findNavController().navigate(R.id.action_logInFragment_to_phoneFragment)

            LogInUiEvent.SkipUiEvent -> {
                (activity as MainActivity).binding.mainGroup.visibility = View.VISIBLE
                (activity as MainActivity).binding.fragmentContainerView.visibility = View.GONE
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_CANCELED) {
            if (requestCode == GOOGLE_ACCOUNT_REQUEST) {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
                account?.let {
                    viewModel.logInGoogleAccount(it, activity as MainActivity)
                }
            }
        }
    }


}