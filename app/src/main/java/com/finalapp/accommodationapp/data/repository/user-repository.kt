package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.User
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
}