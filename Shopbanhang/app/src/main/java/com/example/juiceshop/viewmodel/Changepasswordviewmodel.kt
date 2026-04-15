package com.example.juiceshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juiceshop.data.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangePasswordViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _changePasswordState = MutableLiveData<ChangePasswordState>()
    val changePasswordState: LiveData<ChangePasswordState> = _changePasswordState

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        when {
            currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() ->
                _changePasswordState.value = ChangePasswordState.Error("Please fill in all fields")
            newPassword != confirmPassword ->
                _changePasswordState.value = ChangePasswordState.Error("New passwords do not match")
            newPassword.length < 6 ->
                _changePasswordState.value = ChangePasswordState.Error("Password must be at least 6 characters")
            currentPassword == newPassword ->
                _changePasswordState.value = ChangePasswordState.Error("New password must be different from current password")
            else -> {
                _changePasswordState.value = ChangePasswordState.Loading
                viewModelScope.launch {
                    try {
                        val user = auth.currentUser
                            ?: throw Exception("User not logged in")

                        // Re-authenticate trước khi đổi mật khẩu (yêu cầu của Firebase)
                        val credential = EmailAuthProvider.getCredential(
                            user.email ?: "", currentPassword
                        )
                        user.reauthenticate(credential).await()

                        // Đổi mật khẩu
                        val success = userRepository.changePassword(newPassword)
                        _changePasswordState.value = if (success)
                            ChangePasswordState.Success
                        else
                            ChangePasswordState.Error("Failed to change password")

                    } catch (e: Exception) {
                        _changePasswordState.value = ChangePasswordState.Error(
                            when {
                                e.message?.contains("password is invalid") == true ->
                                    "Current password is incorrect"
                                else -> e.message ?: "An error occurred"
                            }
                        )
                    }
                }
            }
        }
    }

    sealed class ChangePasswordState {
        object Loading : ChangePasswordState()
        object Success : ChangePasswordState()
        data class Error(val message: String) : ChangePasswordState()
    }
}