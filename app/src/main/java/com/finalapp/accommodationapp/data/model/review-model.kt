package com.finalapp.accommodationapp.data.model

import java.util.Date

data class Review(
    val reviewId: Int = 0,
    val bookingId: Int,
    val propertyId: Int,
    val studentId: Int,
    val landlordId: Int,
    val propertyRating: Int, // 1-5 stars
    val landlordRating: Int, // 1-5 stars
    val comment: String?,
    val createdAt: Date? = null,
    // Additional fields for display
    val propertyTitle: String? = null,
    val studentName: String? = null,
    val studentEmail: String? = null
)
