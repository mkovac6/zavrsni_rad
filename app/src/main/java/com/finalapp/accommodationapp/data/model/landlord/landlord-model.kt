package com.finalapp.accommodationapp.data.model.landlord

data class Landlord(
    val landlordId: Int,
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val companyName: String?,
    val phone: String,
    val isVerified: Boolean,
    val rating: Double
)