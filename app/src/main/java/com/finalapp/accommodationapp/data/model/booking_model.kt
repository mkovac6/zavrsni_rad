package com.finalapp.accommodationapp.data.model

import java.util.Date

data class Booking(
    val bookingId: Int = 0,
    val propertyId: Int,
    val studentId: Int,
    val startDate: Date,
    val endDate: Date,
    val status: String, // pending, approved, rejected, cancelled, completed
    val totalPrice: Double,
    val messageToLandlord: String? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    // Additional fields for display
    val propertyTitle: String? = null,
    val propertyAddress: String? = null,
    val landlordName: String? = null,
    val studentName: String? = null
)