package com.finalapp.accommodationapp.data.model.student

data class StudentProfile(
    val studentId: Int,
    val userId: Int,
    val universityId: Int,
    val universityName: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val studentNumber: String?,
    val yearOfStudy: Int?,
    val program: String?,
    val budgetMin: Double?,
    val budgetMax: Double?
)