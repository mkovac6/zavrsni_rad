package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.User
import com.finalapp.accommodationapp.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {
    companion object {
        private const val TAG = "UserRepository"
    }

    suspend fun login(email: String, password: String): User? = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT u.user_id, u.email, u.user_type, u.is_profile_complete,
                       s.first_name as student_first_name, 
                       l.first_name as landlord_first_name
                FROM Users u
                LEFT JOIN Students s ON u.user_id = s.user_id
                LEFT JOIN Landlords l ON u.user_id = l.user_id
                WHERE u.email = ? AND u.password_hash = ?
            """.trimIndent()

            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setString(1, email)
            preparedStatement?.setString(2, password) // In production, this should be hashed!

            val resultSet = preparedStatement?.executeQuery()

            var user: User? = null
            if (resultSet?.next() == true) {
                val firstName = resultSet.getString("student_first_name")
                    ?: resultSet.getString("landlord_first_name")
                    ?: "User"

                user = User(
                    userId = resultSet.getInt("user_id"),
                    email = resultSet.getString("email"),
                    userType = resultSet.getString("user_type"),
                    isProfileComplete = resultSet.getBoolean("is_profile_complete"),
                    firstName = firstName
                )
                Log.d(TAG, "Login successful for: ${user.email}")

                // Save user to session
                UserSession.setUser(user)
            } else {
                Log.d(TAG, "Login failed: Invalid credentials")
            }

            resultSet?.close()
            preparedStatement?.close()
            connection?.close()

            user
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            null
        }
    }

    suspend fun register(email: String, password: String, userType: String = "student"): User? =
        withContext(Dispatchers.IO) {
            try {
                val connection = DatabaseConnection.getConnection()

                // First check if email already exists
                if (checkEmailExists(email)) {
                    Log.d(TAG, "Registration failed: Email already exists")
                    return@withContext null
                }

                // Insert new user
                val insertQuery = """
                INSERT INTO Users (email, password_hash, user_type, is_profile_complete)
                VALUES (?, ?, ?, 0)
            """.trimIndent()

                val preparedStatement = connection?.prepareStatement(
                    insertQuery,
                    java.sql.Statement.RETURN_GENERATED_KEYS
                )
                preparedStatement?.setString(1, email)
                preparedStatement?.setString(2, password) // In production, hash this!
                preparedStatement?.setString(3, userType)

                val rowsAffected = preparedStatement?.executeUpdate() ?: 0

                var user: User? = null
                if (rowsAffected > 0) {
                    val generatedKeys = preparedStatement?.generatedKeys
                    if (generatedKeys?.next() == true) {
                        val userId = generatedKeys.getInt(1)
                        user = User(
                            userId = userId,
                            email = email,
                            userType = userType,
                            isProfileComplete = false,
                            firstName = ""
                        )
                        Log.d(TAG, "Registration successful for: $email")

                        // Save user to session
                        UserSession.setUser(user)
                    }
                    generatedKeys?.close()
                }

                preparedStatement?.close()
                connection?.close()

                user
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}", e)
                null
            }
        }

    suspend fun checkEmailExists(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = "SELECT COUNT(*) as count FROM Users WHERE email = ?"

            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setString(1, email)

            val resultSet = preparedStatement?.executeQuery()
            val exists = resultSet?.next() == true && resultSet.getInt("count") > 0

            resultSet?.close()
            preparedStatement?.close()
            connection?.close()

            exists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking email: ${e.message}", e)
            false
        }
    }

    suspend fun updateUserProfileStatus(userId: Int, isComplete: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val connection = DatabaseConnection.getConnection()
                val query =
                    "UPDATE Users SET is_profile_complete = ?, updated_at = GETDATE() WHERE user_id = ?"
                val preparedStatement = connection?.prepareStatement(query)

                preparedStatement?.apply {
                    setBoolean(1, isComplete)
                    setInt(2, userId)
                }

                val rowsAffected = preparedStatement?.executeUpdate() ?: 0

                preparedStatement?.close()
                connection?.close()

                Log.d(TAG, "Updated profile status for user $userId to $isComplete")
                rowsAffected > 0
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile status: ${e.message}", e)
                false
            }
        }

}