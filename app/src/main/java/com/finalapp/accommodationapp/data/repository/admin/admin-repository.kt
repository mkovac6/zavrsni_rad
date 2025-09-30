package com.finalapp.accommodationapp.data.repository.admin

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.admin.StudentWithUser
import com.finalapp.accommodationapp.data.model.admin.LandlordWithUser
import com.finalapp.accommodationapp.data.model.admin.Amenity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AdminRepository {
    companion object {
        private const val TAG = "AdminRepository"
    }

    private val supabase = SupabaseClient.client

    // Student Management
    suspend fun getAllStudents(): List<StudentWithUser> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("students")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("""
                    *,
                    users!inner(email, is_profile_complete),
                    universities!inner(name)
                """.trimIndent()))
                .decodeList<StudentDto>()

            result.map { dto ->
                StudentWithUser(
                    studentId = dto.student_id,
                    userId = dto.user_id,
                    email = dto.users?.email ?: "",
                    firstName = dto.first_name,
                    lastName = dto.last_name,
                    phone = dto.phone,
                    studentNumber = dto.student_number,
                    yearOfStudy = dto.year_of_study ?: 0,
                    program = dto.program,
                    universityName = dto.universities?.name ?: "",
                    isProfileComplete = dto.users?.is_profile_complete ?: false
                )
            }.also {
                Log.d(TAG, "Loaded ${it.size} students")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading students", e)
            emptyList()
        }
    }

    suspend fun deleteStudent(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete from students first (cascade should handle this)
            supabase.from("students")
                .delete {
                    filter {
                        eq("user_id", userId)
                    }
                }

            // Delete from users
            supabase.from("users")
                .delete {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Deleted student with userId: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting student", e)
            false
        }
    }

    // Landlord Management
    suspend fun getAllLandlords(): List<LandlordWithUser> = withContext(Dispatchers.IO) {
        try {
            val landlords = supabase.from("landlords")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("""
                    *,
                    users!inner(email, is_profile_complete)
                """.trimIndent()))
                .decodeList<LandlordDto>()

            // Get property counts separately
            val propertyCountsMap = mutableMapOf<Int, Int>()
            landlords.forEach { landlord ->
                val count = supabase.from("properties")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("property_id")) {
                        filter {
                            eq("landlord_id", landlord.landlord_id)
                        }
                    }
                    .decodeList<PropertyIdDto>()
                    .size
                propertyCountsMap[landlord.landlord_id] = count
            }

            landlords.map { dto ->
                LandlordWithUser(
                    landlordId = dto.landlord_id,
                    userId = dto.user_id,
                    email = dto.users?.email ?: "",
                    firstName = dto.first_name,
                    lastName = dto.last_name,
                    companyName = dto.company_name,
                    phone = dto.phone,
                    isVerified = dto.is_verified ?: false,
                    rating = dto.rating ?: 0.0,
                    propertyCount = propertyCountsMap[dto.landlord_id] ?: 0
                )
            }.also {
                Log.d(TAG, "Loaded ${it.size} landlords")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading landlords", e)
            emptyList()
        }
    }

    suspend fun deleteLandlord(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get landlord_id first
            val landlordResult = supabase.from("landlords")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("landlord_id")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<LandlordIdDto>()

            landlordResult?.let { landlord ->
                // Delete property amenities for this landlord's properties
                val properties = supabase.from("properties")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("property_id")) {
                        filter {
                            eq("landlord_id", landlord.landlord_id)
                        }
                    }
                    .decodeList<PropertyIdDto>()

                properties.forEach { property ->
                    supabase.from("propertyamenities")
                        .delete {
                            filter {
                                eq("property_id", property.property_id)
                            }
                        }
                }

                // Delete properties
                supabase.from("properties")
                    .delete {
                        filter {
                            eq("landlord_id", landlord.landlord_id)
                        }
                    }

                // Delete landlord
                supabase.from("landlords")
                    .delete {
                        filter {
                            eq("user_id", userId)
                        }
                    }

                // Delete user
                supabase.from("users")
                    .delete {
                        filter {
                            eq("user_id", userId)
                        }
                    }
            }

            Log.d(TAG, "Deleted landlord with userId: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting landlord", e)
            false
        }
    }

    suspend fun createLandlordProfile(
        userId: Int,
        firstName: String,
        lastName: String,
        companyName: String?,
        phone: String,
        isVerified: Boolean,
        rating: Double = 0.0
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val newLandlord = buildJsonObject {
                put("user_id", userId)
                put("first_name", firstName)
                put("last_name", lastName)
                put("company_name", companyName)
                put("phone", phone)
                put("is_verified", isVerified)
                put("rating", rating)
            }

            supabase.from("landlords")
                .insert(newLandlord)

            Log.d(TAG, "Created landlord profile for userId: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating landlord profile", e)
            false
        }
    }

    // Property Management
    suspend fun deleteProperty(propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete amenities first
            supabase.from("propertyamenities")
                .delete {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            // Delete property
            supabase.from("properties")
                .delete {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            Log.d(TAG, "Deleted property with id: $propertyId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting property", e)
            false
        }
    }

    suspend fun createProperty(
        landlordId: Int,
        title: String,
        description: String,
        propertyType: String,
        address: String,
        city: String,
        postalCode: String,
        pricePerMonth: Double,
        bedrooms: Int,
        bathrooms: Int,
        totalCapacity: Int,
        availableFrom: String
    ): Int = withContext(Dispatchers.IO) {
        try {
            val newProperty = buildJsonObject {
                put("landlord_id", landlordId)
                put("title", title)
                put("description", description)
                put("property_type", propertyType)
                put("address", address)
                put("city", city)
                put("postal_code", postalCode)
                put("price_per_month", pricePerMonth)
                put("bedrooms", bedrooms)
                put("bathrooms", bathrooms)
                put("total_capacity", totalCapacity)
                put("available_from", availableFrom)
                put("is_active", true)
            }

            val result = supabase.from("properties")
                .insert(newProperty) {
                    select()
                }
                .decodeSingle<PropertyIdDto>()

            Log.d(TAG, "Created property with ID: ${result.property_id}")
            result.property_id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating property", e)
            0
        }
    }

    suspend fun addPropertyAmenities(propertyId: Int, amenityIds: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            val amenities = amenityIds.map { amenityId ->
                buildJsonObject {
                    put("property_id", propertyId)
                    put("amenity_id", amenityId)
                }
            }

            supabase.from("propertyamenities")
                .insert(amenities)

            Log.d(TAG, "Added ${amenityIds.size} amenities to property $propertyId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding property amenities", e)
            false
        }
    }

    // University Management
    suspend fun deleteUniversity(universityId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if any students are enrolled
            val students = supabase.from("students")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("student_id")) {
                    filter {
                        eq("university_id", universityId)
                    }
                }
                .decodeList<StudentIdDto>()

            if (students.isNotEmpty()) {
                Log.w(TAG, "Cannot delete university with enrolled students")
                return@withContext false
            }

            // Safe to delete
            supabase.from("universities")
                .delete {
                    filter {
                        eq("university_id", universityId)
                    }
                }

            Log.d(TAG, "Deleted university with id: $universityId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting university", e)
            false
        }
    }

    suspend fun addUniversity(name: String, city: String, country: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val newUniversity = buildJsonObject {
                put("name", name)
                put("city", city)
                put("country", country)
                put("is_active", true)
            }

            supabase.from("universities")
                .insert(newUniversity)

            Log.d(TAG, "Added new university: $name")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding university", e)
            false
        }
    }

    // Utility functions
    suspend fun updateUserProfileStatus(userId: Int, isComplete: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val update = buildJsonObject {
                put("is_profile_complete", isComplete)
            }

            supabase.from("users")
                .update(update) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Updated profile status for userId: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile status", e)
            false
        }
    }

    suspend fun createLandlordWithAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        companyName: String?,
        phone: String,
        isVerified: Boolean,
        rating: Double = 0.0
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Create user account
            val newUser = buildJsonObject {
                put("email", email)
                put("password_hash", password) // Should be hashed!
                put("user_type", "landlord")
                put("is_profile_complete", true)
            }

            val userResult = supabase.from("users")
                .insert(newUser) {
                    select()
                }
                .decodeSingle<UserIdDto>()

            // Create landlord profile
            val newLandlord = buildJsonObject {
                put("user_id", userResult.user_id)
                put("first_name", firstName)
                put("last_name", lastName)
                put("company_name", companyName)
                put("phone", phone)
                put("is_verified", isVerified)
                put("rating", rating)
            }

            supabase.from("landlords")
                .insert(newLandlord)

            Log.d(TAG, "Created landlord with userId: ${userResult.user_id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating landlord with account", e)
            false
        }
    }

    suspend fun getAllAmenities(): List<Amenity> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("amenities")
                .select()
                .decodeList<AmenityDto>()

            result.map { dto ->
                Amenity(
                    amenityId = dto.amenity_id,
                    name = dto.name,
                    category = dto.category
                )
            }.sortedBy { it.name }.also {
                Log.d(TAG, "Loaded ${it.size} amenities")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading amenities", e)
            emptyList()
        }
    }
}

// DTOs for Supabase
@Serializable
data class StudentDto(
    val student_id: Int,
    val user_id: Int,
    val first_name: String,
    val last_name: String,
    val phone: String,
    val student_number: String? = null,
    val year_of_study: Int? = null,
    val program: String? = null,
    val users: UserInfoDto? = null,
    val universities: UniversityNameDto? = null
)

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
    val users: UserInfoDto? = null
)

@Serializable
data class UserInfoDto(
    val email: String,
    val is_profile_complete: Boolean
)

@Serializable
data class UniversityNameDto(val name: String)

@Serializable
data class PropertyIdDto(
    val property_id: Int,
    val landlord_id: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val property_type: String? = null,
    val address: String? = null,
    val city: String? = null,
    val postal_code: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val price_per_month: Double? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val total_capacity: Int? = null,
    val available_from: String? = null,
    val available_to: String? = null,
    val is_active: Boolean? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class LandlordIdDto(val landlord_id: Int)

@Serializable
data class StudentIdDto(val student_id: Int)

@Serializable
data class UserIdDto(val user_id: Int)

@Serializable
data class AmenityDto(
    val amenity_id: Int,
    val name: String,
    val category: String? = null
)