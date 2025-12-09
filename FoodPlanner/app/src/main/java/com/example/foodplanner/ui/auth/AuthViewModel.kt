package com.example.foodplanner.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.data.User
import com.example.foodplanner.data.UserRepository
import com.example.foodplanner.utils.await
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}

data class GoogleSignInState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    private val _googleSignInState = MutableStateFlow(GoogleSignInState())
    val googleSignInState = _googleSignInState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.signInWithCredential(credential).await()
                authResult?.user?.let { firebaseUser ->
                    val user = User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
                    userRepository.createUser(user) // This will create or update the user
                }
                _googleSignInState.value = GoogleSignInState(isSignInSuccessful = true)
            } catch (e: Exception) {
                _googleSignInState.value = GoogleSignInState(signInError = e.message)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                authResult?.user?.let { firebaseUser ->
                    val user = User(uid = firebaseUser.uid, email = email)
                    userRepository.createUser(user)
                } ?: run {
                    _authState.value = AuthState.Error("User creation failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun resetState() {
        _googleSignInState.value = GoogleSignInState()
        if (auth.currentUser == null) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}