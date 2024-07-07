package com.example.graduationproject.ui.authenticationScreens.resetPassword

sealed interface ForgetPasswordUiEvent {
    object SendResetPassword : ForgetPasswordUiEvent

}