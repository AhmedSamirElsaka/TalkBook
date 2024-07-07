package com.example.graduationproject.ui.authenticationScreens.logIn

import com.google.android.gms.auth.api.signin.GoogleSignInAccount


data class LogInUiState(
    val email :String = "",
    val password :String = "",
    val emailHelperText :String = "",
    val passwordHelperText :String = "",
    val isLoading:Boolean = false,
    val isEmailValidation: Boolean = false ,
    val isPasswordValidation: Boolean = false,
    val isEmailAndPasswordAreValidation : Boolean =false ,
    val isError : Boolean =false ,
    val error:String = "",
    val isLogInSuccess:Boolean = false ,
    val account: GoogleSignInAccount?=null
)