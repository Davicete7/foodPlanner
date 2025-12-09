package com.example.foodplanner.ui.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.example.foodplanner.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

class GoogleAuthUiClient(private val context: Context) {

    private val oneTapClient = Identity.getSignInClient(context)

    suspend fun signIn(): IntentSender? {
        return try {
            val signInRequest = BeginSignInRequest.Builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.Builder()
                        .setSupported(true)
                        .setServerClientId(context.getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false) // recomendado por Google
                        .build()
                )
                .setAutoSelectEnabled(false) // evita login automático no deseado
                .build()

            val result = oneTapClient.beginSignIn(signInRequest).await()
            result.pendingIntent.intentSender

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    suspend fun getSignInResultFromIntent(intent: Intent): GoogleSignInResult {
        return try {
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken

            if (googleIdToken == null) {
                GoogleSignInResult(false, null, "Google ID token is null")
            } else {
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdToken, null)

                GoogleSignInResult(true, firebaseCredential, null)
            }

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            GoogleSignInResult(false, null, e.message)
        }
    }

    suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }
}

data class GoogleSignInResult(
    val isSuccess: Boolean,
    val credential: AuthCredential? = null,
    val errorMessage: String? = null
)
