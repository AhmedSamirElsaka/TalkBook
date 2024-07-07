package com.example.graduationproject.ui.authenticationScreens.logIn


import android.app.Activity
import android.util.Log
import android.view.View
import androidx.lifecycle.viewModelScope
import com.example.graduationproject.domain.entity.AuthenticationState
import com.example.graduationproject.domain.usecases.LogInWithEmailAndPasswordUseCase
import com.example.graduationproject.domain.usecases.RegisterUsingGoogleAccountUseCase
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
class LogInViewModel @Inject constructor(
    private val logInWithEmailAndPasswordUseCase: LogInWithEmailAndPasswordUseCase,
    private val registerUsingGoogleAccountUseCase: RegisterUsingGoogleAccountUseCase
) : BaseViewModel() {

    private val _loginUIState = MutableStateFlow(LogInUiState())
    val loginUIState = _loginUIState.asStateFlow()

    private val _loginUIEvent = MutableStateFlow<Event<LogInUiEvent?>>(Event(null))
    val loginUIEvent = _loginUIEvent.asStateFlow()


    fun onClickForgetPassword() = _loginUIEvent.update { Event(LogInUiEvent.ForgetPasswordEvent) }
    fun onClickLogInWithPhone() = _loginUIEvent.update { Event(LogInUiEvent.LogInWithPhoneEvent) }
    fun onClickLogInWithGoogle() = _loginUIEvent.update { Event(LogInUiEvent.LogInWithGoogleEvent) }

    fun onClickSignUp() = _loginUIEvent.update { Event(LogInUiEvent.SignUpEvent) }

    fun resetEmailAndPasswordAndErrorMessage() =
        _loginUIState.update { it.copy(password = "", isError = false) }

    fun onClickSkip () =  _loginUIEvent.update { Event(LogInUiEvent.SkipUiEvent) }
    fun onClickLogIn() = logInWithEmailANdPassword()


    private fun logInWithEmailANdPassword() = viewModelScope.launch {
        _loginUIState.update { it.copy(isLoading = true) }
        logInWithEmailAndPasswordUseCase.execute(
            _loginUIState.value.email,
            _loginUIState.value.password
        ).collectLatest { state ->
            when (state) {

                AuthenticationState.Loading -> _loginUIState.update { it.copy(isLoading = true) }
                is AuthenticationState.Error -> _loginUIState.update {
                    it.copy(
                        isLoading = false,
                        error = state.message,
                        isError = true
                    )
                }

                is AuthenticationState.Success -> {
                    _loginUIState.update { it.copy(isLoading = false, isLogInSuccess = true) }
                    _loginUIEvent.update { Event(LogInUiEvent.LogInEvent) }
                }
            }
        }

    }

    fun logInGoogleAccount(account: GoogleSignInAccount, activity: Activity) =
        viewModelScope.launch {
            _loginUIState.update { it.copy(account = account) }
            _loginUIState.update { it.copy(isLoading = true) }
            registerUsingGoogleAccountUseCase.execute(account).collectLatest { state ->
                when (state) {
                    AuthenticationState.Loading -> {
                        Log.i("hello", "logInGoogleAccount: loading")

                        _loginUIState.update { it.copy(isLoading = true) }
                    }

                    is AuthenticationState.Error -> {
                        Log.i("hello", "logInGoogleAccount: error")

                        _loginUIState.update {
                            it.copy(
                                isLoading = false,
                                error = state.message,
                                isError = true
                            )
                        }
                    }

                    is AuthenticationState.Success -> {
                        _loginUIState.update { it.copy(isLoading = false, isLogInSuccess = true) }
                        _loginUIEvent.update { Event(LogInUiEvent.LogInEvent) }

                        (activity as MainActivity).binding.mainGroup.visibility = View.VISIBLE
                        (activity as MainActivity).binding.fragmentContainerView.visibility =
                            View.GONE
                        Log.i("hello", "logInGoogleAccount: ")
                    }
                }
            }
        }

    fun onEmailInputChange(text: CharSequence) {
        val emailValidationState = InputValidator.checkEmailValidation(text.toString())
        _loginUIState.update {
            it.copy(
                email = text.toString(),
                emailHelperText = if (emailValidationState is InputValidationState.InValid)
                    emailValidationState.message else "",
                isEmailValidation = emailValidationState is InputValidationState.InValid,
                isEmailAndPasswordAreValidation = InputValidator.emailAndPasswordIsValid(
                    _loginUIState.value.email, _loginUIState.value.password
                )
            )
        }
    }

    fun onPasswordInputChange(text: CharSequence) {
        val passwordValidationState = InputValidator.checkPasswordValidation(text.toString())
        _loginUIState.update {
            it.copy(
                password = text.toString(),
                passwordHelperText = if (passwordValidationState is InputValidationState.InValid)
                    passwordValidationState.message else "",
                isPasswordValidation = passwordValidationState is InputValidationState.InValid,
                isEmailAndPasswordAreValidation = InputValidator.emailAndPasswordIsValid(
                    _loginUIState.value.email, _loginUIState.value.password
                )
            )
        }
    }

}

