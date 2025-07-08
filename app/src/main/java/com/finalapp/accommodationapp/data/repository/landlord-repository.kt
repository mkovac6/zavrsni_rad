package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.Landlord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LandlordRepository {
    companion object {
        private const val TAG = "LandlordRepository"
    }
    
    suspend fun getLandlordByUserId(userId: Int): Landlord? = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT * FROM Landlords WHERE user_id = ?
            """.trimIndent()
            
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, userId)
            val resultSet = preparedStatement?.executeQuery()
            
            val landlord = if (resultSet?.next() == true) {
                Landlord(
                    landlordId = resultSet.getInt("landlord_id"),
                    userId = resultSet.getInt("user_id"),
                    firstName = resultSet.getString("first_name"),
                    lastName = resultSet.getString("last_name"),
                    companyName = resultSet.getString("company_name"),
                    phone = resultSet.getString("phone"),
                    isVerified = resultSet.getBoolean("is_verified"),
                    rating = resultSet.getDouble("rating")
                )
            } else null
            
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
            
            Log.d(TAG, "Loaded landlord for userId: $userId")
            landlord
        } catch (e: Exception) {
            Log.e(TAG, "Error loading landlord", e)
            null
        }
    }
}