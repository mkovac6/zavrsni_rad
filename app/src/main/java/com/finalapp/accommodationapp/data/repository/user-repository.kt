package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.User
import com.finalapp.accommodationapp.data.UserSession
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class UserRepository {
    companion object {
        private const val TAG = "UserRepository"
    }

    private val supabase = SupabaseClient.client

    suspend fun login(email: String, password: String): User? = withContext(Dispatchers.IO) {
        try {
            val userResult = supabase.from("users")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("user_id", "email", "user_type", "is_profile_complete")) {
                    filter {
                        eq("email", email)
                        eq("password_hash", password) // This filters but doesn't return the password
                    }
                }
                .decodeSingleOrNull<SimpleUserDto>()

            if (userResult == null) {
                Log.d(TAG, "Login failed: Invalid credentials for $email")
                return@withContext null
            }

            // Get the first name based on user type
            val firstName = when (userResult.user_type) {
                "student" -> {
                    try {
                        val student = supabase.from("students")
                            .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("first_name")) {
                                filter {
                                    eq("user_id", userResult.user_id)
                                }
                            }
                            .decodeSingleOrNull<FirstNameDto>()
                        student?.first_name ?: "User"
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching student name: ${e.message}")
                        "User"
                    }
                }
                "landlord" -> {
                    try {
                        val landlord = supabase.from("landlords")
                            .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("first_name")) {
                                filter {
                                    eq("user_id", userResult.user_id)
                                }
                            }
                            .decodeSingleOrNull<FirstNameDto>()
                        landlord?.first_name ?: "User"
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching landlord name: ${e.message}")
                        "User"
                    }
                }
                else -> "User"
            }

            val user = User(
                userId = userResult.user_id,
                email = userResult.email,
                userType = userResult.user_type,
                isProfileComplete = userResult.is_profile_complete,
                firstName = firstName
            )

            Log.d(TAG, "Login successful for: ${user.email} (${user.userType})")
            UserSession.setUser(user)
            user

        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            null
        }
    }

    suspend fun register(email: String, password: String, userType: String = "student"): User? =
        withContext(Dispatchers.IO) {
            try {
                // Check if email exists
                val existing = supabase.from("users")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("user_id")) {
                        filter {
                            eq("email", email)
                        }
                    }
                    .decodeList<UserIdDto>()

                if (existing.isNotEmpty()) {
                    Log.d(TAG, "Registration failed: Email already exists")
                    return@withContext null
                }

                // Insert new user
                val newUser = buildJsonObject {
                    put("email", email)
                    put("password_hash", password) // Should be hashed!
                    put("user_type", userType)
                    put("is_profile_complete", false)
                }

                val insertedUser = supabase.from("users")
                    .insert(newUser) {
                        select(columns = io.github.jan.supabase.postgrest.query.Columns.list("user_id", "email", "user_type", "is_profile_complete"))
                    }
                    .decodeSingle<SimpleUserDto>()

                val user = User(
                    userId = insertedUser.user_id,
                    email = insertedUser.email,
                    userType = insertedUser.user_type,
                    isProfileComplete = insertedUser.is_profile_complete,
                    firstName = ""
                )

                Log.d(TAG, "Registration successful for: $email")
                UserSession.setUser(user)
                user

            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}", e)
                null
            }
        }

    suspend fun checkEmailExists(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("users")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("user_id")) {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeList<UserIdDto>()

            result.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking email: ${e.message}", e)
            false
        }
    }

    suspend fun updateUserProfileStatus(userId: Int, isComplete: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val updates = buildJsonObject {
                    put("is_profile_complete", isComplete)
                }

                supabase.from("users")
                    .update(updates) {
                        filter {
                            eq("user_id", userId)
                        }
                    }

                Log.d(TAG, "Updated profile status for user $userId to $isComplete")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile status: ${e.message}", e)
                false
            }
        }
}

// DTOs for Supabase
@Serializable
data class SimpleUserDto(
    val user_id: Int,
    val email: String,
    val user_type: String,
    val is_profile_complete: Boolean
)

@Serializable
data class UserIdDto(
    val user_id: Int
)

@Serializable
data class FirstNameDto(
    val first_name: String
)