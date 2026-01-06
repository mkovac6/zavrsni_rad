package com.finalapp.accommodationapp.data.model

data class PropertyImage(
    val imageId: Int,
    val propertyId: Int,
    val imageUrl: String,
    val isPrimary: Boolean = false,
    val displayOrder: Int = 0,
    val uploadedAt: String? = null
)
