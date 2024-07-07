package com.example.graduationproject.domain.entity

sealed class AuthenticationState {
    object Loading : AuthenticationState()
    data class Success( val message: String) : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()

}

