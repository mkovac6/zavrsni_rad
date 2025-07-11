package com.finalapp.accommodationapp.data

import com.finalapp.accommodationapp.data.model.User

// Simple in-memory session storage
object UserSession {
    var currentUser: User? = null

    fun setUser(user: User) {
        currentUser = user
    }

    fun getUser(): User? = currentUser

    fun getUserId(): Int = currentUser?.userId ?: 0

    fun clear() {
        currentUser = null
    }

    fun isLoggedIn(): Boolean = currentUser != null
}