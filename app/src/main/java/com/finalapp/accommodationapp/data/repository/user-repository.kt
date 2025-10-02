package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.data.model.User
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

class UserRepository {
    companion object {
        private const val TAG = "UserRepository"
    }

    private val supabase = SupabaseClient.client

    /**
     * Hash password with SHA-256 and salt
     * For production, consider using BCrypt library instead
     */
    private fun hashPassword(password: String, salt: String = generateSalt()): Pair<String, String> {
        val combined = password + salt
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(combined.toByteArray())
        val hash = bytes.fold("") { str, it -> str + "%02x".format(it) }
        return Pair(hash, salt)
    }

    /**
     * Generate a random salt
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Verify password against hash
     */
    private fun verifyPassword(password: String, hash: String, salt: String): Boolean {
        val (computedHash, _) = hashPassword(password, salt)
        return computedHash == hash
    }

    /**
     * Simple hash for legacy/migration purposes
     * Use this temporarily for existing plain text passwords
     */
    private fun simpleHash(password: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun testSupabaseConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing Supabase connection...")

            val result = supabase.from("users")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("user_id")) {
                    limit(1)
                }
                .decodeList<UserIdTestDto>()

            Log.d(TAG, "Supabase connection successful. Found ${result.size} users")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Supabase connection failed: ${e.message}", e)
            false
        }
    }

    suspend fun debugLogin(email: String, password: String): User? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== DEBUG LOGIN START ===")
            Log.d(TAG, "Attempting login for email: $email")

            // First, let's try to get ANY user to see if the query works
            val allUsers = try {
                supabase.from("users")
                    .select()
                    .decodeList<SimpleUserDto>()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching all users: ${e.message}", e)
                emptyList<SimpleUserDto>()
            }

            Log.d(TAG, "Total users in database: ${allUsers.size}")
            allUsers.forEach { user ->
                Log.d(TAG, "Found user: ${user.email} (type: ${user.user_type})")
            }

            // Now try to get the specific user
            val userResult = try {
                supabase.from("users")
                    .select() {
                        filter {
                            eq("email", email)
                        }
                    }
                    .decodeSingleOrNull<SimpleUserDto>()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user by email: ${e.message}", e)
                null
            }

            if (userResult == null) {
                Log.d(TAG, "No user found with email: $email")
                // Let's check if it's a case sensitivity issue
                val lowerCaseResult = supabase.from("users")
                    .select() {
                        filter {
                            ilike("email", email) // case insensitive
                        }
                    }
                    .decodeSingleOrNull<SimpleUserDto>()

                if (lowerCaseResult != null) {
                    Log.d(TAG, "Found user with case-insensitive search: ${lowerCaseResult.email}")
                }
                return@withContext null
            }

            Log.d(TAG, "User found: ${userResult.email}, checking password...")
            Log.d(TAG, "Password from DB: ${userResult.password_hash}")
            Log.d(TAG, "Password entered: $password")

            // Check password
            val passwordValid = when {
                // Check if it's the hashed admin password
                userResult.email == "admin@accommodation.com" -> {
                    val hashedInput = simpleHash(password)
                    Log.d(TAG, "Admin password hash comparison:")
                    Log.d(TAG, "Input hashed: $hashedInput")
                    Log.d(TAG, "Stored hash: ${userResult.password_hash}")
                    hashedInput == userResult.password_hash
                }
                // For other users, check plain text (for now)
                else -> {
                    Log.d(TAG, "Checking plain text password")
                    password == userResult.password_hash
                }
            }

            Log.d(TAG, "Password valid: $passwordValid")

            if (passwordValid) {
                Log.d(TAG, "Login successful for: $email (${userResult.user_type})")
                User(
                    userId = userResult.user_id,
                    email = userResult.email,
                    userType = userResult.user_type,
                    isProfileComplete = userResult.is_profile_complete
                )
            } else {
                Log.d(TAG, "Invalid password for: $email")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            Log.e(TAG, "Stack trace:", e)
            null
        }
    }

    suspend fun login(email: String, password: String): User? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting login for: $email")

            // First, get the user by email to check their password
            val userResult = supabase.from("users")
                .select() {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeSingleOrNull<UserWithSaltDto>()

            if (userResult == null) {
                Log.d(TAG, "No user found with email: $email")
                return@withContext null
            }

            // Check password based on whether salt exists
            val passwordValid = if (userResult.salt != null) {
                // User has salt, use proper verification
                verifyPassword(password, userResult.password_hash, userResult.salt)
            } else {
                // Legacy check - for migration period
                // First try simple hash
                val hashedPassword = simpleHash(password)
                if (hashedPassword == userResult.password_hash) {
                    true
                } else {
                    // Try plain text (for initial admin setup)
                    password == userResult.password_hash
                }
            }

            if (passwordValid) {
                Log.d(TAG, "Login successful for: $email (${userResult.user_type})")

                // If using plain text, update to hashed password
                if (userResult.salt == null && password == userResult.password_hash) {
                    updatePasswordToHashed(userResult.user_id, password)
                }

                User(
                    userId = userResult.user_id,
                    email = userResult.email,
                    userType = userResult.user_type,
                    isProfileComplete = userResult.is_profile_complete
                )
            } else {
                Log.d(TAG, "Invalid password for: $email")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            null
        }
    }

    suspend fun register(email: String, password: String, userType: String): User? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Registering new user: $email as $userType")

            // Hash password with salt
            val (hash, salt) = hashPassword(password)

            val newUser = buildJsonObject {
                put("email", email)
                put("password_hash", hash)
                put("salt", salt)
                put("user_type", userType)
                put("is_profile_complete", false)
            }

            val result = supabase.from("users")
                .insert(newUser) {
                    select()
                }
                .decodeSingle<UserDto>()

            Log.d(TAG, "User registered successfully: ${result.email}")

            User(
                userId = result.user_id,
                email = result.email,
                userType = result.user_type,
                isProfileComplete = result.is_profile_complete
            )
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
                .decodeList<UserIdOnlyDto>()

            result.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking email: ${e.message}", e)
            false
        }
    }

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
            Log.e(TAG, "Error updating profile status: ${e.message}", e)
            false
        }
    }

    /**
     * Update plain text password to hashed password
     * Used for migrating existing plain text passwords
     */
    private suspend fun updatePasswordToHashed(userId: Int, plainPassword: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (hash, salt) = hashPassword(plainPassword)

            val update = buildJsonObject {
                put("password_hash", hash)
                put("salt", salt)
            }

            supabase.from("users")
                .update(update) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Updated password to hashed for userId: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating password: ${e.message}", e)
            false
        }
    }

    suspend fun changePassword(userId: Int, oldPassword: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // First verify old password
            val user = supabase.from("users")
                .select() {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserWithSaltDto>()

            if (user == null) return@withContext false

            // Verify old password
            val oldPasswordValid = if (user.salt != null) {
                verifyPassword(oldPassword, user.password_hash, user.salt)
            } else {
                simpleHash(oldPassword) == user.password_hash || oldPassword == user.password_hash
            }

            if (!oldPasswordValid) {
                Log.d(TAG, "Old password incorrect")
                return@withContext false
            }

            // Update to new password
            val (hash, salt) = hashPassword(newPassword)

            val update = buildJsonObject {
                put("password_hash", hash)
                put("salt", salt)
            }

            supabase.from("users")
                .update(update) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Password changed successfully for userId: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error changing password: ${e.message}", e)
            false
        }
    }
}

// DTOs
@Serializable
data class UserDto(
    val user_id: Int,
    val email: String,
    val password_hash: String,
    val user_type: String,
    val is_profile_complete: Boolean,
    val salt: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SimpleUserDto(
    val user_id: Int,
    val email: String,
    val password_hash: String,
    val user_type: String,
    val is_profile_complete: Boolean,
    val salt: String? = null,  // Added salt field
    val created_at: String? = null,  // Added this field
    val updated_at: String? = null   // Added this field
)

@Serializable
data class UserWithSaltDto(
    val user_id: Int,
    val email: String,
    val password_hash: String,
    val salt: String? = null,
    val user_type: String,
    val is_profile_complete: Boolean,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class UserIdOnlyDto(
    val user_id: Int
)

@Serializable
data class UserIdTestDto(val user_id: Int)