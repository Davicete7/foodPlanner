package com.example.foodplanner.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.data.User
import com.example.foodplanner.data.UserRepository
import com.example.foodplanner.utils.await
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

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = application.applicationContext
        )
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            viewModelScope.launch {
                try {
                    if (firebaseAuth.currentUser != null) {
                        val firestoreUser = userRepository.getUser(firebaseAuth.currentUser!!.uid)
                        _user.value = firestoreUser
                        _authState.value = AuthState.Authenticated
                    } else {
                        _user.value = null
                        _authState.value = AuthState.Unauthenticated
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error getting user: ${e.message}")
                    if (firebaseAuth.currentUser != null) {
                        _authState.value = AuthState.Authenticated
                    } else {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            }
        }
    }

    fun onGoogleSignInResult(result: GoogleSignInResult) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (result.isSuccess && result.credential != null) {
                try {
                    val authResult = auth.signInWithCredential(result.credential).await()
                    authResult?.user?.let { firebaseUser ->
                        val user = User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
                        userRepository.createUser(user) // This creates or updates the user
                    }
                } catch (e: Exception) {
                    _authState.value = AuthState.Error(e.message ?: "Unknown error")
                }
            } else {
                _authState.value = AuthState.Error(result.errorMessage ?: "Unknown error")
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
        viewModelScope.launch {
            googleAuthUiClient.signOut()
            auth.signOut()
            _user.value = null
        }
    }

    fun resetState() {
        if (auth.currentUser == null) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}