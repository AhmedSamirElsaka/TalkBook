package com.example.graduationproject.ui.authenticationScreens.signUp

import android.app.Activity
import android.view.View
import androidx.lifecycle.viewModelScope
import com.example.graduationproject.domain.entity.AuthenticationState
import com.example.graduationproject.domain.usecases.RegisterUsingGoogleAccountUseCase
import com.example.graduationproject.domain.usecases.SignUpWithEmailAndPasswordUseCase
import com.example.graduationproject.ui.MainActivity
import com.example.graduationproject.ui.base.BaseViewModel
import com.example.graduationproject.utilities.Event
import com.example.graduationproject.utilities.InputValidationState
import com.example.graduationproject.utilities.InputValidator
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpWithEmailAndPasswordUseCase: SignUpWithEmailAndPasswordUseCase,
    private val registerUsingGoogleAccountUseCase : RegisterUsingGoogleAccountUseCase
) : BaseViewModel() {


    private val _signUpUIState = MutableStateFlow(SignUpUiState())
    val signUpUIState = _signUpUIState.asStateFlow()

    private val _signUpUIEvent  = MutableStateFlow<Event<SignUpUiEvent?>>(Event(null))
    val  signUpUIEvent = _signUpUIEvent.asStateFlow()


    fun onClickLogInWithPhone() = _signUpUIEvent.update { Event(SignUpUiEvent.SignUpWithPhoneEvent ) }
    fun onClickLogInWithGoogle() = _signUpUIEvent.update { Event(SignUpUiEvent.SignUpWithGoogleEvent ) }

    fun onClickLogIn() = _signUpUIEvent.update { Event(SignUpUiEvent.LogInEvent ) }

    fun resetUiState() = _signUpUIState.update {it.copy(password = "" , isError = false)}
    fun onClickSignUp() = signUpWithEmailANdPassword()


    private fun signUpWithEmailANdPassword() = viewModelScope.launch {
        _signUpUIState.update { it.copy(isLoading = true) }
        signUpWithEmailAndPasswordUseCase.execute(_signUpUIState.value.email ,_signUpUIState.value.password).collectLatest {state->
            when( state) {

                AuthenticationState.Loading -> _signUpUIState.update { it.copy(isLoading = true) }
                is AuthenticationState.Error -> _signUpUIState.update {
                    it.copy(
                        isLoading = false,
                        error = state.message,
                        isError = true
                    )
                }
                is AuthenticationState.Success -> {
                    _signUpUIState.update { it.copy(isLoading = false, isLogInSuccess = true) }
                    _signUpUIEvent.update { Event(SignUpUiEvent.SignUpEvent) }
                }
            }
        }

    }

    fun signUpWithGoogleAccount(account: GoogleSignInAccount , activity: Activity) = viewModelScope.launch {
        _signUpUIState.update { it.copy(account = account) }
        _signUpUIState.update { it.copy(isLoading = true) }
        registerUsingGoogleAccountUseCase.execute(account).collectLatest {state ->
            when(state) {
                AuthenticationState.Loading -> _signUpUIState.update { it.copy(isLoading = true) }
                is AuthenticationState.Error -> _signUpUIState.update {
                    it.copy(
                        isLoading = false,
                        error = state.message,
                        isError = true
                    )
                }
                is AuthenticationState.Success -> {
                    _signUpUIState.update { it.copy(isLoading = false, isLogInSuccess = true) }
                    _signUpUIEvent.update { Event(SignUpUiEvent.SignUpEvent) }

                    ( activity as MainActivity).binding.mainGroup.visibility = View.VISIBLE
                    ( activity as MainActivity).binding.fragmentContainerView.visibility = View.GONE

                }
            }
        }

    }

    fun onEmailInputChange(text: CharSequence) {
        val emailValidationState = InputValidator.checkEmailValidation(text.toString())
        _signUpUIState.update {
            it.copy(
                email = text.toString() ,
                emailHelperText = if (emailValidationState is InputValidationState.InValid)
                    emailValidationState.message else "" ,
                isEmailValidation = emailValidationState is InputValidationState.InValid ,
                isEmailAndPasswordAreValidation = InputValidator.emailAndPasswordIsValid(
                    _signUpUIState.value.email ,   _signUpUIState.value.password)
            )
        }
    }

    fun onPasswordInputChange(text: CharSequence) {
        val passwordValidationState = InputValidator.checkPasswordValidation(text.toString())
        _signUpUIState.update {
            it.copy(
                password = text.toString() ,
                passwordHelperText = if (passwordValidationState is InputValidationState.InValid)
                    passwordValidationState.message else "" ,
                isPasswordValidation = passwordValidationState is InputValidationState.InValid ,
                isEmailAndPasswordAreValidation = InputValidator.emailAndPasswordIsValid(
                    _signUpUIState.value.email ,   _signUpUIState.value.password)
            )
        }
    }


}