package com.example.graduationproject.ui.authenticationScreens.phoneAuthentication.phone

sealed interface PhoneUiEvent {

    data class SendVerifactionCode(val phoneNumber : String) : PhoneUiEvent
    data class CodeSent(val verificationId : String) : PhoneUiEvent

}