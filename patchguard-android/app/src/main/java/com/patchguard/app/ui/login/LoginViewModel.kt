package com.patchguard.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patchguard.app.data.CredentialRepository
import com.patchguard.app.network.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val authService: AuthService,
    private val credentialRepository: CredentialRepository,
    private val onTokenReceived: (String) -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            runCatching { authService.login(email, password) }
                .onSuccess { token ->
                    credentialRepository.save(email, password)
                    onTokenReceived(token)
                }
                .onFailure {
                    _uiState.value = LoginUiState.Error("Invalid email or password.")
                }
        }
    }
}
