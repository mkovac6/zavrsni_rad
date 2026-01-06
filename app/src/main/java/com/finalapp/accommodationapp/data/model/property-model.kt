package com.finalapp.accommodationapp.data.model

import java.util.Date

data class Property(
    val propertyId: Int,
    val title: String,
    val description: String,
    val propertyType: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val latitude: Double,
    val longitude: Double,
    val pricePerMonth: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val totalCapacity: Int,
    val availableFrom: Date?,
    val availableTo: Date?,
    val landlordName: String,
    val landlordPhone: String,
    val landlordRating: Double,
    val companyName: String? = null,
    val isActive: Boolean = true,
    val imageUrls: List<String> = emptyList(),
    val primaryImageUrl: String? = null
)