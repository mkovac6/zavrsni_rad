package com.finalapp.accommodationapp.data.repository.admin

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.admin.StudentWithUser
import com.finalapp.accommodationapp.data.model.admin.LandlordWithUser
import com.finalapp.accommodationapp.data.model.admin.Amenity
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import io.github.jan.supabase.postgrest.from
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
            Log.d(TAG, "Fetching all students")

            // First get all students
            val students = supabase.from("students")
                .select()
                .decodeList<StudentDto>()

            Log.d(TAG, "Found ${students.size} students")

            // Get user emails and university names separately
            val userIds = students.map { it.user_id }.distinct()
            val universityIds = students.map { it.university_id }.distinct()

            // Fetch users
            val users = if (userIds.isNotEmpty()) {
                supabase.from("users")
                    .select() {
                        filter {
                            isIn("user_id", userIds)
                        }
                    }
                    .decodeList<UserDto>()
                    .associateBy { it.user_id }
            } else {
                emptyMap()
            }

            // Fetch universities
            val universities = if (universityIds.isNotEmpty()) {
                supabase.from("universities")
                    .select() {
                        filter {
                            isIn("university_id", universityIds)
                        }
                    }
                    .decodeList<UniversityDto>()
                    .associateBy { it.university_id }
            } else {
                emptyMap()
            }

            // Combine the data
            students.map { student ->
                val user = users[student.user_id]
                val university = universities[student.university_id]

                StudentWithUser(
                    studentId = student.student_id,
                    userId = student.user_id,
                    email = user?.email ?: "",
                    firstName = student.first_name,
                    lastName = student.last_name,
                    phone = student.phone,
                    studentNumber = student.student_number,
                    yearOfStudy = student.year_of_study,
                    program = student.program,
                    universityName = university?.name ?: "Unknown University",
                    isProfileComplete = user?.is_profile_complete ?: false
                )
            }.also {
                Log.d(TAG, "Successfully mapped ${it.size} students")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading students: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun deleteStudent(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete from students first
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
            Log.e(TAG, "Error deleting student: ${e.message}", e)
            false
        }
    }

    // Landlord Management
    suspend fun getAllLandlords(): List<LandlordWithUser> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching all landlords")

            // Get all landlords
            val landlords = supabase.from("landlords")
                .select()
                .decodeList<LandlordDto>()

            Log.d(TAG, "Found ${landlords.size} landlords")

            // Get user emails
            val userIds = landlords.map { it.user_id }.distinct()
            val users = if (userIds.isNotEmpty()) {
                supabase.from("users")
                    .select() {
                        filter {
                            isIn("user_id", userIds)
                        }
                    }
                    .decodeList<UserDto>()
                    .associateBy { it.user_id }
            } else {
                emptyMap()
            }

            // Get property counts
            val propertyCountsMap = mutableMapOf<Int, Int>()
            landlords.forEach { landlord ->
                try {
                    val properties = supabase.from("properties")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("property_id")) {
                            filter {
                                eq("landlord_id", landlord.landlord_id)
                            }
                        }
                        .decodeList<PropertyIdOnlyDto>()
                    propertyCountsMap[landlord.landlord_id] = properties.size
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error getting property count for landlord ${landlord.landlord_id}",
                        e
                    )
                    propertyCountsMap[landlord.landlord_id] = 0
                }
            }

            // Combine the data
            landlords.map { landlord ->
                val user = users[landlord.user_id]

                LandlordWithUser(
                    landlordId = landlord.landlord_id,
                    userId = landlord.user_id,
                    email = user?.email ?: "",
                    firstName = landlord.first_name,
                    lastName = landlord.last_name,
                    companyName = landlord.company_name,
                    phone = landlord.phone,
                    isVerified = landlord.is_verified ?: false,
                    rating = landlord.rating ?: 0.0,
                    propertyCount = propertyCountsMap[landlord.landlord_id] ?: 0
                )
            }.also {
                Log.d(TAG, "Successfully mapped ${it.size} landlords")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading landlords: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun deleteLandlord(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get landlord_id first
            val landlord = supabase.from("landlords")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("landlord_id")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<LandlordIdOnlyDto>()

            if (landlord != null) {
                // Delete property amenities for this landlord's properties
                val properties = supabase.from("properties")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("property_id")) {
                        filter {
                            eq("landlord_id", landlord.landlord_id)
                        }
                    }
                    .decodeList<PropertyIdOnlyDto>()

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

            Log.d(TAG, "Deleted landlord with userId: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting landlord: ${e.message}", e)
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

            // Delete favorites
            supabase.from("favorites")
                .delete {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            // Delete bookings
            supabase.from("bookings")
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
            Log.e(TAG, "Error deleting property: ${e.message}", e)
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
            Log.d(TAG, "=== CREATE PROPERTY START ===")
            Log.d(TAG, "Address: $address, City: $city")

            // Get coordinates using geocoding
            Log.d(TAG, "About to call geocodeAddress...")
            val propertyRepo = PropertyRepository()
            val (latitude, longitude) = propertyRepo.geocodeAddress(address, city)

            Log.d(TAG, "Geocoding returned: lat=$latitude, lng=$longitude")

            val newProperty = buildJsonObject {
                put("landlord_id", landlordId)
                put("title", title)
                put("description", description)
                put("property_type", propertyType)
                put("address", address)
                put("city", city)
                put("postal_code", postalCode)
                put("latitude", latitude)
                put("longitude", longitude)
                put("price_per_month", pricePerMonth)
                put("bedrooms", bedrooms)
                put("bathrooms", bathrooms)
                put("total_capacity", totalCapacity)
                put("available_from", availableFrom)
                put("is_active", true)
            }

            Log.d(TAG, "About to insert property into database...")
            val result = supabase.from("properties")
                .insert(newProperty) {
                    select(columns = io.github.jan.supabase.postgrest.query.Columns.list("property_id"))
                }
                .decodeSingle<PropertyIdOnlyDto>()

            Log.d(TAG, "SUCCESS! Created property with ID: ${result.property_id} at coordinates: $latitude, $longitude")
            Log.d(TAG, "=== CREATE PROPERTY END ===")
            result.property_id
        } catch (e: Exception) {
            Log.e(TAG, "ERROR creating property: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            0
        }
    }

    suspend fun addPropertyAmenities(propertyId: Int, amenityIds: List<Int>): Boolean =
        withContext(Dispatchers.IO) {
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
                Log.e(TAG, "Error adding property amenities: ${e.message}", e)
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
            Log.e(TAG, "Error creating landlord profile: ${e.message}", e)
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
                .decodeList<StudentIdOnlyDto>()

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
            Log.e(TAG, "Error deleting university: ${e.message}", e)
            false
        }
    }

    suspend fun addUniversity(name: String, city: String, country: String): Boolean =
        withContext(Dispatchers.IO) {
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
                Log.e(TAG, "Error adding university: ${e.message}", e)
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
            Log.e(TAG, "Error loading amenities: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun updateUserProfileStatus(userId: Int, isComplete: Boolean): Boolean =
        withContext(Dispatchers.IO) {
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
                Log.e(TAG, "Error updating profile status: ${e.message}", e)
                false
            }
        }

    suspend fun debugGetAllStudents(): List<StudentWithUser> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== DEBUG GET ALL STUDENTS ===")

            // First, let's check if we can get raw students
            Log.d(TAG, "Step 1: Getting students from database")
            val students = try {
                supabase.from("students")
                    .select()
                    .decodeList<StudentDto>()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching students: ${e.message}", e)
                emptyList<StudentDto>()
            }

            Log.d(TAG, "Found ${students.size} students in database")
            students.forEach { student ->
                Log.d(TAG, "Student: ${student.first_name} ${student.last_name} (ID: ${student.student_id}, UserID: ${student.user_id})")
            }

            if (students.isEmpty()) {
                Log.d(TAG, "No students found - returning empty list")
                return@withContext emptyList()
            }

            // Step 2: Get user emails
            Log.d(TAG, "Step 2: Getting user emails")
            val userIds = students.map { it.user_id }.distinct()
            Log.d(TAG, "User IDs to fetch: $userIds")

            val users = try {
                if (userIds.isNotEmpty()) {
                    val result = supabase.from("users")
                        .select() {
                            filter {
                                isIn("user_id", userIds)
                            }
                        }
                        .decodeList<UserDto>()
                    Log.d(TAG, "Found ${result.size} users")
                    result.associateBy { it.user_id }
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching users: ${e.message}", e)
                emptyMap<Int, UserDto>()
            }

            // Step 3: Get universities
            Log.d(TAG, "Step 3: Getting universities")
            val universityIds = students.map { it.university_id }.distinct()
            Log.d(TAG, "University IDs to fetch: $universityIds")

            val universities = try {
                if (universityIds.isNotEmpty()) {
                    val result = supabase.from("universities")
                        .select() {
                            filter {
                                isIn("university_id", universityIds)
                            }
                        }
                        .decodeList<UniversityDto>()
                    Log.d(TAG, "Found ${result.size} universities")
                    result.associateBy { it.university_id }
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching universities: ${e.message}", e)
                emptyMap<Int, UniversityDto>()
            }

            // Step 4: Combine the data
            Log.d(TAG, "Step 4: Combining data")
            val result = students.map { student ->
                val user = users[student.user_id]
                val university = universities[student.university_id]

                Log.d(TAG, "Mapping student ${student.student_id}: user=${user?.email}, university=${university?.name}")

                StudentWithUser(
                    studentId = student.student_id,
                    userId = student.user_id,
                    email = user?.email ?: "",
                    firstName = student.first_name,
                    lastName = student.last_name,
                    phone = student.phone,
                    studentNumber = student.student_number,
                    yearOfStudy = student.year_of_study,
                    program = student.program,
                    universityName = university?.name ?: "Unknown University",
                    isProfileComplete = user?.is_profile_complete ?: false
                )
            }

            Log.d(TAG, "Successfully mapped ${result.size} students")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error in debugGetAllStudents: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun debugGetAllLandlords(): List<LandlordWithUser> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== DEBUG GET ALL LANDLORDS ===")

            // Get all landlords
            Log.d(TAG, "Step 1: Getting landlords from database")
            val landlords = try {
                supabase.from("landlords")
                    .select()
                    .decodeList<LandlordDto>()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching landlords: ${e.message}", e)
                emptyList<LandlordDto>()
            }

            Log.d(TAG, "Found ${landlords.size} landlords in database")
            landlords.forEach { landlord ->
                Log.d(TAG, "Landlord: ${landlord.first_name} ${landlord.last_name} (ID: ${landlord.landlord_id}, UserID: ${landlord.user_id})")
            }

            if (landlords.isEmpty()) {
                Log.d(TAG, "No landlords found - returning empty list")
                return@withContext emptyList()
            }

            // Get user emails
            Log.d(TAG, "Step 2: Getting user emails")
            val userIds = landlords.map { it.user_id }.distinct()
            val users = try {
                if (userIds.isNotEmpty()) {
                    supabase.from("users")
                        .select() {
                            filter {
                                isIn("user_id", userIds)
                            }
                        }
                        .decodeList<UserDto>()
                        .associateBy { it.user_id }
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching users: ${e.message}", e)
                emptyMap<Int, UserDto>()
            }

            Log.d(TAG, "Found ${users.size} users for landlords")

            // Get property counts
            Log.d(TAG, "Step 3: Getting property counts")
            val propertyCountsMap = mutableMapOf<Int, Int>()
            landlords.forEach { landlord ->
                try {
                    val properties = supabase.from("properties")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("property_id")) {
                            filter {
                                eq("landlord_id", landlord.landlord_id)
                            }
                        }
                        .decodeList<PropertyIdOnlyDto>()
                    propertyCountsMap[landlord.landlord_id] = properties.size
                    Log.d(TAG, "Landlord ${landlord.landlord_id} has ${properties.size} properties")
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting property count for landlord ${landlord.landlord_id}", e)
                    propertyCountsMap[landlord.landlord_id] = 0
                }
            }

            // Combine the data
            Log.d(TAG, "Step 4: Combining data")
            val result = landlords.map { landlord ->
                val user = users[landlord.user_id]

                LandlordWithUser(
                    landlordId = landlord.landlord_id,
                    userId = landlord.user_id,
                    email = user?.email ?: "",
                    firstName = landlord.first_name,
                    lastName = landlord.last_name,
                    companyName = landlord.company_name,
                    phone = landlord.phone,
                    isVerified = landlord.is_verified ?: false,
                    rating = landlord.rating ?: 0.0,
                    propertyCount = propertyCountsMap[landlord.landlord_id] ?: 0
                )
            }

            Log.d(TAG, "Successfully mapped ${result.size} landlords")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error in debugGetAllLandlords: ${e.message}", e)
            emptyList()
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
            Log.e(TAG, "Error creating landlord with account: ${e.message}", e)
            false
        }
    }

}

// DTOs for Supabase - Updated with all fields
@Serializable
data class StudentDto(
    val student_id: Int,
    val user_id: Int,
    val university_id: Int,
    val first_name: String,
    val last_name: String,
    val phone: String,
    val student_number: String? = null,
    val year_of_study: Int? = null,
    val program: String? = null,
    val profile_picture: String? = null,
    val bio: String? = null,
    val preferred_move_in_date: String? = null,
    val budget_min: Double? = null,
    val budget_max: Double? = null,
    val created_at: String? = null,
    val updated_at: String? = null
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
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class UserDto(
    val user_id: Int,
    val email: String,
    val password_hash: String? = null,
    val salt: String? = null,
    val user_type: String,
    val is_profile_complete: Boolean,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class UniversityDto(
    val university_id: Int,
    val name: String,
    val city: String? = null,
    val country: String? = null,
    val is_active: Boolean? = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class PropertyIdOnlyDto(
    val property_id: Int
)

@Serializable
data class LandlordIdOnlyDto(
    val landlord_id: Int
)

@Serializable
data class StudentIdOnlyDto(
    val student_id: Int
)

@Serializable
data class UserIdDto(
    val user_id: Int
)

@Serializable
data class AmenityDto(
    val amenity_id: Int,
    val name: String,
    val category: String? = null
)