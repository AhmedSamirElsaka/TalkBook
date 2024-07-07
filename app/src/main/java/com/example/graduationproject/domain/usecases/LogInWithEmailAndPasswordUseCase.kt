package com.example.graduationproject.domain.usecases

import com.example.graduationproject.domain.repo.AuthenticationRepo
import javax.inject.Inject


class LogInWithEmailAndPasswordUseCase @Inject constructor(
    private val authenticationRepo: AuthenticationRepo
    )  {
    suspend fun execute(email :String , password : String) = authenticationRepo.logInWithEmailAndPassword(email, password)
}