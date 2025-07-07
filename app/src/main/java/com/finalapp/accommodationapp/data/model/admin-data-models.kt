package com.finalapp.accommodationapp.data.model

data class StudentWithUser(
    val studentId: Int,
    val userId: Int,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val studentNumber: String?,
    val yearOfStudy: Int?,
    val program: String?,
    val universityName: String,
    val isProfileComplete: Boolean
)

data class LandlordWithUser(
    val landlordId: Int,
    val userId: Int,
    val email: String,
    val firstName: String,
    val lastName: String,
    val companyName: String?,
    val phone: String,
    val isVerified: Boolean,
    val rating: Double,
    val propertyCount: Int
)