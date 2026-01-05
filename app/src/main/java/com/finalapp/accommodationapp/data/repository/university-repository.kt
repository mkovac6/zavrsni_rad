package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.University
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class UniversityRepository {
    companion object {
        private const val TAG = "UniversityRepository"
    }

    private val supabase = SupabaseClient.client

    /**
     * Fetch university by ID with coordinates
     */
    suspend fun getUniversityById(universityId: Int): University? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching university with ID: $universityId")

            val result = supabase.from("universities")
                .select() {
                    filter {
                        eq("university_id", universityId)
                    }
                }
                .decodeSingleOrNull<UniversityDto>()

            result?.let {
                University(
                    universityId = it.university_id,
                    name = it.name,
                    city = it.city ?: "",
                    country = it.country ?: "Croatia",
                    latitude = it.latitude ?: 0.0,
                    longitude = it.longitude ?: 0.0
                ).also { university ->
                    Log.d(TAG, "Found university: ${university.name} at (${university.latitude}, ${university.longitude})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching university $universityId: ${e.message}", e)
            null
        }
    }

    /**
     * Fetch all active universities
     */
    suspend fun getAllUniversities(): List<University> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching all universities")

            val results = supabase.from("universities")
                .select() {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<UniversityDto>()

            results.map { dto ->
                University(
                    universityId = dto.university_id,
                    name = dto.name,
                    city = dto.city ?: "",
                    country = dto.country ?: "Croatia",
                    latitude = dto.latitude ?: 0.0,
                    longitude = dto.longitude ?: 0.0
                )
            }.also {
                Log.d(TAG, "Loaded ${it.size} universities")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching universities: ${e.message}", e)
            emptyList()
        }
    }
}

// DTO for Supabase
@Serializable
data class UniversityDto(
    val university_id: Int,
    val name: String,
    val city: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_active: Boolean? = true,
    val created_at: String? = null,
    val updated_at: String? = null
)