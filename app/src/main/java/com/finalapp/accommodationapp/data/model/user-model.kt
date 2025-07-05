package com.finalapp.accommodationapp.data.model

data class User(
    val userId: Int,
    val email: String,
    val userType: String,
    val isProfileComplete: Boolean,
    val firstName: String = ""
)