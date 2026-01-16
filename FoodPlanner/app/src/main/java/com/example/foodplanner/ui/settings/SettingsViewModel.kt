package com.example.foodplanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.example.foodplanner.utils.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        object Loading : UpdateStatus()
        object Success : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    fun updateUser(uid: String, firstName: String, lastName: String, newPassword: String?) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Loading
            try {
                // Update Firestore
                val updates = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName
                )
                userRepository.updateUser(uid, updates)

                // Update password if provided
                if (newPassword != null) {
                    auth.currentUser?.updatePassword(newPassword)?.await()
                }

                _updateStatus.value = UpdateStatus.Success
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error(e.message ?: "Unknown error")
            }
        }
    }
}