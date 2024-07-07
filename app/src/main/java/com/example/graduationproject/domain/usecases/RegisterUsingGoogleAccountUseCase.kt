package com.example.graduationproject.domain.usecases

import com.example.graduationproject.domain.repo.AuthenticationRepo
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import javax.inject.Inject


class RegisterUsingGoogleAccountUseCase @Inject constructor (
    private val authenticationRepo: AuthenticationRepo
) {
    suspend fun execute(account: GoogleSignInAccount) =
        authenticationRepo.registerUsingGoogleAccount(account)
}