package com.finalapp.accommodationapp.data.repository.landlord

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.landlord.Landlord
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

    suspend fun updateLandlordProfile(
        landlordId: Int,
        firstName: String,
        lastName: String,
        companyName: String?,
        phone: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()

            val updateQuery = """
            UPDATE Landlords SET 
                first_name = ?,
                last_name = ?,
                company_name = ?,
                phone = ?,
                updated_at = GETDATE()
            WHERE landlord_id = ?
        """.trimIndent()

            val preparedStatement = connection?.prepareStatement(updateQuery)
            preparedStatement?.apply {
                setString(1, firstName)
                setString(2, lastName)
                setString(3, companyName)
                setString(4, phone)
                setInt(5, landlordId)
            }

            val rowsAffected = preparedStatement?.executeUpdate() ?: 0
            preparedStatement?.close()
            connection?.close()

            Log.d(TAG, "Updated landlord profile: $landlordId, rows affected: $rowsAffected")
            rowsAffected > 0

        } catch (e: Exception) {
            Log.e(TAG, "Error updating landlord profile: ${e.message}", e)
            false
        }
    }
}