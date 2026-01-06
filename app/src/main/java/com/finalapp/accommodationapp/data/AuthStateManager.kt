package com.finalapp.accommodationapp.data

import com.finalapp.accommodationapp.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized authentication state manager using StateFlow
 * Replaces the UserSession singleton with proper dependency injection
 */
class AuthStateManager {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun setUser(user: User) {
        _currentUser.value = user
    }

    fun getUser(): User? = _currentUser.value

    fun getUserId(): Int = _currentUser.value?.userId ?: 0

    fun clear() {
        _currentUser.value = null
    }

    fun isLoggedIn(): Boolean = _currentUser.value != null
}
