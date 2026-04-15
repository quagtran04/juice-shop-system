package com.example.juiceshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juiceshop.data.UserRepository
import com.example.juiceshop.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _updateState = MutableLiveData<UpdateState?>()
    val updateState: LiveData<UpdateState?> = _updateState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadProfile()
    }

    fun loadProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _user.value = userRepository.getUserById(uid)
            _isLoading.value = false
        }
    }

    fun updateProfile(phone: String, address: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (phone.isBlank() || address.isBlank()) {
            _updateState.value = UpdateState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val success = userRepository.updateProfile(uid, phone, address)
            _updateState.value = if (success)
                UpdateState.Success
            else
                UpdateState.Error("Update failed")
            _isLoading.value = false
        }
    }

    fun logout() {
        userRepository.logout()
    }

    sealed class UpdateState {
        object Success : UpdateState()
        data class Error(val message: String) : UpdateState()
    }
}