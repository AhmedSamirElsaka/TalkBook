package com.example.graduationproject.domain.usecases

import com.example.graduationproject.domain.repo.AuthenticationRepo
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val authenticationRepo: AuthenticationRepo
    ) {
     suspend fun execute(email:String) = authenticationRepo.resetPassword(email)
}