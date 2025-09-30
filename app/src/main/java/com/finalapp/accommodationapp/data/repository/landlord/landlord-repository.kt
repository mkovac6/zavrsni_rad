package com.finalapp.accommodationapp.data.repository.landlord

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.landlord.Landlord
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class LandlordRepository {
    companion object {
        private const val TAG = "LandlordRepository"
    }

    private val supabase = SupabaseClient.client

    suspend fun getLandlordByUserId(userId: Int): Landlord? = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("landlords")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<LandlordDto>()

            result?.let { dto ->
                Landlord(
                    landlordId = dto.landlord_id,
                    userId = dto.user_id,
                    firstName = dto.first_name,
                    lastName = dto.last_name,
                    companyName = dto.company_name,
                    phone = dto.phone,
                    isVerified = dto.is_verified ?: false,
                    rating = dto.rating ?: 0.0
                )
            }.also {
                Log.d(TAG, "Loaded landlord for userId: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading landlord", e)
            null
        }
    }

    suspend fun updateLandlordProfile(
        landlordId: Int,
        firstName: String,
        lastName: String,
        companyName: String?,
        phone: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
                put("company_name", companyName)
                put("phone", phone)
            }

            supabase.from("landlords")
                .update(updates) {
                    filter {
                        eq("landlord_id", landlordId)
                    }
                }

            Log.d(TAG, "Updated landlord profile: $landlordId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error updating landlord profile: ${e.message}", e)
            false
        }
    }

    suspend fun createLandlordProfile(
        userId: Int,
        firstName: String,
        lastName: String,
        companyName: String?,
        phone: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if profile already exists
            val existing = supabase.from("landlords")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<LandlordDto>()

            if (existing.isNotEmpty()) {
                Log.d(TAG, "Landlord profile already exists for user $userId")
                return@withContext false
            }

            // Insert new profile
            val newLandlord = buildJsonObject {
                put("user_id", userId)
                put("first_name", firstName)
                put("last_name", lastName)
                put("company_name", companyName)
                put("phone", phone)
                put("is_verified", false)
            }

            supabase.from("landlords")
                .insert(newLandlord)

            // Update user profile completion
            val updateUser = buildJsonObject {
                put("is_profile_complete", true)
            }

            supabase.from("users")
                .update(updateUser) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Landlord profile created successfully for user $userId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error creating landlord profile: ${e.message}", e)
            false
        }
    }
}

// DTO for Supabase
@Serializable
data class LandlordDto(
    val landlord_id: Int,
    val user_id: Int,
    val first_name: String,
    val last_name: String,
    val company_name: String? = null,
    val phone: String,
    val is_verified: Boolean? = false,
    val rating: Double? = null,
    val created_at: String? = null,  // Add this field
    val updated_at: String? = null   // Add this field
)