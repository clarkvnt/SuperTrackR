package com.fittracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        _uiState.value = LoginUiState(isLoading = true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = LoginUiState()
                    onSuccess()
                } else {
                    _uiState.value = LoginUiState(error = task.exception?.message ?: "Login failed")
                }
            }
    }

    fun register(email: String, password: String, onSuccess: () -> Unit) {
        _uiState.value = LoginUiState(isLoading = true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = LoginUiState()
                    onSuccess()
                } else {
                    _uiState.value = LoginUiState(error = task.exception?.message ?: "Registration failed")
                }
            }
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
) 