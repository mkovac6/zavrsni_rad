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

    suspend fun getAllUniversities(): List<University> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("universities")
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<UniversityDto>()

            result.map { dto ->
                University(
                    universityId = dto.university_id,
                    name = dto.name,
                    city = dto.city ?: "",
                    country = dto.country ?: ""
                )
            }.sortedBy { it.name }.also {
                Log.d(TAG, "Loaded ${it.size} universities")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching universities: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getUniversityById(id: Int): University? = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("universities")
                .select {
                    filter {
                        eq("university_id", id)
                    }
                }
                .decodeSingleOrNull<UniversityDto>()

            result?.let { dto ->
                University(
                    universityId = dto.university_id,
                    name = dto.name,
                    city = dto.city ?: "",
                    country = dto.country ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching university by id: ${e.message}", e)
            null
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
    val is_active: Boolean? = true
)