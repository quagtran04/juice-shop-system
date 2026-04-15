package com.example.juiceshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juiceshop.data.UserRepository
import com.example.juiceshop.model.User
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, password: String) {
        when {
            email.isBlank() ->
                _loginState.value = LoginState.Error("Please enter your email")
            password.isBlank() ->
                _loginState.value = LoginState.Error("Please enter your password")
            else -> {
                _loginState.value = LoginState.Loading
                viewModelScope.launch {
                    val result = userRepository.login(email, password)
                    _loginState.value = if (result.isSuccess)
                        LoginState.Success(result.getOrNull()!!)
                    else
                        LoginState.Error(result.exceptionOrNull()?.message ?: "Login failed")
                }
            }
        }
    }

    sealed class LoginState {
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}