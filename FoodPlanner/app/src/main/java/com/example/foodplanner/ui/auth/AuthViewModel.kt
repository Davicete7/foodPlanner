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
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser != null) {
                        // Try to get the user from Firestore
                        val firestoreUser = userRepository.getUser(currentUser.uid)
                        _user.value = firestoreUser

                        // If the user exists in Auth but not in Firestore (e.g., recently created via Google Sign In),
                        // we wait for onGoogleSignInResult to create it.
                        // We set the state to Authenticated if a Firestore user already exists
                        // to prevent navigation from being blocked.
                        if (firestoreUser != null) {
                            _authState.value = AuthState.Authenticated
                        }
                    } else {
                        _user.value = null
                        _authState.value = AuthState.Unauthenticated
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error getting user: ${e.message}")
                    _authState.value = AuthState.Unauthenticated
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
                        // IMPORTANT: Check if the user already exists to avoid overwriting data
                        val existingUser = userRepository.getUser(firebaseUser.uid)

                        if (existingUser == null) {
                            // New user: create in Firestore
                            val newUser = User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
                            userRepository.createUser(newUser)
                            _user.value = newUser
                        } else {
                            // Existing user: update local state
                            _user.value = existingUser
                        }
                        _authState.value = AuthState.Authenticated
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
                // The AuthStateListener will handle updating the state to Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun register(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                authResult?.user?.let { firebaseUser ->
                    val user = User(
                        uid = firebaseUser.uid, 
                        email = email,
                        firstName = firstName,
                        lastName = lastName
                    )
                    userRepository.createUser(user)
                    _user.value = user
                    _authState.value = AuthState.Authenticated
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
            try {
                googleAuthUiClient.signOut()
                auth.signOut()
                _user.value = null
                _authState.value = AuthState.Unauthenticated
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error logging out", e)
            }
        }
    }

    fun resetState() {
        if (auth.currentUser == null) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}
