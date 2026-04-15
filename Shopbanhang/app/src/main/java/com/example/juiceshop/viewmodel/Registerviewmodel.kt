package com.example.juiceshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juiceshop.data.UserRepository
import com.example.juiceshop.model.User
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _registerState = MutableLiveData<RegisterState>()
    val registerState: LiveData<RegisterState> = _registerState

    fun register(email: String, password: String, confirmPassword: String, username: String) {
        when {
            username.isBlank() ->
                _registerState.value = RegisterState.Error("Please enter your username")
            email.isBlank() ->
                _registerState.value = RegisterState.Error("Please enter your email")
            password.isBlank() ->
                _registerState.value = RegisterState.Error("Please enter your password")
            password != confirmPassword ->
                _registerState.value = RegisterState.Error("Passwords do not match")
            password.length < 6 ->
                _registerState.value = RegisterState.Error("Password must be at least 6 characters")
            else -> {
                _registerState.value = RegisterState.Loading
                viewModelScope.launch {
                    val result = userRepository.register(email, password, username)
                    _registerState.value = if (result.isSuccess)
                        RegisterState.Success(result.getOrNull()!!)
                    else
                        RegisterState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
                }
            }
        }
    }

    sealed class RegisterState {
        object Loading : RegisterState()
        data class Success(val user: User) : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}