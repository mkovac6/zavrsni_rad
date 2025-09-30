package com.finalapp.accommodationapp.data.repository.student

import android.annotation.SuppressLint
import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.student.StudentProfile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class StudentRepository {
    companion object {
        private const val TAG = "StudentRepository"
    }

    private val supabase = SupabaseClient.client

    suspend fun createStudentProfile(
        userId: Int,
        universityId: Int,
        firstName: String,
        lastName: String,
        phone: String,
        studentNumber: String?,
        yearOfStudy: Int?,
        program: String?,
        preferredMoveInDate: String?,
        budgetMin: Double?,
        budgetMax: Double?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if profile exists - use user_id not student_id since we're checking by user
            val existing = supabase.from("students")
                .select {
                    filter {
                        eq("user_id", userId)  // Fixed: use userId, not studentId
                    }
                }
                .decodeList<StudentDto>()  // Fixed: added decodeList

            if (existing.isNotEmpty()) {
                Log.d(TAG, "Student profile already exists for user $userId")
                return@withContext false
            }

            // Insert new profile
            val newStudent = buildJsonObject {
                put("user_id", userId)
                put("university_id", universityId)
                put("first_name", firstName)
                put("last_name", lastName)
                put("phone", phone)
                studentNumber?.let { put("student_number", it) }
                yearOfStudy?.let { put("year_of_study", it) }
                program?.let { put("program", it) }
                preferredMoveInDate?.let { put("preferred_move_in_date", it) }
                budgetMin?.let { put("budget_min", it) }
                budgetMax?.let { put("budget_max", it) }
            }

            supabase.from("students")
                .insert(newStudent)

            // Update user profile completion
            supabase.from("users")
                .update(
                    buildJsonObject { put("is_profile_complete", true) }
                ) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Student profile created successfully for user $userId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error creating student profile: ${e.message}", e)
            false
        }
    }

    suspend fun getStudentProfile(userId: Int): StudentProfile? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching student profile for userId: $userId")

            // First get the student record without joins
            val result = supabase.from("students")
                .select() {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<StudentDto>()

            if (result != null) {
                // Get university name separately if needed
                val universityName = if (result.university_id > 0) {
                    try {
                        val uni = supabase.from("universities")
                            .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("name")) {
                                filter {
                                    eq("university_id", result.university_id)
                                }
                            }
                            .decodeSingleOrNull<UniversityNameDto>()
                        uni?.name ?: "Unknown University"
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching university name: ${e.message}")
                        "Unknown University"
                    }
                } else {
                    "Unknown University"
                }

                Log.d(TAG, "Found student profile: studentId=${result.student_id}, userId=${result.user_id}")

                StudentProfile(
                    studentId = result.student_id,
                    userId = result.user_id,
                    universityId = result.university_id,
                    universityName = universityName,
                    firstName = result.first_name,
                    lastName = result.last_name,
                    phone = result.phone,
                    studentNumber = result.student_number,
                    yearOfStudy = result.year_of_study ?: 0,
                    program = result.program,
                    budgetMin = result.budget_min ?: 0.0,
                    budgetMax = result.budget_max ?: 0.0
                )
            } else {
                Log.e(TAG, "No student profile found for userId: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching student profile: ${e.message}", e)
            null
        }
    }

    suspend fun updateStudentProfile(
        studentId: Int,
        firstName: String,
        lastName: String,
        phone: String,
        studentNumber: String?,
        yearOfStudy: Int?,
        program: String?,
        budgetMin: Double?,
        budgetMax: Double?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
                put("phone", phone)
                studentNumber?.let { put("student_number", it) }
                yearOfStudy?.let { put("year_of_study", it) }
                program?.let { put("program", it) }
                budgetMin?.let { put("budget_min", it) }
                budgetMax?.let { put("budget_max", it) }
            }

            supabase.from("students")
                .update(updates) {
                    filter {
                        eq("student_id", studentId)
                    }
                }

            Log.d(TAG, "Updated student profile: $studentId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error updating student profile: ${e.message}", e)
            false
        }
    }
}
// DTOs for Supabase
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
    val profile_picture: String? = null,  // Add this field
    val bio: String? = null,  // Add this field
    val preferred_move_in_date: String? = null,  // Add this field if it exists
    val budget_min: Double? = null,
    val budget_max: Double? = null,
    val created_at: String? = null,  // Add these timestamp fields
    val updated_at: String? = null
)

@Serializable
data class UniversityNameDto(
    val name: String
)

@Serializable
data class StudentProfileDto(
    val student_id: Int,
    val user_id: Int,
    val university_id: Int,
    val first_name: String,
    val last_name: String,
    val phone: String,
    val student_number: String? = null,
    val year_of_study: Int? = null,
    val program: String? = null,
    val budget_min: Double? = null,
    val budget_max: Double? = null,
    val universities: UniversityRelation
) {
    @Serializable
    data class UniversityRelation(val name: String)

    fun toStudentProfile() = StudentProfile(
        studentId = student_id,
        userId = user_id,
        universityId = university_id,
        universityName = universities.name,
        firstName = first_name,
        lastName = last_name,
        phone = phone,
        studentNumber = student_number,
        yearOfStudy = year_of_study ?: 0,
        program = program,
        budgetMin = budget_min ?: 0.0,
        budgetMax = budget_max ?: 0.0
    )
}