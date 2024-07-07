package com.example.graduationproject.domain.usecases

import com.example.graduationproject.domain.repo.AuthenticationRepo
import javax.inject.Inject

class SignUpWithEmailAndPasswordUseCase @Inject constructor (
    private val authenticationRepo: AuthenticationRepo
    ) {
    suspend fun execute(email :String, password : String) =
        authenticationRepo.signUpWithEmailAndPasswordUseCase(email, password)
}