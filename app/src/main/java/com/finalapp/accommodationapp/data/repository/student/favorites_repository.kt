package com.finalapp.accommodationapp.data.repository.student

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.Property
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*

class FavoritesRepository {
    companion object {
        private const val TAG = "FavoritesRepository"
    }

    private val supabase = SupabaseClient.client
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun addToFavorites(studentId: Int, propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if already exists
            val existing = supabase.from("favorites")
                .select {
                    filter {
                        eq("student_id", studentId)
                        eq("property_id", propertyId)
                    }
                }
                .decodeList<FavoriteDto>()

            if (existing.isNotEmpty()) {
                Log.d(TAG, "Property $propertyId already in favorites for student $studentId")
                return@withContext true
            }

            // Add to favorites
            val newFavorite = buildJsonObject {
                put("student_id", studentId)
                put("property_id", propertyId)
            }

            supabase.from("favorites")
                .insert(newFavorite)

            Log.d(TAG, "Added property $propertyId to favorites for student $studentId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to favorites", e)
            false
        }
    }

    suspend fun removeFromFavorites(studentId: Int, propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.from("favorites")
                .delete {
                    filter {
                        eq("student_id", studentId)
                        eq("property_id", propertyId)
                    }
                }

            Log.d(TAG, "Removed property $propertyId from favorites for student $studentId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from favorites", e)
            false
        }
    }

    suspend fun isFavorite(studentId: Int, propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("favorites")
                .select {
                    filter {
                        eq("student_id", studentId)
                        eq("property_id", propertyId)
                    }
                }
                .decodeList<FavoriteDto>()

            result.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking favorite status", e)
            false
        }
    }

    suspend fun getStudentFavorites(studentId: Int): List<Property> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading favorites for student: $studentId")

            // First get favorite property IDs
            val favorites = supabase.from("favorites")
                .select() {
                    filter {
                        eq("student_id", studentId)
                    }
                }
                .decodeList<FavoriteDto>()

            val propertyIds = favorites.map { it.property_id }

            if (propertyIds.isEmpty()) {
                Log.d(TAG, "No favorites found for student $studentId")
                return@withContext emptyList()
            }

            // Fetch properties for those IDs
            val properties = supabase.from("properties")
                .select() {
                    filter {
                        isIn("property_id", propertyIds)
                        eq("is_active", true)
                    }
                }
                .decodeList<SimpleFavoritePropertyDto>()

            // Map to Property objects
            properties.map { dto ->
                Property(
                    propertyId = dto.property_id,
                    title = dto.title,
                    description = dto.description ?: "",
                    propertyType = dto.property_type,
                    address = dto.address,
                    city = dto.city,
                    postalCode = dto.postal_code ?: "",
                    latitude = dto.latitude ?: 0.0,
                    longitude = dto.longitude ?: 0.0,
                    pricePerMonth = dto.price_per_month,
                    bedrooms = dto.bedrooms,
                    bathrooms = dto.bathrooms,
                    totalCapacity = dto.total_capacity,
                    availableFrom = dto.available_from?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    availableTo = dto.available_to?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    isActive = dto.is_active ?: true,
                    // We'll skip landlord details for now to keep it simple
                    landlordName = "",
                    landlordPhone = "",
                    landlordRating = 0.0
                )
            }.also {
                Log.d(TAG, "Loaded ${it.size} favorite properties for student $studentId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading favorite properties: ${e.message}", e)
            emptyList()
        }
    }
}

// DTOs for Supabase
@Serializable
data class FavoriteDto(
    val student_id: Int,
    val property_id: Int,
    val created_at: String? = null
)

@Serializable
data class SimpleFavoritePropertyDto(
    val property_id: Int,
    val landlord_id: Int,
    val title: String,
    val description: String? = null,
    val property_type: String,
    val address: String,
    val city: String,
    val postal_code: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val price_per_month: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val total_capacity: Int,
    val available_from: String? = null,
    val available_to: String? = null,
    val is_active: Boolean? = true
)

@Serializable
data class FavoriteWithPropertyDto(
    val student_id: Int,
    val property_id: Int,
    val created_at: String? = null,
    val properties: PropertyWithLandlordDto? = null
)

@Serializable
data class PropertyWithLandlordDto(
    val property_id: Int,
    val landlord_id: Int,
    val title: String,
    val description: String? = null,
    val property_type: String,
    val address: String,
    val city: String,
    val postal_code: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val price_per_month: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val total_capacity: Int,
    val available_from: String? = null,
    val available_to: String? = null,
    val is_active: Boolean? = true,
    val landlords: LandlordBasicDto? = null
)

@Serializable
data class LandlordBasicDto(
    val first_name: String,
    val last_name: String,
    val phone: String,
    val rating: Double? = null
)